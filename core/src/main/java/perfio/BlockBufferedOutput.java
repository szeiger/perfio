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
    super(new byte[initialBufferSize], bigEndian, 0, 0, initialBufferSize, initialBufferSize, false, Long.MAX_VALUE, true);
  }

  @Override
  public BlockBufferedOutput order(ByteOrder order) {
    super.order(order);
    return this;
  }

  void flushBlocks(boolean forceFlush) throws IOException {}

  public InputStream toInputStream() throws IOException {
    return new BufferIteratorInputStream(bufferIterator());
  }

  public BufferedInput toBufferedInput() throws IOException {
    return new SwitchingHeapBufferedInput(bufferIterator(), bigEndian);
  }

  BufferIterator bufferIterator() throws IOException {
    if(state != BufferedOutput.STATE_CLOSED) throw new IOException("Cannot access buffer before closing");
    return new BlockBufferedOutputIterator(this);
  }
}


final class BlockBufferedOutputIterator extends BufferIterator {
  private BufferedOutput block;
  private final BlockBufferedOutput root;

  BlockBufferedOutputIterator(BlockBufferedOutput root) { this.root = root; }

  private BufferedOutput skipEmpty(BufferedOutput b) {
    while(true) {
      if(b.pos - b.start > 0) return b;
      if(b == root) return null;
      b = b.next;
    }
  }

  public Object next() throws IOException {
    if(block == root) return null;
    var b = skipEmpty(block == null ? root.next : block.next);
    if(b == null) return null;
    block = b;
    return b;
  }

  public void returnBuffer(Object id) {}

  public byte[] buffer() { return block.buf; }
  public int start() { return block.start; }
  public int end() { return block.pos; }
};
