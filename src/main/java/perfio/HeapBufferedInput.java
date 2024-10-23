package perfio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static perfio.BufferUtil.*;

class HeapBufferedInput extends BufferedInput {
  byte[] buf;
  final InputStream in;
  final int minRead;

  HeapBufferedInput(byte[] buf, ByteBuffer bb, int pos, int lim, long totalReadLimit, InputStream in, int minRead, BufferedInput parent) {
    super(bb, pos, lim, totalReadLimit, in, parent);
    this.buf = buf;
    this.in = in;
    this.minRead = minRead;
  }

  void copyBufferFrom(BufferedInput b) {
    buf = ((HeapBufferedInput)b).buf;
  }

  protected void clearBuffer() {
    buf = null;
  }

  private int fillBuffer() throws IOException {
    var read = in.read(buf, lim, buf.length - lim);
    if(read > 0) {
      totalBuffered += read;
      lim += read;
      if(totalBuffered >= totalReadLimit) {
        excessRead = (int)(totalBuffered-totalReadLimit);
        lim -= excessRead;
        totalBuffered -= excessRead;
      }
    }
    return read;
  }

  void prepareAndFillBuffer(int count) throws IOException {
    checkState();
    if(in != null && totalBuffered < totalReadLimit) {
      if(pos + count > buf.length || pos >= buf.length-minRead) {
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
          bb = ByteBuffer.wrap(buf).order(bb.order());
        } else if (a > 0 && pos != offset) {
          System.arraycopy(buf, pos, buf, offset, a);
        }
        pos = offset;
        lim = a + offset;
      }
      while(fillBuffer() >= 0 && available() < count) {}
    }
  }

  public void bytes(byte[] a, int off, int len) throws IOException {
    var tot = totalBytesRead() + len;
    if(tot < 0 || tot > totalReadLimit) throw new EOFException();
    var copied = Math.min(len, available());
    if(copied > 0) {
      System.arraycopy(buf, pos, a, off, copied);
      pos += copied;
    }
    var rem = len - copied;
    while(rem >= minRead && in != null) {
      var r = in.read(a, off + copied, rem);
      if(r <= 0) throw new EOFException();
      totalBuffered += r;
      copied += r;
      rem -= r;
    }
    if(rem > 0) {
      var p = fwd(rem);
      System.arraycopy(buf, p, a, off + copied, rem);
    }
  }

  protected BufferedInput createEmptyView() { return new HeapBufferedInput(null, null, 0, 0, 0, in, minRead, this); }

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
      if(buf[pos-1] != 0) throwFormatError("Missing \\0 terminator in string");
      return len == 1 ? "" : new String(buf, p, len-1, charset);
    }
  }

  public long skip(long bytes) throws IOException {
    checkState();
    var base = 0L;
    while(true) {
      if(bytes <= 0) return base;
      var skipAv = (int)Math.min(bytes, available());
      pos += skipAv;
      var rem = bytes - skipAv;
      if(rem <= 0 || in == null) return base + skipAv;
      var remTotal = totalReadLimit - totalBuffered;
      rem = Math.min(rem, remTotal);
      rem -= trySkipIn(rem); //TODO have a minSkip similar to minRead?
      if(rem <= 0) return base + bytes;
      request((int)Math.min(rem, buf.length));
      if(available() <= 0) return base + bytes - rem;
      base = base + bytes - rem;
      bytes = rem;
    }
  }

  // repeatedly try in.skip(); may return early even when more data is available
  private long trySkipIn(long b) throws IOException {
    var skipped = 0L;
    while(skipped < b) {
      var l = in.skip(b-skipped);
      if(l <= 0) return skipped;
      totalBuffered += l;
      skipped += l;
    }
    return skipped;
  }
}
