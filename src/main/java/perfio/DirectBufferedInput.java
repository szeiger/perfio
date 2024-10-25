package perfio;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import static perfio.BufferUtil.*;

// This could be a lot simpler if we didn't have to do pagination but ByteBuffer is limited
// to 2 GB and direct MemorySegment access is much, much slower as of JDK 22.
final class DirectBufferedInput extends BufferedInput {
  MemorySegment bbSegment;
  final MemorySegment ms;
  private final byte[][] linebuf;

  DirectBufferedInput(ByteBuffer bb, MemorySegment bbSegment, int pos, int lim, long totalReadLimit, MemorySegment ms, Closeable closeable, BufferedInput parent, byte[][] linebuf) {
    super(bb, pos, lim, totalReadLimit, closeable, parent);
    this.bbSegment = bbSegment;
    this.ms = ms;
    this.linebuf = linebuf;
  }

  long bbStart = 0L;

  void copyBufferFrom(BufferedInput b) {
    var db = (DirectBufferedInput)b;
    bbSegment = db.bbSegment;
    bbStart = db.bbStart;
  }

  void clearBuffer() {
    bbSegment = null;
  }

  void prepareAndFillBuffer(int count) throws IOException {
    checkState();
    if(ms != null && totalBuffered < totalReadLimit) {
      var a = available();
      var newStart = parentTotalOffset+totalBuffered-a;
      var newLen = Math.max(Math.min(totalReadLimit+parentTotalOffset-newStart, MaxDirectBufferSize), 0);
      bbSegment = ms.asSlice(newStart, newLen);
      bb = bbSegment.asByteBuffer().order(bb.order());
      bbStart = newStart;
      pos = 0;
      lim = (int)newLen;
      totalBuffered += newLen - a;
      if(totalBuffered >= totalReadLimit) {
        excessRead = (int)(totalBuffered-totalReadLimit);
        lim -= excessRead;
        totalBuffered -= excessRead;
      }
    }
  }

  public void bytes(byte[] a, int off, int len) throws IOException {
    var p = fwd(len);
    bb.get(p, a, off, len);
  }

  BufferedInput createEmptyView() { return new DirectBufferedInput(null, null, 0, 0, 0L, ms, null, this, linebuf); }

  private byte[] extendBuffer(int len) {
    var buflen = linebuf[0].length;
    while(buflen < len) buflen *= 2;
    return new byte[buflen];
  }

  private String makeString(int start, int len, Charset charset) {
    var lb = linebuf[0];
    if(lb.length < len) {
      lb = extendBuffer(len);
      linebuf[0] = lb;
    }
    bb.get(start, lb, 0, len);
    return new String(lb, 0, len, charset);
  }

  public byte int8() throws IOException {
    var p = fwd(1);
    return bb.get(p);
  }

  public short int16() throws IOException {
    var p = fwd(2);
    return (short)(bigEndian ? BB_SHORT_BIG : BB_SHORT_LITTLE).get(bb, p);
  }

  public char uint16() throws IOException {
    var p = fwd(2);
    return (char)(bigEndian ? BB_CHAR_BIG : BB_CHAR_LITTLE).get(bb, p);
  }

  public int int32() throws IOException {
    var p = fwd(4);
    return (int)(bigEndian ? BB_INT_BIG : BB_INT_LITTLE).get(bb, p);
  }

  public long int64() throws IOException {
    var p = fwd(8);
    return (long)(bigEndian ? BB_LONG_BIG : BB_LONG_LITTLE).get(bb, p);
  }

  public float float32() throws IOException {
    var p = fwd(4);
    return (float)(bigEndian ? BB_FLOAT_BIG : BB_FLOAT_LITTLE).get(bb, p);
  }

  public double float64() throws IOException {
    var p = fwd(8);
    return (double)(bigEndian ? BB_DOUBLE_BIG : BB_DOUBLE_LITTLE).get(bb, p);
  }

  public String string(int len, Charset charset) throws IOException {
    if(len == 0) {
      checkState();
      return "";
    } else {
      var p = fwd(len);
      return makeString(p, len, charset);
    }
  }

  public String zstring(int len, Charset charset) throws IOException {
    if(len == 0) {
      checkState();
      return "";
    } else {
      var p = fwd(len);
      if(bb.get(pos-1) != 0) throw new IOException("Missing \\0 terminator in string");
      return len == 1 ? "" : makeString(p, len-1, charset);
    }
  }

  public long skip(long bytes) throws IOException {
    checkState();
    if(bytes > 0) {
      var skipAv = (int)Math.min(bytes, available());
      pos += skipAv;
      var rem = bytes - skipAv;
      var remTotal = totalReadLimit - totalBuffered;
      rem = Math.min(rem, remTotal);
      if(rem > 0 && ms != null) {
        var newStart = parentTotalOffset+totalBuffered+rem;
        var newLen = Math.min(totalReadLimit+parentTotalOffset-newStart, MaxDirectBufferSize);
        bb = ms.asSlice(newStart, newLen).asByteBuffer().order(bb.order());
        totalBuffered += rem + newLen;
        pos = 0;
        lim = (int)newLen;
        return skipAv + rem;
      } else return skipAv;
    } else return 0;
  }

  /** Move this buffer to the given absolute position, updating the ByteBuffer and local position if necessary.
   * If the position is beyond the absolute limit, the buffer is moved to the limit instead. */
  void reposition(long absPos) {
    absPos = Math.min(absPos, totalReadLimit);
    if(absPos < bbStart || absPos > bbStart + lim) {
      var offset = absPos % BufferUtil.VECTOR_LENGTH;
      var newStart = absPos - offset;
      var shift = newStart - bbStart;
      totalBuffered += shift;
      var newLen = Math.min(totalReadLimit+parentTotalOffset-newStart, MaxDirectBufferSize);
      bbSegment = ms.asSlice(newStart, newLen);
      bb = bbSegment.asByteBuffer().order(bb.order());
      bbStart = newStart;
      lim = (int)newLen;
    }
    pos = (int)(absPos - bbStart);
  }
}
