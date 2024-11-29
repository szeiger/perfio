package perfio;

import java.io.IOException;

abstract class BlockFlushingBufferedOutput extends CacheRootBufferedOutput {

  BlockFlushingBufferedOutput(boolean bigEndian, int initialBufferSize, int blockDepth) {
    super(new byte[initialBufferSize], bigEndian, 0, 0, initialBufferSize, initialBufferSize, false, Long.MAX_VALUE);
  }

  void flushBlocks(boolean forceFlush) throws IOException {
    //TODO split buffer to forceFlush
    while(next != this) {
      var b = next;
      if(!b.closed) return;
      var blen = b.pos - b.start;
      if(b.sharing == SHARING_LEFT) {
        var n = b.next;
        n.start = b.start;
        n.totalFlushed -= blen;
        b.unlinkAndReturn();
      } else if(blen != 0) {
        b.unlinkOnly();
        put(b);
      } else b.unlinkAndReturn();
    }
    if(closed) put(this);
  }

  abstract void put(BufferedOutput b) throws IOException;

  boolean preferSplit() { return true; }
}
