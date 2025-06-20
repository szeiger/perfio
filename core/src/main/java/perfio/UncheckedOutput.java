package perfio;

import java.io.IOException;

/// Provides unchecked direct access to a [WritableBuffer]'s byte array buffer to build custom
/// abstractions on top of WritableBuffer that can run with the same performance as perfIO's
/// built-in ones (like [TextOutput]).
/// 
/// A [WritableBuffer] always contains a current `byte[]` buffer. In case of a [BufferedOutput] it
/// can never be smaller than [BufferedOutput#initialBufferSize()]. The buffer has a position from
/// which you can write and a limit that must not be exceeded. You can write directly to this
/// region without allocating space:
/// 
/// ```
/// WritableBuffer out;
/// if(available(out) >= 2) {
///   buf(out)[position(out)] = 1;
///   buf(out)[position(out) + 1] = 2;
///   position(out, position(out) + 2)
/// }
/// ```
/// 
/// Allocating a region in the buffer supports three use cases in the most efficient way:
/// - Writing a fixed number of bytes with [#advance(WritableBuffer, int)].
/// - Producing a flexible number of bytes that should be aligned with the buffer size to avoid
///   growing or splitting buffers with [#ensureAvailable(WritableBuffer, int)].
/// - Over-allocating some headroom for a write (e.g. for flexible-length text encoding) without
///   failing when you hit the buffer's size limit with [#tryAdvance(WritableBuffer, int)].
/// 
/// All allocations may update the buffer, position and limit. Do not cache these values across
/// allocation calls.
public class UncheckedOutput {
  private UncheckedOutput() {}

  /// Ensure that the requested number of bytes can be written and advance the buffer's position
  /// accordingly. The buffer is flushed and/or reallocated as necessary. You must call
  /// [#buf(WritableBuffer)] *after* this method to retrieve the correct buffer.
  ///
  /// After a call `int p = advance(out, c)` returns, you must write to offsets `[p...(p+c)[`
  /// in `buf()`.
  /// 
  /// If you may end up writing less than the number of requested bytes, you should use
  /// [#tryAdvance(WritableBuffer, int)] instead.
  ///
  /// Example: Writing 3 bytes with a single range check:
  /// ```java
  /// WritableBuffer out;
  /// byte b0, b1, b2;
  /// var p = advance(out, 3);
  /// var buf = buf(out);
  /// buf[p] = b0;
  /// buf[p+1] = b1;
  /// buf[p+2] = b2;
  /// ```
  /// Note: This simple example does not actually benefit from using `UncheckedOutput`. HotSpot is
  /// able to optimize `out.int8(b0).int8(b1).int8(b2)` to be just as fast.
  /// 
  /// @throws java.io.EOFException if a buffer's size limit would be exceeeded.
  /// @return The previous position, i.e. the position in the buffer where the
  /// requested region starts.
  public static int advance(WritableBuffer<?> out, int count) throws IOException { return out.fwd(count); }

  /// Attempt to make room for the requested number of bytes and advance the buffer's position 
  /// accordingly. Unlike [#advance(WritableBuffer, int)], this method may make less than the
  /// requested number of bytes (or even 0) available if the buffer's size limit is reached.
  ///
  /// You must call [#position(WritableBuffer)] to check the number of available bytes before writing any data
  /// to the buffer. After a call `int p = request(c)` returns, you can write to offsets
  /// `[p...(position())[` in `buf()`. If you end up writing fewer bytes, must use
  /// [#position(WritableBuffer, int)] to adjust the position accordingly.
  ///
  /// @return The previous position, i.e. the position in the buffer where the
  /// requested region starts.
  /// @see #advance(WritableBuffer, int)
  public static int tryAdvance(WritableBuffer<?> out, int count) throws IOException { return out.tryFwd(count); }

  /// Ensure that the requested number of bytes can be written to the buffer. This method is
  /// usually used with a count of 1 to ensure that at least 1 byte can be written, and then
  /// writing up to the limit of the buffer.
  /// 
  /// Example: Reading data from an `InputStream` directly into a buffer:
  /// ```
  /// InputStream in;
  /// WritableBuffer out;
  /// out.ensureAvailable(1);
  /// var l = in.read(buf(out), position(out), available(out));
  /// if(l > 0) position(out, position(out) + l);
  /// ```
  /// 
  /// @throws java.io.EOFException if a buffer's size limit would be exceeeded.
  public static void ensureAvailable(WritableBuffer<?> out, int count) throws IOException { out.ensureAvailable(count); }
  
  /// Return the current position in the buffer.
  public static int position(WritableBuffer<?> out) { return out.pos; }

  /// Set the current position in the buffer. The position must not be advanced beyond the
  /// [#limit(WritableBuffer)] even if the [#buf(WritableBuffer)]] byte array has more room.
  public static void position(WritableBuffer<?> out, int pos) { out.pos = pos; }

  /// Return the limit of the buffer (i.e. the index after the last valid
  /// [#position(WritableBuffer)]).
  public static int limit(WritableBuffer<?> out) { return out.lim; }

  /// Return the start of the buffer.
  public static int start(WritableBuffer<?> out) { return out.start; }

  /// Return the current available space in the buffer. This is equal to
  /// `limit - position`.
  public static int available(WritableBuffer<?> out) { return out.available(); }

  /// Return the currently used space in the buffer. This is equal to
  /// `position - start`.
  public static int used(WritableBuffer<?> out) { return out.pos - out.start; }

  /// Returns the current buffer. The result of this method must not be cached. Calls to
  /// [#advance(WritableBuffer, int)], [#tryAdvance(WritableBuffer, int)], TODO, or other
  /// buffer operations may change the buffer reference.
  public static byte[] buf(WritableBuffer<?> out) { return out.buf; }
}
