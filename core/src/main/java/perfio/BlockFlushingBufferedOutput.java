package perfio;

import java.io.IOException;

abstract class BlockFlushingBufferedOutput extends TopLevelBufferedOutput {

  BlockFlushingBufferedOutput(boolean bigEndian, int initialBufferSize) {
    super(new byte[initialBufferSize], bigEndian, 0, 0, initialBufferSize, initialBufferSize, false, Long.MAX_VALUE, null);
  }

  /// Check if the given buffer is closed. This method returns `false` if the buffer is in
  /// [#STATE_OPEN]. If `blocking == false`, it also returns `false` when the buffer is in
  /// [#STATE_PROCESSING], otherwise it waits until it has reached [#STATE_CLOSED].
  boolean checkClosed(BufferedOutput b, boolean blocking) throws IOException { return b.state == STATE_CLOSED; }

  void flushBlocks(boolean forceFlush) throws IOException {
    //TODO split buffer to forceFlush
    while(next != this) {
      var b = next;
      if(!checkClosed(b, forceFlush)) return;
      var blen = b.pos - b.start;
      if(blen != 0) {
        b.unlinkOnly();
        put(b);
      } else b.unlinkAndReturn();
    }
    if(checkClosed(this, forceFlush)) put(this);
  }

  abstract void put(BufferedOutput b) throws IOException;

  boolean preferSplit() { return true; }
}
