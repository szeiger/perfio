package perfio;

import perfio.internal.BufferUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

/// A [BufferedOutput] which accumulates all data in a single byte array. The size is limited to
/// [BufferUtil#SOFT_MAX_ARRAY_LENGTH]. The array can be a fixed array segment provided upon
/// construction, or reallocated dynamically from an initial size. It can be accessed with
/// [#buffer()], [#copyToByteArray()], [#toBufferedInput()] or [#toInputStream()] once the buffer
/// is closed.
/// 
/// Instances are created with [BufferedOutput#growing(int)] or
/// [BufferedOutput#ofArray(byte\[\], int, int, int)] (and their overloads).
public final class ArrayBufferedOutput extends AccumulatingBufferedOutput {

  ArrayBufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, int initialBufferSize, boolean fixed) {
    super(buf, bigEndian, start, pos, lim, initialBufferSize, fixed, Long.MAX_VALUE);
  }

  private byte[] outbuf = null;
  private int outstart, outpos = 0;

  @Override
  public ArrayBufferedOutput order(ByteOrder order) {
    super.order(order);
    return this;
  }

  void flushBlocks(boolean forceFlush) throws IOException {
    while(next != this) {
      var b = next;
      if(!b.closed) return;
      var blen = b.pos - b.start;
      if(blen > 0) flushSingle(b, true);
      else b.unlinkAndReturn();
    }
  }

  private void flushSingle(BufferedOutput b, boolean unlink) throws IOException {
    var blen = b.pos - b.start;
    if(outbuf == null) {
      outbuf = b.buf;
      outstart = b.start;
      outpos = b.pos;
      if(unlink) b.unlinkOnly();
    } else {
      var outrem = outbuf.length - outpos;
      if(outrem < blen) {
        if(blen + outpos < 0) throw new IOException("Buffer exceeds maximum array length");
        growOutBuffer((int)(blen + outpos));
      }
      System.arraycopy(b.buf, b.start, outbuf, outpos, blen);
      outpos += blen;
      if(unlink) b.unlinkAndReturn();
    }
  }

  private void growOutBuffer(int target) {
    var buflen = BufferUtil.growBuffer(outbuf.length, target, 1);
    if(buflen > outbuf.length) outbuf = Arrays.copyOf(outbuf, buflen);
  }

  boolean preferSplit() { return false; }

  @Override
  void closeUpstream() throws IOException {
    flushSingle(this, false);
    super.closeUpstream();
  }

  private void checkClosed() throws IOException {
    if(!closed) throw new IOException("Cannot access buffer before closing");
  }

  /// Copy the data to a newly allocated byte array of the exact size.
  public byte[] copyToByteArray() throws IOException { return Arrays.copyOfRange(buffer(), outstart, outpos); }

  /// Returns the buffer. This method should be used together with [#start()] and [#length()]
  /// to get the array segment that contains the data.
  public byte[] buffer() throws IOException {
    checkClosed();
    return outbuf;
  }

  /// Returns the first used index within the buffer. This will always be 0 for a growing buffer.
  /// A buffer created with [BufferedOutput#ofArray(byte\[\], int, int, int)] can have a non-zero
  /// starting index.
  public int start() throws IOException {
    checkClosed();
    return outstart;
  }

  /// Returns the index after the last used index in the buffer.
  public int end() throws IOException {
    checkClosed();
    return outpos;
  }

  /// Returns the number of bytes in the buffer, equivalent to `end() - start()`.
  public int length() throws IOException {
    checkClosed();
    return outpos - outstart;
  }

  /// Create a new [BufferedInput] that can read the data. This method does not consume the data
  /// and can be used repeatedly or together with other buffer access methods. 
  public BufferedInput toBufferedInput() throws IOException {
    return BufferedInput.ofArray(buffer(), outstart, length());
  }

  /// Create a new [InputStream] that can read the data. This method does not consume the data
  /// and can be used repeatedly or together with other buffer access methods. 
  public InputStream toInputStream() throws IOException {
    return new ByteArrayInputStream(buffer(), outstart, length());
  }
}
