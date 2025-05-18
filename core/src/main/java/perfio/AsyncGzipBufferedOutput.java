package perfio;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class AsyncGzipBufferedOutput extends AsyncFilteringBufferedOutput {
  private final Deflater defl = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
  private final CRC32 crc = new CRC32();

  public AsyncGzipBufferedOutput(BufferedOutput parent, int depth) throws IOException {
    super(parent, true, depth, true, 0, 0, null);
    GzipUtil.writeHeader(parent);
  }

  public AsyncGzipBufferedOutput(BufferedOutput parent) throws IOException { this(parent, 3); }

  @Override protected void finish() throws IOException {
    if(!defl.finished()) {
      try {
        defl.finish();
        while(!defl.finished()) {
          parent.ensureAvailable(1);
          parent.pos += defl.deflate(parent.buf, parent.pos, parent.lim - parent.pos);
        }
        GzipUtil.writeTrailer(parent, crc.getValue(), defl.getTotalIn());
      } finally { defl.end(); }
    }
  }

  protected void filterAsync(Task t) {
    if(t.state == Task.STATE_NEW) {
      crc.update(t.buf, t.start, t.end - t.start);
      defl.setInput(t.buf, t.start, t.end - t.start);
    }
    var o = t.to;
    o.pos += defl.deflate(o.buf, o.start, o.buf.length-o.start);
    if(defl.needsInput()) t.consume();
  }
}
