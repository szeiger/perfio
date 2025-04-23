package perfio;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/// This class provides a convenient base for asynchronous and parallel [FilteringBufferedOutput]
/// implementations. Subclasses only need to implement the [#filterAsync(Task)] method which is
/// called on the worker threads to process a single input block into an output block. All
/// scheduling is handled by this class.
/// 
/// Subclasses that want to provide synchronous filtering as an option (which can take advantage
/// of in-place updates and direct writing to the parent) should also overwrite
/// [#filterBlock(BufferedOutput)] accordingly.
/// 
/// All tasks are scheduled on the [ForkJoinPool#commonPool()].
public abstract class AsyncFilteringBufferedOutput extends FilteringBufferedOutput {
  private Task firstPending, lastPending;
  private int pendingCount;
  private final boolean sequential;
  private final int depth;

  /// A task represents an input block to be transformed, together with an output block to write
  /// the data to. The same [Task] object can be scheduled multiple times with new output blocks
  /// if necessary (to continue after an overflow).
  protected class Task implements Runnable {
    private Task next;
    private CompletableFuture<?> future;
    private final CompletableFuture<?> done = sequential ? new CompletableFuture<>() : null;
    protected BufferedOutput from, to;

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
  ///   thread pool's parallelism. This can be used to throttle the main thread to limit memory
  ///   usage when the filter cannot keep up with the incoming data.
  protected AsyncFilteringBufferedOutput(BufferedOutput parent, boolean sequential, int depth) {
    super(parent, false);
    this.sequential = sequential;
    this.depth = depth < 0 ? ForkJoinPool.getCommonPoolParallelism() : depth;
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

  private void flushFirst() throws IOException {
    var t = firstPending;
    if(t.from.pos != t.from.start) { // overflow -> reschedule
      appendBlockToParent(t.to);
      t.to = allocTargetBlock(t.from);
      t.future = CompletableFuture.runAsync(t);
    } else {
      if(t.to.pos != t.to.start) appendBlockToParent(t.to);
      else returnToCache(t.to);
      dequeueFirst();
    }
  }

  protected void filterBlock(BufferedOutput b) throws IOException {
    if(depth > 0) flushPending(depth-1);
    while(firstIsDone()) flushFirst();
    var t = new Task();
    t.from = b;
    t.to = allocTargetBlock(b);
    t.future = sequential && lastPending != null ? lastPending.done.thenRunAsync(t) : CompletableFuture.runAsync(t);
    enqueue(t);
  }

  @Override protected void flushPending() throws IOException { flushPending(0); }

  private void flushPending(int until) throws IOException {
    while(pendingCount > until) {
      firstPending.await();
      flushFirst();
    }
  }
}
