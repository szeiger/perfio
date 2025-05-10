package perfio;

import java.io.Closeable;
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
  private Task firstPending, lastPending, activePartition;
  private int pendingCount;
  private final int depth, partitionSize;
  private final Executor pool;
  private final boolean sequential;
  private final boolean allowAppend;
  private ConcurrentLinkedQueue<Closeable> cleanUp = new ConcurrentLinkedQueue<>();
  
  /// A task represents an input block to be transformed, together with an output block to write
  /// the data to. Filter implementations should not rely on the identity of [Task] objects.
  /// Additional tasks for continuations after an overflow or underflow may use the same [Task]
  /// object or a different one. State that needs to be kept around for the duration of a partition
  /// can be stored in the [Task#data] field.
  public final class Task implements Runnable {
    private Task next, prevInPartition;
    private CompletableFuture<?> stepDone;
    private final CompletableFuture<?> allDone = sequential || (partitionSize > 0) ? new CompletableFuture<>() : null;
    private BufferedOutput _from;
    public BufferedOutput to;
    public byte[] buf;
    public int start, end, totalLength;
    public byte state;
    private boolean consumed, isLastInPartition, ownsSourceBlock;
    
    /// Partition-specific data
    public Object data;
    
    /// The task starts a new partition with a new input block and a new output block
    public static final byte STATE_NEW = 0;
    /// The task is resubmitted with a new output block after an overflow
    public static final byte STATE_OVERFLOWED = 1;
    /// The task is resubmitted with a new input block after an underflow. This can only happen
    /// when a fixed partition size is set. If this is the last input block in the stream, it may
    /// be empty.
    public static final byte STATE_UNDERFLOWED = 2;
    
    private Task() {}

    public void run() {
      if(prevInPartition != null) {
        data = prevInPartition.data;
        prevInPartition = null;
      }
      filterAsync(this);
      if(allDone != null && consumed) allDone.complete(null);
    }

    private void await() throws IOException {
      try { stepDone.get(); }
      catch (InterruptedException | ExecutionException e) { throw new IOException(e); }
    }
    
    public void consume() { consumed = true; }
    
    public boolean isLast() { return isLastInPartition; }
    
    public int length() { return end - start; }
    
    public String toString() {
      var st = state == STATE_OVERFLOWED ? "OVER" : state == STATE_UNDERFLOWED ? "UNDER" : "NEW";
      var s = st + " " + _from.hashCode() + "[" + start + "," + end + "[";
      if(isLastInPartition) s += " LAST";
      if(ownsSourceBlock) s += " OWNS";
      if(data != null) s += " (DATA)";
      s += " tot=" + totalLength;
      return s;
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
  /// @param partitionSize When set to 0, each [Task] processes one input block, which is typically
  ///   between 1/2 and 1 times the parent's initial buffer size, but may be smaller or larger.
  ///   When set to a non-zero value, each task processes exactly this number of bytes, which may
  ///   come from multiple input blocks. The last task of a stream may be shorter.
  /// @param pool The thread pool on which the asynchronous tasks will be executed.
  protected AsyncFilteringBufferedOutput(BufferedOutput parent, boolean sequential, int depth, boolean allowAppend,
      int partitionSize, Executor pool) {
    super(parent, false);
    this.sequential = sequential;
    this.depth = depth < 0 ? ForkJoinPool.getCommonPoolParallelism() : depth;
    this.allowAppend = allowAppend;
    this.partitionSize = partitionSize;
    this.pool = pool;
  }

  /// Constructor that uses the common ForkJoinPool.
  /// 
  /// @see #AsyncFilteringBufferedOutput(BufferedOutput, boolean, int, boolean, int, Executor)
  protected AsyncFilteringBufferedOutput(BufferedOutput parent, boolean sequential, int depth, boolean allowAppend,
      int partitionSize) {
    this(parent, sequential, depth, allowAppend, partitionSize, ForkJoinPool.commonPool());
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

  private boolean firstIsDone() { return firstPending != null && firstPending.stepDone.isDone(); }

  /// Process the data in `t.buf` at `[t.start - t.end[` and write the output into `t.to`. This
  /// method is called on a worker thread. If the input was fully consumed, it must call
  /// `t.consume()`, otherwise the task will be rescheduled with a new output block.
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
    if(!t.consumed) { // overflow -> reschedule
      appendBlockToParent(t.to);
      t.to = allocTargetBlock(t._from);
      t.state = Task.STATE_OVERFLOWED;
      t.stepDone = CompletableFuture.runAsync(t, pool);
      //System.out.println("Rescheduled "+t);
    } else {
      if(t.to.pos != t.to.start) {
        if(reclaim && allowAppend && t.to.pos - t.to.start < t.to.buf.length / 2) ret = t.to;
        else appendBlockToParent(t.to);
      } else {
        if(reclaim) ret = t.to;
        else releaseBlock(t.to);
      }
      if(t.ownsSourceBlock) releaseBlock(t._from);
      dequeueFirst();
    }
    return ret;
  }

  protected void filterBlock(BufferedOutput b) throws IOException {
    BufferedOutput reclaimed = null;
    if(depth > 0) reclaimed = flushPending(depth-1, true);
    else while(firstIsDone()) reclaimed = flushFirst(pendingCount == 1);
    var start = b.start;
    final var end = b.pos;
    while(start != end) {
      var t = new Task();
      t.to = reclaimed == null ? allocTargetBlock(b) : reclaimed;
      reclaimed = null;
      t._from = b;
      t.buf = b.buf;
      t.start = start;

      var blen = end - start;
      int tlen, prevTotal;
      boolean isNew = activePartition == null;
      if(isNew) {
        tlen = partitionSize > 0 ? Math.min(blen, partitionSize) : blen;
        prevTotal = 0;
      } else {
        prevTotal = activePartition.totalLength;
        tlen = Math.min(blen, partitionSize - prevTotal);
        t.state = Task.STATE_UNDERFLOWED;
      }

      t.end = start + tlen;
      t.ownsSourceBlock = t.end == end;
      t.totalLength = prevTotal + tlen;
      t.isLastInPartition = partitionSize == 0 || t.totalLength == partitionSize;
      t.prevInPartition = activePartition;
      
      if(activePartition != null)
        t.stepDone = activePartition.allDone.thenRunAsync(t, pool);
      else 
        t.stepDone = sequential && lastPending != null ? lastPending.allDone.thenRunAsync(t, pool) : CompletableFuture.runAsync(t, pool);
      start = t.end;
      activePartition = t.isLastInPartition ? null : t;
      //System.out.println("Scheduled "+t);

      enqueue(t);
    }
  }
  
  @Override protected void flushPending() throws IOException {
    if(activePartition != null) {
      var t = new Task();
      t._from = activePartition._from;
      t.to = allocTargetBlock(activePartition._from);
      t.buf = t.to.buf;
      t.totalLength = activePartition.totalLength;
      t.isLastInPartition = true;
      t.ownsSourceBlock = false;
      t.prevInPartition = activePartition;
      t.state = Task.STATE_UNDERFLOWED;
      t.stepDone = activePartition.allDone.thenRunAsync(t); // TODO run on current thread if prev is already done?
      activePartition = null;
      //System.out.println("Scheduled end marker "+t);
      enqueue(t);
    }
    flushPending(0, false);
  }

  private BufferedOutput flushPending(int until, boolean reclaim) throws IOException {
    BufferedOutput ret = null;
    while(pendingCount > until || firstIsDone()) {
      firstPending.await();
      ret = flushFirst(reclaim && pendingCount == 1);
    }
    return ret;
  }
  
  /// Register a [Closeable] object to be closed together with this [BufferedOutput]. This method
  /// is safe to call from worker threads during [#filterAsync(Task)] to register thread-local
  /// state that needs to be cleaned up.
  protected void registerCleanUp(Closeable c) { cleanUp.add(c); }

  @Override
  protected void cleanUp() throws IOException {
    super.cleanUp();
    Closeable c;
    while((c = cleanUp.poll()) != null) c.close();
  }
}
