package perfio;

import java.io.IOException;

abstract class BlockFlushingBufferedOutput extends TopLevelBufferedOutput {

  BlockFlushingBufferedOutput(boolean bigEndian, int initialBufferSize) {
    super(new byte[initialBufferSize], bigEndian, 0, 0, initialBufferSize, initialBufferSize, false, Long.MAX_VALUE, null, true);
  }

  void flushBlocks(boolean forceFlush) throws IOException {
    //TODO split buffer to forceFlush
    while(next != this) {
      var b = next;
      if(b.state != STATE_CLOSED) return;
      var blen = b.pos - b.start;
      if(blen != 0) {
        b.unlinkOnly();
        put(b);
      } else b.unlinkAndReturn();
    }
    if(state == STATE_CLOSED) put(this);
  }

  abstract void put(BufferedOutput b) throws IOException;
}
