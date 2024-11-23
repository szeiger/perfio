package perfio;

import perfio.internal.BufferUtil;
import perfio.internal.MemoryAccessor;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

// This could be a lot simpler if we didn't have to do pagination but ByteBuffer is limited
// to 2 GB and direct MemorySegment access is much, much slower as of JDK 22.
final class DirectBufferedInput extends BufferedInput {
  MemorySegment bbSegment;
  final MemorySegment ms;
  private final LineBuffer linebuf;
  private ByteBuffer bb;

  DirectBufferedInput(ByteBuffer bb, MemorySegment bbSegment, int pos, int lim, long totalReadLimit, MemorySegment ms, Closeable closeable, BufferedInput parent, LineBuffer linebuf) {
    super(pos, lim, totalReadLimit, closeable, parent, bb.order() == ByteOrder.BIG_ENDIAN);
    this.bbSegment = bbSegment;
    this.ms = ms;
    this.linebuf = linebuf;
    this.bb = bb;
  }

  long bbStart = 0L;

  void copyBufferFrom(BufferedInput b) {
    var db = (DirectBufferedInput)b;
    bb = db.bb;
    bbSegment = db.bbSegment;
    bbStart = db.bbStart;
  }

  void clearBuffer() {
    bb = null;
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

  BufferedInput createEmptyView() { return new DirectBufferedInput(bb, null, 0, 0, 0L, ms, null, this, linebuf); }

  private String makeString(int start, int len, Charset charset) {
    var lb = linebuf.get(len);
    bb.get(start, lb, 0, len);
    return new String(lb, 0, len, charset);
  }

  public byte int8() throws IOException { var p = fwd(1); return MemoryAccessor.INSTANCE.int8(bb, p); }

  public short int16()  throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.int16(bb, p, bigEndian); }
  public short int16n() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.int16n(bb, p); }
  public short int16b() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.int16b(bb, p); }
  public short int16l() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.int16l(bb, p); }

  public char uint16()  throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.uint16(bb, p, bigEndian); }
  public char uint16n() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.uint16n(bb, p); }
  public char uint16b() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.uint16b(bb, p); }
  public char uint16l() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.uint16l(bb, p); }

  public int int32()  throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.int32(bb, p, bigEndian); }
  public int int32n() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.int32n(bb, p); }
  public int int32b() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.int32b(bb, p); }
  public int int32l() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.int32l(bb, p); }

  public long int64()  throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.int64(bb, p, bigEndian); }
  public long int64n() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.int64n(bb, p); }
  public long int64b() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.int64b(bb, p); }
  public long int64l() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.int64l(bb, p); }

  public float float32()  throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.float32(bb, p, bigEndian); }
  public float float32n() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.float32n(bb, p); }
  public float float32b() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.float32b(bb, p); }
  public float float32l() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.float32l(bb, p); }

  public double float64()  throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.float64(bb, p, bigEndian); }
  public double float64n() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.float64n(bb, p); }
  public double float64b() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.float64b(bb, p); }
  public double float64l() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.float64l(bb, p); }

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

  /// Move this buffer to the given absolute position, updating the ByteBuffer and local position if necessary.
  /// If the position is beyond the absolute limit, the buffer is moved to the limit instead.
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
