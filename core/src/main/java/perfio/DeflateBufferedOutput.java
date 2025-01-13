package perfio;

import java.io.IOException;
import java.util.zip.Deflater;

public class DeflateBufferedOutput extends FilteringBufferedOutput {
  private final Deflater defl = new Deflater();

  public DeflateBufferedOutput(BufferedOutput parent) { super(parent); }

  @Override void finish() throws IOException { defl.end(); }

  void finalizeBlock(BufferedOutput b, boolean blocking) {
    var out = b.topLevel.getExclusiveBlock();
    defl.setInput(b.buf, b.start, b.pos - b.start);
    defl.finish();
    defl.deflate(out.buf, 0, out.buf.length, Deflater.NO_FLUSH);
    defl.needsInput();
    //xxx;
    b.state = BufferedOutput.STATE_CLOSED;
  }
}
