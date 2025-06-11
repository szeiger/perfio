package perfio;

/// A FilterTask represents an input block to be transformed, together with an output block to
/// write the data to. Filter implementations must not rely on the identity of FilterTask
/// objects. Additional tasks for continuations after an overflow or underflow may use the same
/// FilterTask or a different one. State that needs to be kept around for the duration of a
/// partition can be stored in the [#data] field.
/// 
/// @param <Data> The type of the filter implementation's [#data].
public abstract class FilterTask<Data> {
  /// The task starts a new partition with a new input block and a new output block
  public static final byte STATE_NEW = 0;

  /// The task is resubmitted with a new output block after an overflow
  public static final byte STATE_OVERFLOWED = 1;

  /// The task is resubmitted with a new input block after an underflow. This can only happen
  /// when a fixed partition size is set. If this is the last input block in the stream, it may
  /// be empty.
  public static final byte STATE_UNDERFLOWED = 2;

  /// The output block to which the filter should write. Filters implementations can use the
  /// primitive writing methods, or write directly to `to.buf` and set `to.start` and `to.pos`
  /// accordingly.
  public WritableBuffer<?> to;

  /// The input block's buffer.
  public byte[] buf;
  
  /// The first used index in [#buf]. A filter implementation may update this before
  /// producing an overflow to keep track of how much data it has already processed.
  public int start;

  /// The index after the last used index in [#buf].
  public int end;

  /// Partition-specific data for use by filter implementations, initially set to `null` in a
  /// new partition. This object is carried over from each [FilterTask] of a partition to the next.
  /// When used to hold resources that must be closed manually, this should be done at the end
  /// of the partition (i.e. [#isLastInPartition] was true, and no overflow produced).
  public Data data;

  /// One of [#STATE_NEW], [#STATE_OVERFLOWED] and [#STATE_UNDERFLOWED].
  public byte state;

  boolean consumed, isLastInPartition;

  FilterTask() {}

  /// Mark the input as consumed. Returning without consuming the input indicates an overflow.
  public void consume() { consumed = true; }

  /// Check if this is the last input block in a partition
  public boolean isLast() { return isLastInPartition; }
  
  /// Check if the input block is empty. Unless the filter implementation caused this on its
  /// own by updating [#start] or [#end], this can only happen in non-batched partitioned mode
  /// where an empty block is used to terminate the last partition before the end of the stream
  /// or an explicit [#flush()].
  public boolean isEmpty() { return start == end; }
  
  /// Check if the input block is new (i.e. `state != STATE_OVERFLOWED`)
  public boolean isNew() { return state != STATE_OVERFLOWED; }

  /// Return the length of the input block (`end - start`)
  public int length() { return end - start; }
}
