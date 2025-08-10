package perfio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/// Provides unchecked direct access to a [ReadableBuffer]'s byte array or [ByteBuffer] to build
/// custom abstractions on top of ReadableBuffer that can run with the same performance as perfIO's
/// built-in ones (like [LineTokenizer]).
/// 
/// A [ReadableBuffer] can be either heap-based or direct (off-heap). A heap-based buffer contains
/// a non-null `byte[]` [#buf(ReadableBuffer)], a direct one does not. A ReadableBuffer subclass
/// may support both modes, but any given instance is either heap-based or direct for its entire
/// lifecycle. Do not use any other means of distinguishing between the two. In particular, every
/// direct buffer contains a non-null `ByteBuffer` [#bb(ReadableBuffer)] but its presence or
/// absence in heap-based buffers is undefined.
/// 
/// The buffer has a position from which you can read and a limit that must not be exceeded. You
/// can read directly from this region without buffering:
/// 
/// ```
/// ReadableBuffer in;
/// if(available(in) >= 1) {
///   var p = position(in);
///   byte b;
///   if(buf(in) != null) b = buf(in)[p];
///   else b = bb(in).get(p);
///   position(in, p + 1);
/// }
/// ```
/// 
/// Buffering supports two use cases in the most efficient way:
/// - Reading a fixed number of bytes with [#advance(ReadableBuffer, int)].
/// - Reading a flexible number of bytes that should be aligned with the buffer size to avoid
///   growing or splitting buffers with [#requestAvailable(ReadableBuffer, int)].
/// 
/// All buffering operations may update the buffer, position and limit. Do not cache these values
/// across buffering calls.
public class UncheckedInput {
  //TODO provide MemorySegment access for direct buffers
  
  private UncheckedInput() {}

  /// Ensure that `count` bytes are available to read in the buffer, advance the buffer to the
  /// position after these bytes and return the previous position. Throws [EOFException] if the end
  /// of the input is reached before the requested number of bytes is available. This method may
  /// change the buffer references.
  ///
  /// After a call `int p = advance(out, c)` returns, you can read from offsets `[p...(p+c)[`
  /// in `buf()` or `bb()`.
  /// 
  /// @throws java.io.EOFException if the end of the input would be exceeeded.
  /// @return The previous position, i.e. the position in the buffer where the
  /// requested region starts.
  public static int advance(ReadableBuffer in, int count) throws IOException { return in.fwd(count); }

  /// Request `count` bytes to be available to read in the buffer. Less may be available if the end
  /// of the input is reached. This method may change the buffer references. This method is often
  /// used with a count of 1 to ensure that at least 1 byte can be read, and then reading up to the
  /// limit of the buffer.
  public static void requestAvailable(ReadableBuffer in, int count) throws IOException { in.requestAvailable(count); }
  
  /// Return the current position in the buffer.
  public static int position(ReadableBuffer in) { return in.pos; }

  /// Set the current position in the buffer. The position must not be advanced beyond the
  /// [#limit(ReadableBuffer)] even if the [#buf(ReadableBuffer)]] or [#bb(ReadableBuffer)]]
  /// has more room.
  public static void position(ReadableBuffer in, int pos) { in.pos = pos; }

  /// Return the limit of the buffer (i.e. the index after the last valid
  /// [#position(ReadableBuffer)]).
  public static int limit(ReadableBuffer in) { return in.lim; }

  /// Returns the current buffer byte array for a heap-based buffer, or `null` for a direct buffer.
  /// The result of this method must not be cached.
  public static byte[] buf(ReadableBuffer in) { return in.buf; }

  /// Returns the current [ByteBuffer] for a direct buffer. The result for heap-based buffers is
  /// undefined. The position and limit of the [ByteBuffer] object are undefined. All access should
  /// be done with absolute positions based on [#position(ReadableBuffer)]] and
  /// [#limit(ReadableBuffer)]]. The result of this method must not be cached.
  public static ByteBuffer bb(ReadableBuffer in) { return in.bb; }
}
