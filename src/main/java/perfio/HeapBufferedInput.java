package perfio;

import perfio.internal.BufferUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;

import static perfio.internal.BufferUtil.*;

sealed abstract class HeapBufferedInput extends BufferedInput permits StreamingHeapBufferedInput, SwitchingHeapBufferedInput {
  byte[] buf;

  HeapBufferedInput(byte[] buf, int pos, int lim, long totalReadLimit, Closeable closeable, BufferedInput parent, boolean bigEndian) {
    super(pos, lim, totalReadLimit, closeable, parent, bigEndian);
    this.buf = buf;
  }

  void copyBufferFrom(BufferedInput b) {
    buf = ((HeapBufferedInput)b).buf;
  }

  void clearBuffer() {
    buf = null;
  }

  public byte int8() throws IOException {
    var p = fwd(1);
    return buf[p];
  }

  public short int16() throws IOException {
    var p = fwd(2);
    return (short)(bigEndian ? BA_SHORT_BIG : BA_SHORT_LITTLE).get(buf, p);
  }

  public char uint16() throws IOException {
    var p = fwd(2);
    return (char)(bigEndian ? BA_CHAR_BIG : BA_CHAR_LITTLE).get(buf, p);
  }

  public int int32() throws IOException {
    var p = fwd(4);
    return (int)(bigEndian ? BA_INT_BIG : BA_INT_LITTLE).get(buf, p);
  }

  public long int64() throws IOException {
    var p = fwd(8);
    return (long)(bigEndian ? BA_LONG_BIG : BA_LONG_LITTLE).get(buf, p);
  }

  public float float32() throws IOException {
    var p = fwd(4);
    return (float)(bigEndian ? BA_FLOAT_BIG : BA_FLOAT_LITTLE).get(buf, p);
  }

  public double float64() throws IOException {
    var p = fwd(8);
    return (double)(bigEndian ? BA_DOUBLE_BIG : BA_DOUBLE_LITTLE).get(buf, p);
  }

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
