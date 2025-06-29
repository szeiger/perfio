package perfio;

import perfio.internal.BufferUtil;
import perfio.internal.MemoryAccessor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;

abstract non-sealed class HeapBufferedInput extends BufferedInput {
  byte[] buf;

  HeapBufferedInput(byte[] buf, int pos, int lim, long totalReadLimit, BufferedInput parent, boolean bigEndian) {
    super(pos, lim, totalReadLimit, parent, bigEndian);
    this.buf = buf;
  }

  void copyBufferFrom(BufferedInput b) {
    buf = ((HeapBufferedInput)b).buf;
  }

  void clearBuffer() {
    buf = null;
  }

  public byte int8() throws IOException { var p = fwd(1); return MemoryAccessor.INSTANCE.int8(buf, p); }

  public short int16()  throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.int16(buf, p, bigEndian); }
  public short int16n() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.int16n(buf, p); }
  public short int16b() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.int16b(buf, p); }
  public short int16l() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.int16l(buf, p); }

  public char uint16()  throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.uint16(buf, p, bigEndian); }
  public char uint16n() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.uint16n(buf, p); }
  public char uint16b() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.uint16b(buf, p); }
  public char uint16l() throws IOException { var p = fwd(2); return MemoryAccessor.INSTANCE.uint16l(buf, p); }

  public int int32()  throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.int32(buf, p, bigEndian); }
  public int int32n() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.int32n(buf, p); }
  public int int32b() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.int32b(buf, p); }
  public int int32l() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.int32l(buf, p); }

  public long int64()  throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.int64(buf, p, bigEndian); }
  public long int64n() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.int64n(buf, p); }
  public long int64b() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.int64b(buf, p); }
  public long int64l() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.int64l(buf, p); }

  public float float32()  throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.float32(buf, p, bigEndian); }
  public float float32n() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.float32n(buf, p); }
  public float float32b() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.float32b(buf, p); }
  public float float32l() throws IOException { var p = fwd(4); return MemoryAccessor.INSTANCE.float32l(buf, p); }

  public double float64()  throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.float64(buf, p, bigEndian); }
  public double float64n() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.float64n(buf, p); }
  public double float64b() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.float64b(buf, p); }
  public double float64l() throws IOException { var p = fwd(8); return MemoryAccessor.INSTANCE.float64l(buf, p); }

  public String string(int len, Charset charset) throws IOException {
    if(len == 0) {
      checkState();
      return "";
    } else {
      var p = fwd(len);
      return new String(buf, p, len, charset);
    }
  }

  public String zstring(int len, Charset charset) throws IOException {
    if(len == 0) {
      checkState();
      return "";
    } else {
      var p = fwd(len);
      if(buf[pos-1] != 0) throw new IOException("Missing \\0 terminator in string");
      return len == 1 ? "" : new String(buf, p, len-1, charset);
    }
  }

  /// Shift the remaining buffer data to the left and/or reallocate the buffer to make room for
  /// `count` bytes past the current `pos`.
  void shiftOrGrow(int count) {
    var a = available();
    // Buffer shifts must be aligned to the vector size, otherwise VectorizedLineTokenizer
    // performance will tank after rebuffering even when all vector reads are aligned.
    var offset = a > 0 ? pos % BufferUtil.VECTOR_LENGTH : 0;
    if(count + offset > buf.length) {
      var buflen = buf.length;
      while(buflen < count + offset) buflen *= 2;
      var buf2 = new byte[buflen];
      if(a > 0) System.arraycopy(buf, pos, buf2, offset, a);
      buf = buf2;
    } else if(a > 0 && pos != offset) {
      System.arraycopy(buf, pos, buf, offset, a);
    }
    pos = offset;
    lim = a + offset;
  }
}
