package perfio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

final class StreamingHeapBufferedInput extends HeapBufferedInput {
  final InputStream in;
  final int minRead;

  StreamingHeapBufferedInput(byte[] buf, int pos, int lim, long totalReadLimit, InputStream in, int minRead, BufferedInput viewParent, boolean bigEndian) {
    super(buf, pos, lim, totalReadLimit, viewParent, bigEndian, in);
    this.in = in;
    this.minRead = minRead;
  }

  private int fillBuffer() throws IOException {
    var read = in.read(buf, lim, buf.length - lim);
    if(read > 0) {
      totalBuffered += read;
      lim += read;
      if(totalBuffered >= totalReadLimit) {
        excessRead = (int)(totalBuffered - totalReadLimit);
        lim -= excessRead;
        totalBuffered -= excessRead;
      }
    }
    return read;
  }

  protected void prepareAndFillBuffer(int count) throws IOException {
    checkState();
    if(in != null && totalBuffered < totalReadLimit) {
      if(pos + count > buf.length || pos >= buf.length - minRead)
        shiftOrGrow(count);
      while(fillBuffer() >= 0 && available() < count) {}
    }
  }

  BufferedInput createEmptyView() {
    return new StreamingHeapBufferedInput(null, 0, 0, 0, in, minRead, this, bigEndian);
  }

  @Override
  public void bytes(byte[] a, int off, int len) throws IOException {
    var tot = totalBytesRead() + len;
    if(tot < 0 || tot > totalReadLimit) throw new EOFException();
    var copied = Math.min(len, available());
    if(copied > 0) {
      System.arraycopy(buf, pos, a, off, copied);
      pos += copied;
    }
    var rem = len - copied;
    // Copy directly without buffering
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

  @Override
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
      // Skip directly without buffering
      rem -= trySkipIn(rem); //TODO have a minSkip similar to minRead?
      if(rem <= 0) return base + bytes;
      tryFwd((int)Math.min(rem, buf.length));
      if(available() <= 0) return base + bytes - rem;
      base = base + bytes - rem;
      bytes = rem;
    }
  }

  // repeatedly try in.skip(); may return early even when more data is available
  private long trySkipIn(long b) throws IOException {
    var skipped = 0L;
    while(skipped < b) {
      var l = in.skip(b - skipped);
      if(l <= 0) return skipped;
      totalBuffered += l;
      skipped += l;
    }
    return skipped;
  }
}
