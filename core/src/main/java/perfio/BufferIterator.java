package perfio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/// An iterator over a list of byte array buffers. It is initially positioned before the first
/// buffer. Calling [#next()] advances it to the next buffer. All buffers must be non-empty. An
/// empty iterator contains no buffers.
/// 
/// After a buffer has been handed over with [#next()] it may be modified by the consumer, both
/// inside and outside the bounds indicated by [#start()] and [#end()]. Consumers should return
/// buffers after use with [#returnBuffer(Object)] so they can be reused more efficiently than
/// having to garbage-collect and reallocate them.
abstract class BufferIterator {

  /// Advance to the next non-empty buffer and return a buffer id, or null if the end of the list
  /// has been reached.
  public abstract Object next() throws IOException;

  /// The current buffer.
  /// The behavior is undefined before successfully retrieving a buffer with [#next()].
  public abstract byte[] buffer();
  
  /// The first used index in the current buffer.
  /// The behavior is undefined before successfully retrieving a buffer with [#next()].
  public abstract int start();
  
  /// The index after the last used index in the current buffer.
  /// The behavior is undefined before successfully retrieving a buffer with [#next()].
  public abstract int end();
  
  /// The number of bytes in the current buffer, equivalent to `end() - start()`
  /// The behavior is undefined before successfully retrieving a buffer with [#next()].
  public int length() { return end() - start(); }

  /// Return the buffer with the given id so it can be reused py the producer.
  /// Buffers may be returned out of order.
  public abstract void returnBuffer(Object id);
}


final class BufferIteratorInputStream extends InputStream {
  private final BufferIterator it;
  private byte[] buf;
  private Object bufferId;
  private int pos, lim;

  BufferIteratorInputStream(BufferIterator it) { this.it = it; }

  // Make data available in the current buffer, advancing it if necessary. Returns
  // false if the end of the data has been reached.
  private boolean request() throws IOException {
    if(lim - pos <= 0) {
      if(bufferId != null) it.returnBuffer(bufferId);
      if((bufferId = it.next()) == null) return false;
      buf = it.buffer();
      pos = it.start();
      lim = it.end();
    }
    return true;
  }

  public int read() throws IOException {
    if(!request()) return -1;
    return buf[pos++] & 0xFF;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, b.length);
    if(len == 0) return 0;
    if(!request()) return -1;
    var r = Math.min(len, available());
    System.arraycopy(buf, pos, b, off, r);
    pos += r;
    return r;
  }

  @Override
  public long skip(long n) throws IOException {
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
