package perfio;

import perfio.internal.BufferUtil;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.*;

/// This class provides a convenient base for asynchronous and parallel [FilteringBufferedOutput]
/// implementations. Subclasses only need to implement the [#filterAsync(Task)] method which is
/// called on the worker threads to process a single input block into an output block. All
/// scheduling is handled by this class.
///
/// Subclasses that want to provide synchronous filtering as an option (which can take advantage
/// of in-place updates and direct writing to the parent) should also overwrite
/// [#filterBlock(BufferedOutput)] accordingly.
///
/// Subclasses should clean up any resources before throwing an Exception from any of these
/// methods. In case of a sequential filter, no further methods will be called and instead the
/// asynchronous Exception is rethrown, but a parallel filter may still make further
/// [#filterAsync(Task)] calls on other threads.
public abstract class AsyncFilteringBufferedOutput extends FilteringBufferedOutput {
  private final int depth, minPartitionSize, maxPartitionSize;
  private final boolean sequential, allowAppend, batchSubmit;
  private final Executor pool;

  private Task firstPending, lastPending, activePartition, firstUnsubmitted;
  private int pendingCount, activePartitionLength;
  private Throwable asyncError;
  //private final AtomicInteger asyncAllocated = new AtomicInteger();

  /// Constructor to be called by subclasses.
  ///
  /// @param parent The parent buffer to write to.
  /// @param sequential If set to true, blocks will be processed sequentially, i.e. a new block is
  ///   not started before the previous one has been processed completely (including potentially
  ///   rescheduling it in case of overflows). Note that `sequential = true` is different from
  ///   `depth = 1`. While both guarantee sequential processing, `sequential = true` (with a
  ///   `depth` greater than 1) starts processing a new block immediately when the previous one
  ///   is done and does not block the main thread. While partitioning is not useful in sequential
  ///   mode, `minPartitionSize` can be set together with `batchSubmit = true`.
  /// @param depth The maximum number of blocks or partitions in flight, 0 for no limit, or -1 to
  ///   match the common ForkJoinPool's parallelism. This can be used to throttle the main thread
  ///   to limit memory usage when the filter cannot keep up with the incoming data. When
  ///   `batchSubmit == true` the count is in partitions, otherwise in blocks.
  /// @param allowAppend When set to true, [#filterAsync(Task)] must support appending to a
  ///   non-empty block, otherwise it is guaranteed to get an empty block (i.e. `start = 0`).
  /// @param minPartitionSize When both `minPartitionSize` and `maxPartitionSize` are set to 0,
  ///   each [Task] processes one input block, which is typically between 1/2 and 1 times the
  ///   parent's initial buffer size, but may be smaller or larger. When set to a non-zero value,
  ///   each task processes between the minimum and maximum number of bytes, which may come from
  ///   multiple input blocks. The last task of a stream may be smaller than the minimum.
  /// @param maxPartitionSize The maximum partition size, or 0 for unlimited. When unlimited, input
  ///   blocks are never split into multiple partitions.
  /// @param batchSubmit When set to true, a partition is submitted to the thread pool (and its
  ///   result is awaited) in one go, otherwise each input block is submitted immediately. Batching
  ///   can help avoid frequent stalls of the filter processing threads which lead to high thread
  ///   parking overhead.
  /// @param pool The thread pool on which the asynchronous tasks will be executed, or `null` to
  ///   use the common ForkJoinPool.
  protected AsyncFilteringBufferedOutput(BufferedOutput parent, boolean sequential, int depth, boolean allowAppend,
      int minPartitionSize, int maxPartitionSize, boolean batchSubmit, Executor pool) {
    super(parent, false);
    this.sequential = sequential;
    this.depth = depth < 0 ? ForkJoinPool.getCommonPoolParallelism() : depth;
    this.allowAppend = allowAppend;
    this.minPartitionSize = minPartitionSize;
    this.maxPartitionSize = maxPartitionSize > 0 ? maxPartitionSize : Integer.MAX_VALUE;
    this.batchSubmit = batchSubmit;
    this.pool = pool != null ? pool : ForkJoinPool.commonPool();
  }

  /// A task represents an input block to be transformed, together with an output block to write
  /// the data to. Filter implementations should not rely on the identity of [Task] objects.
  /// Additional tasks for continuations after an overflow or underflow may use the same [Task]
  /// object or a different one. State that needs to be kept around for the duration of a partition
  /// can be stored in the [Task#data] field.
  public final class Task implements Runnable {
    private Task next, prevInPartition, nextInBatch;
    private Throwable asyncError;
    private CountDownLatch taskDone;
    private BufferedOutput _from;
    public BufferedOutput to;
    public byte[] buf;
    public int start, end;
    public byte state;
    private boolean consumed, isLastInPartition, ownsSourceBlock;
    private volatile Object continuation;

    /// Partition-specific data
    public Object data;

    private static final VarHandle CONTINUATION =
      BufferUtil.findVarHandle(MethodHandles.lookup(), Task.class, "continuation", Object.class);
    private static final Object FINISHED = new Object();

    /// The task starts a new partition with a new input block and a new output block
    public static final byte STATE_NEW = 0;
    /// The task is resubmitted with a new output block after an overflow
    public static final byte STATE_OVERFLOWED = 1;
    /// The task is resubmitted with a new input block after an underflow. This can only happen
    /// when a fixed partition size is set. If this is the last input block in the stream, it may
    /// be empty.
    public static final byte STATE_UNDERFLOWED = 2;

    private Task() {}

    public void run() { runAll(this); }

    private void await() throws IOException {
      try { taskDone.await(); }
      catch (InterruptedException e) { throw new IOException(e); }
    }

    private boolean isDone() { return taskDone.getCount() == 0; }

    /// Mark the input as consumed. Returning without consuming the input indicates an overflow.
    public void consume() { consumed = true; }

    /// Check if this is the last task in a partition
    public boolean isLast() { return isLastInPartition; }

    public int length() { return end - start; }

    public String toString() {
      var st = state == STATE_OVERFLOWED ? "OVER" : state == STATE_UNDERFLOWED ? "UNDER" : "NEW";
      var s = st + " " + _from.hashCode() + "[" + start + "," + end + "[";
      if(isLastInPartition) s += " LAST";
      if(ownsSourceBlock) s += " OWNS";
      if(data != null) s += " (DATA)";
      return s;
    }

    private Task getContinuation() {
      if(CONTINUATION.compareAndSet(this, null, FINISHED)) return null;
      else return (Task) continuation;
    }
  }

  private void runAll(Task t) {
    while(true) {
      try {
        runBatch(t);
      } catch(Throwable ex) {
        t.asyncError = ex;
        t.taskDone.countDown();
        return;
      }
      var t2 = t.getContinuation();
      if(depth != 0) t.taskDone.countDown();
      if(t2 == null) {
        // With unlimited depth we only need to notify once at the end of the chain of
        // continuations. All tasks share the same latch.
        if(depth == 0) t.taskDone.countDown();
        break;
      }
      t = t2;
    }
  }

  private void runBatch(Task firstInBatch) throws IOException {
    var t = firstInBatch;
    var stealFrom = firstInBatch;
    while(true) {
      if(t.prevInPartition != null) {
        t.data = t.prevInPartition.data;
        t.prevInPartition = null;
      }
      while(true) {
        filterAsync(t);
        if(t.consumed) break;
        BufferedOutput n = null;
        // Try to steal an empty block from the same batch before allocating a non-cacheable one
        while(stealFrom != t) {
          var cand = stealFrom.to.prev;
          if(cand.start == cand.pos) {
            if(cand == stealFrom.to) {
              stealFrom.to = null;
              stealFrom = stealFrom.nextInBatch;
            } else {
              cand.unlinkOnly();
              cand.next = cand.prev = cand;
            }
            n = cand;
            break;
          }
          stealFrom = stealFrom.nextInBatch;
        }
        if(n == null) {
          n = allocTargetBlockNoCache(t._from);
          //asyncAllocated.incrementAndGet();
        }
        t.to.insertAllBefore(n);
        t.to = n;
        t.state = Task.STATE_OVERFLOWED;
      }
      var n = t.nextInBatch;
      if(n == null) break;
      t = n;
    }
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

  private boolean firstIsDone() { return firstPending != null && firstPending.isDone(); }

  /// Process the data in `t.buf` at `[t.start - t.end[` and write the output into `t.to`. This
  /// method is called on a worker thread. If the input was fully consumed, it must call
  /// `t.consume()`, otherwise the task will be rescheduled with a new output block.
  ///
  /// `t.to` is initialized to a state where the primitive writing methods (e.g.
  /// [BufferedOutput#int8(byte)]) can be used on it. Filter implementations may also write
  /// directly to `t.to.buf` and set `t.to.start` and `t.to.pos` accordingly.
  protected abstract void filterAsync(Task t) throws IOException;

  private BufferedOutput allocTargetBlock(BufferedOutput from) {
    var b = (NestedBufferedOutput)allocBlock();
    b.reinit(b.buf, from.bigEndian, 0, 0, b.buf.length, SHARING_EXCLUSIVE, b.buf.length, 0, true, from.rootBlock, b, from.topLevel);
    b.next = b.prev = b;
    return b;
  }

  // safe to call from worker threads
  private BufferedOutput allocTargetBlockNoCache(BufferedOutput from) {
    var b = new NestedBufferedOutput(new byte[initialBufferSize], false, this);
    b.reinit(b.buf, from.bigEndian, 0, 0, b.buf.length, SHARING_EXCLUSIVE, b.buf.length, 0, true, from.rootBlock, b, from.topLevel);
    b.next = b.prev = b;
    b.nocache = true;
    return b;
  }

  protected void filterBlock(BufferedOutput b) throws IOException {
    if(asyncError != null) throw new IOException(asyncError);

    BufferedOutput reclaimed = null;
    while(firstIsDone()) reclaimed = flushFirst(reclaimed == null);
    if(depth > 0) {
      while(pendingCount >= depth) {
        firstPending.await();
        reclaimed = flushFirst(reclaimed == null);
      }
    }

    var start = b.start;
    final var end = b.pos;
    while(start != end) {
      var t = new Task();
      t.to = reclaimed == null ? allocTargetBlock(b) : reclaimed;
      reclaimed = null;
      t._from = b;
      t.buf = b.buf;
      t.start = start;

      int tlen = Math.min(end - start, maxPartitionSize - activePartitionLength);
      boolean isNew = activePartition == null;
      t.state = isNew ? Task.STATE_NEW : Task.STATE_UNDERFLOWED;
      t.end = start + tlen;
      t.ownsSourceBlock = t.end == end;
      t.isLastInPartition = (activePartitionLength + tlen) >= minPartitionSize;
      t.prevInPartition = activePartition;
      start = t.end;

      if(t.isLastInPartition) activePartitionLength = 0;
      else activePartitionLength += tlen;

      if(batchSubmit) {
        if(isNew) {
          if(t.isLastInPartition) scheduleNewOrSequential(t);
          else {
            activePartition = t;
            firstUnsubmitted = t;
          }
        } else {
          activePartition.nextInBatch = t;
          if(t.isLastInPartition) {
            scheduleNewOrSequential(firstUnsubmitted);
            activePartition = null;
            firstUnsubmitted = null;
          } else activePartition = t;
        }
      } else {
        if(activePartition != null) scheduleContinuation(activePartition, t);
        else scheduleNewOrSequential(t);
        activePartition = t.isLastInPartition ? null : t;
        //System.out.println("Scheduled "+t);
      }
    }
  }

  private void handleAsyncError(Throwable ex) throws IOException {
    asyncError = ex;
    try {
      if(activePartition != null && firstUnsubmitted == null) {
        // Ensure that an already submitted active partition is properly terminated to free resources
        var t = createPartitionTerminator();
        scheduleContinuation(t.prevInPartition, t);
      }
    } finally { throw new IOException(ex); }
  }

  private BufferedOutput flushFirst(boolean reclaim) throws IOException {
    BufferedOutput reclaimed = null;
    var t = firstPending;
    if(t.asyncError != null) handleAsyncError(t.asyncError);
    while(true) {
      //System.err.println("flushFirst "+t);
      if(t.to != null) {
        var n = t.to.next;
        while(true) {
          var nn = n.next;
          if(n.pos != n.start) {
            if(reclaim && reclaimed == null && allowAppend && firstPending == lastPending && t.nextInBatch == null && n == t.to && n.pos - n.start < n.buf.length / 2) reclaimed = n;
            else appendBlockToParent(n);
          } else {
            if(reclaim && reclaimed == null) reclaimed = n;
            else releaseBlock(n);
          }
          if(n == t.to) break;
          n = nn;
        }
      }
      if(t.ownsSourceBlock) releaseBlock(t._from);
      if(t.nextInBatch == null) break;
      t = t.nextInBatch;
    }
    dequeueFirst();
    if(reclaimed != null) reclaimed.prev = reclaimed.next = reclaimed;
    return reclaimed;
  }

  @Override protected void flushPending() throws IOException {
    if(asyncError != null) throw new IOException(asyncError);

    //System.err.println("flushPending: firstUnsubmitted="+firstUnsubmitted+", activePartition="+activePartition);
    if(firstUnsubmitted != null) {
      activePartition.isLastInPartition = true;
      scheduleNew(firstUnsubmitted);
      firstUnsubmitted = null;
      activePartition = null;
    }
    while(pendingCount > 0) {
      firstPending.await();
      flushFirst(false);
    }
    if(activePartition != null) {
      // TODO: Try to run this as a continuation and fall back to running synchronously if that fails
      var t = createPartitionTerminator();
      runBatch(t);
      enqueue(t);
      flushFirst(false);
    }
    //System.out.println("asyncAllocated: "+asyncAllocated.get());
  }

  private Task createPartitionTerminator() throws IOException {
    var t = new Task();
    t._from = activePartition._from;
    t.to = allocTargetBlock(activePartition._from);
    t.buf = t.to.buf;
    t.isLastInPartition = true;
    t.ownsSourceBlock = false;
    t.prevInPartition = activePartition;
    t.state = Task.STATE_UNDERFLOWED;
    activePartition = null;
    return t;
  }

  private void scheduleNewOrSequential(Task t) {
    if(sequential && lastPending != null) scheduleContinuation(lastPending, t);
    else scheduleNew(t);
  }

  private void scheduleContinuation(Task prev, Task t) {
    t.taskDone = depth == 0 ? prev.taskDone : new CountDownLatch(1);
    if(!Task.CONTINUATION.compareAndSet(prev, null, t)) {
      if(depth == 0) t.taskDone = new CountDownLatch(1);
      pool.execute(t);
    }
    enqueue(t);
  }

  private void scheduleNew(Task t) {
    t.taskDone = new CountDownLatch(1);
    pool.execute(t);
    enqueue(t);
  }
}
