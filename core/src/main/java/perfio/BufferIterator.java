package perfio;

import java.io.InputStream;
import java.util.Objects;

/// An iterator over a list of byte array buffers. It is initially positioned
/// on the first buffer upon creation. Calling [#next()] advances it to the next
/// buffer. An empty iterator consists of a single empty buffer, otherwise all
/// buffers are non-empty.
abstract class BufferIterator {

  /// Advance to the next non-empty buffer and return `true`, or return `false`
  /// if the end of the list has been reached.
  public abstract boolean next();

  /// The current buffer
  public abstract byte[] buffer();
  
  /// The first used index in the current buffer
  public abstract int start();
  
  /// The index after the last used index in the current buffer
  public abstract int end();
  
  /// The number of bytes in the current buffer, equivalent to `end() - start()`
  public int length() { return end() - start(); }
}


final class BufferIteratorInputStream extends InputStream {
  private final BufferIterator it;
  private byte[] buf;
  private int pos, lim;

  BufferIteratorInputStream(BufferIterator it) {
    this.it = it;
    updateBuffer();
  }

  private void updateBuffer() {
    buf = it.buffer();
    pos = it.start();
    lim = it.end();
  }

  // Make data available in the current buffer, advancing it if necessary. Returns
  // false if the end of the data has been reached.
  private boolean request() {
    if(lim - pos <= 0) {
      if(!it.next()) return false;
      updateBuffer();
    }
    return true;
  }

  public int read() {
    if(!request()) return -1;
    return buf[pos++] & 0xFF;
  }

  @Override
  public int read(byte[] b, int off, int len) {
    Objects.checkFromIndexSize(off, len, b.length);
    if(len == 0) return 0;
    if(!request()) return -1;
    var r = Math.min(len, available());
    System.arraycopy(buf, pos, b, off, r);
    pos += r;
    return r;
  }

  @Override
  public long skip(long n) {
    long rem = n;
    while(true) {
      if(rem <= available()) {
        pos += (int)rem;
        return n;
      } else {
        rem -= available();
        if(!request()) return n-rem;
      }
    }
  }

  @Override
  public int available() { return lim - pos; }
}
