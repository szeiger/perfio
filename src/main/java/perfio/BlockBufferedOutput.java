package perfio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/// A [BufferedOutput] which collects data in one or more byte array segments. The data can be
/// accessed with [#toBufferedInput()] or [#toInputStream()] after closing the buffer.
/// 
/// Instances are created with [BufferedOutput#ofBlocks(int)] (and its overloads).
final class BlockBufferedOutput extends AccumulatingBufferedOutput {

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
    return new BufferIteratorInputStream(bufferIterator());
  }

  public BufferedInput toBufferedInput() throws IOException {
    return new SwitchingHeapBufferedInput(bufferIterator(), bigEndian);
  }

  BufferIterator bufferIterator() throws IOException {
    if(!closed) throw new IOException("Cannot access buffer before closing");
    return new BlockBufferedOutputIterator(this);
  }
}


final class BlockBufferedOutputIterator extends BufferIterator {
  private BufferedOutput block;
  private final BufferedOutput root;

  BlockBufferedOutputIterator(BufferedOutput root) {
    this.root = root;
    this.block = skipEmpty(root.next);
    if(this.block == null) this.block = root.next;
  }

  private BufferedOutput skipEmpty(BufferedOutput b) {
    while(true) {
      if(b.sharing == BufferedOutput.SHARING_LEFT) {
        var n = b.next;
        n.start = b.start;
      } else if(b.pos - b.start > 0) return b;
      if(b == root) return null;
      b = b.next;
    }
  }

  public boolean next() {
    if(block == root) return false;
    var b = skipEmpty(block.next);
    if(b == null) return false;
    block = b;
    return true;
  }

  public byte[] buffer() { return block.buf; }
  public int start() { return block.start; }
  public int end() { return block.pos; }
};
