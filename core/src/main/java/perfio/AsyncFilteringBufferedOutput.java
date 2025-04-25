package perfio;

import java.io.IOException;
import java.util.concurrent.*;

/// This class provides a convenient base for asynchronous and parallel [FilteringBufferedOutput]
/// implementations. Subclasses only need to implement the [#filterAsync(Task)] method which is
/// called on the worker threads to process a single input block into an output block. All
/// scheduling is handled by this class.
///
/// Subclasses that want to provide synchronous filtering as an option (which can take advantage
/// of in-place updates and direct writing to the parent) should also overwrite
/// [#filterBlock(BufferedOutput)] accordingly.
public abstract class AsyncFilteringBufferedOutput extends FilteringBufferedOutput {
  private Task firstPending, lastPending;
  private int pendingCount;
  private final int depth;
  private final Executor pool;
  private final boolean sequential;
  private final boolean allowAppend;

  /// A task represents an input block to be transformed, together with an output block to write
  /// the data to. The same [Task] object can be scheduled multiple times with new output blocks
  /// if necessary (to continue after an overflow).
  protected class Task implements Runnable {
    private Task next;
    private CompletableFuture<?> future;
    private final CompletableFuture<?> done = sequential ? new CompletableFuture<>() : null;
    protected BufferedOutput from, to;
    protected boolean continuation;

    public void run() {
      filterAsync(this);
      if(sequential && from.start == from.pos) done.complete(null);
    }

    private void await() throws IOException {
      try { future.get(); }
      catch (InterruptedException | ExecutionException e) { throw new IOException(e); }
    }
  }

  /// Constructor to be called by subclasses.
  ///
  /// @param parent The parent buffer to write to.
  /// @param sequential If set to true, blocks will be processed sequentially, i.e. a new block is
  ///   not started before the previous one has been processed completely (including potentially
  ///   rescheduling it in case of overflows). Note that `sequential = true` is different from
  ///   `depth = 1`. While both guarantee sequential processing, `sequential = true` (with a
  ///   `depth` greater than 1) starts  processing a new block immediately when the previous one
  ///   is done and does not block the main thread.
  /// @param depth The maximum number of blocks in flight, 0 for no limit, or -1 to match the
  ///   common ForkJoinPool's parallelism. This can be used to throttle the main thread to limit
  ///   memory usage when the filter cannot keep up with the incoming data.
  /// @param allowAppend When set to true, [#filterAsync(Task)] must support appending to a
  ///   non-empty block, otherwise it is guaranteed to get an empty block (i.e. `start = 0`).
  /// @param pool The thread pool on which the asynchronous tasks will be executed.
  protected AsyncFilteringBufferedOutput(BufferedOutput parent, boolean sequential, int depth, boolean allowAppend, Executor pool) {
    super(parent, false);
    this.sequential = sequential;
    this.depth = depth < 0 ? ForkJoinPool.getCommonPoolParallelism() : depth;
    this.allowAppend = allowAppend;
    this.pool = pool;
  }

  /// Constructor that uses the common ForkJoinPool.
  /// 
  /// @see #AsyncFilteringBufferedOutput(BufferedOutput, boolean, int, boolean, Executor)
  protected AsyncFilteringBufferedOutput(BufferedOutput parent, boolean sequential, int depth, boolean allowAppend) {
    this(parent, sequential, depth, allowAppend, ForkJoinPool.commonPool());
    //this(parent, sequential, depth, allowAppend, new Executor() {
    //  public void execute(Runnable command) { command.run(); }
    //});
    //this(parent, sequential, depth, allowAppend, Executors.newSingleThreadExecutor(new ThreadFactory() {
    //  public Thread newThread(Runnable r) {
    //    var t = Executors.defaultThreadFactory().newThread(r);
    //    t.setDaemon(true);
    //    return t;
    //  }
    //}));
  }

  private void enqueue(Task t) {
    pendingCount++;
    if(lastPending != null) lastPending.next = t;
    else firstPending = t;
    lastPending = t;
  }

  private void dequeueFirst() {
    pendingCount--;
    firstPending = firstPending.next;
    if(firstPending == null) lastPending = null;
  }

  private boolean firstIsDone() { return firstPending != null && firstPending.future.isDone(); }

  /// Process the block `t.from` and write the output into `t.to`. This method is called on a
  /// worker thread. If the input was fully consumed, it must set `t.from.start` to be equal to
  /// `t.from.pos`, otherwise the task will be rescheduled with a fresh output block.
  ///
  /// `t.to` is initialized to a state where the primitive writing methods (e.g.
  /// [BufferedOutput#int8(byte)]) can be used on it. Filter implementations may also write
  /// directly to `t.to.buf` and set `t.to.start` and `t.to.pos` accordingly.
  protected abstract void filterAsync(Task t);

  private BufferedOutput allocTargetBlock(BufferedOutput from) {
    var b = (NestedBufferedOutput)allocBlock();
    b.reinit(b.buf, from.bigEndian, 0, 0, b.buf.length, SHARING_EXCLUSIVE, b.buf.length, 0, true, from.rootBlock, b, from.topLevel);
    return b;
  }

  private BufferedOutput flushFirst(boolean reclaim) throws IOException {
    var t = firstPending;
    BufferedOutput ret = null;
    if(t.from.pos != t.from.start) { // overflow -> reschedule
      //System.out.println("overflow: "+reclaim+", "+(t.to.pos-t.to.start));
      appendBlockToParent(t.to);
      t.to = allocTargetBlock(t.from);
      t.continuation = true;
      t.future = CompletableFuture.runAsync(t, pool);
    } else {
      if(t.to.pos != t.to.start) {
        //System.out.println("no overflow, not empty: "+reclaim+", "+(t.to.pos-t.to.start)+" of "+t.to.buf.length);
        if(reclaim && allowAppend && t.to.pos - t.to.start < t.to.buf.length / 2) ret = t.to;
        else appendBlockToParent(t.to);
        //if(ret != null) System.out.println("reclaiming");
      } else {
        //System.out.println("no overflow, empty: "+reclaim+", "+(t.to.pos-t.to.start));
        if(reclaim) ret = t.to;
        else releaseBlock(t.to);
      }
      releaseBlock(t.from);
      dequeueFirst();
    }
    return ret;
  }

  protected void filterBlock(BufferedOutput b) throws IOException {
    BufferedOutput reclaimed = null;
    if(depth > 0) reclaimed = flushPending(depth-1, true);
    while(firstIsDone()) reclaimed = flushFirst(pendingCount == 1);
    var t = new Task();
    t.from = b;
    t.to = reclaimed == null ? allocTargetBlock(b) : reclaimed;
    t.future = sequential && lastPending != null ? lastPending.done.thenRunAsync(t, pool) : CompletableFuture.runAsync(t, pool);
    enqueue(t);
  }

  @Override protected void flushPending() throws IOException { flushPending(0, false); }

  private BufferedOutput flushPending(int until, boolean reclaim) throws IOException {
    BufferedOutput ret = null;
    while(pendingCount > until) {
      firstPending.await();
      ret = flushFirst(reclaim && pendingCount == 1);
    }
    return ret;
  }
}
