package perfio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Objects;

public final class BlockBufferedOutput extends CacheRootBufferedOutput {
  private boolean retrieved = false;

  BlockBufferedOutput(boolean bigEndian, int initialBufferSize) {
    super(new byte[initialBufferSize], bigEndian, 0, 0, initialBufferSize, initialBufferSize, false, Long.MAX_VALUE);
  }

  @Override
  public BlockBufferedOutput order(ByteOrder order) {
    super.order(order);
    return this;
  }

  void flushBlocks(boolean forceFlush) throws IOException {}

  void flushUpstream() {}

  @Override
  void closeUpstream() throws IOException {
    super.closeUpstream();
    if(next.pos <= next.start && next != this) nextBuffer();
  }

  private void checkRetrievalState() throws IOException {
    if(!closed) throw new IOException("Cannot access buffer before closing");
    if(retrieved) throw new IOException("Buffer was already retrieved");
  }

  @Override
  void flushAndGrow(int count) throws IOException {
    // switch to a new buffer if this one is sufficiently filled
    if(lim-pos <= initialBufferSize/2) {
      checkState();
      var b = cacheRoot.getExclusiveBlock();
      totalFlushed += (pos-start);
      buf = b.reinit(buf, bigEndian, start, pos, lim, sharing, 0L, 0L, true, root, null);
      b.closed = true;
      b.insertBefore(this);
      start = 0;
      pos = 0;
      lim = buf.length;
    } else super.flushAndGrow(count);
  }

  public InputStream toInputStream() throws IOException {
    checkRetrievalState();
    retrieved = true;
    return new BlockBufferedInputStream(this);
  }

  public BufferedInput toBufferedInput() throws IOException {
    checkRetrievalState();
    retrieved = true;
    //return BufferedInput.of(new BlockBufferedInputStream(this), initialBufferSize); //TODO don't copy buffers
    return new SwitchingHeapBufferedInput(this);
  }

  /// Advance to the next non-empty buffer (if there is one), merging and skipping SHARING_LEFT
  /// buffers along the way.
  /// @return true for success or false if the end of the data has been reached.
  boolean nextBuffer() {
    var c = next;
    if(c == this) return false;
    c.unlinkOnly();
    while(true) {
      var b = next;
      var blen = b.pos - b.start;
      if(blen <= 0 && b != this) {
        b.unlinkOnly();
      } else if(b.sharing == SHARING_LEFT) {
        var n = b.next;
        n.start = b.start;
        n.totalFlushed -= blen;
        b.unlinkOnly();
      } else return blen > 0;
    }
  }
}


final class BlockBufferedInputStream extends InputStream {
  private final BlockBufferedOutput bo;
  private byte[] buf;
  private int pos, lim;

  BlockBufferedInputStream(BlockBufferedOutput bo) {
    this.bo = bo;
    updateBuffer();
  }

  private void updateBuffer() {
    var n = bo.next;
    buf = n.buf;
    pos = n.start;
    lim = n.pos;
  }

  // Make data available in the current buffer, advancing it if necessary. Returns
  // false if the end of the data has been reached.
  private boolean request() {
    if(lim - pos <= 0) {
      if(!bo.nextBuffer()) return false;
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
