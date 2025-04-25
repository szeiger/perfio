package perfio;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class AsyncGzipBufferedOutput extends AsyncFilteringBufferedOutput {
  private final Deflater defl = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
  private final CRC32 crc = new CRC32();

  public AsyncGzipBufferedOutput(BufferedOutput parent, int depth) throws IOException {
    super(parent, true, depth, true);
    writeGzipHeader(parent);
  }

  public AsyncGzipBufferedOutput(BufferedOutput parent) throws IOException { this(parent, 2); }

  @Override void finish() throws IOException {
    if(!defl.finished()) {
      try {
        defl.finish();
        while(!defl.finished()) {
          parent.ensureAvailable(1);
          parent.pos += defl.deflate(parent.buf, parent.pos, parent.lim - parent.pos);
        }
        writeGzipTrailer(parent);
      } finally { defl.end(); }
    }
  }

  protected void filterAsync(Task t) {
    var b = t.from;
    if(!t.continuation) {
      crc.update(b.buf, b.start, b.pos - b.start);
      defl.setInput(b.buf, b.start, b.pos - b.start);
    }
    var o = t.to;
    o.pos += defl.deflate(o.buf, o.start, o.buf.length-o.start);
    if(defl.needsInput()) b.start = b.pos;
  }

  private void writeGzipHeader(BufferedOutput b) throws IOException {
    b.int64l(0x88b1f).int16l((short)0xff00);
  }

  private void writeGzipTrailer(BufferedOutput b) throws IOException {
    b.int32l((int)crc.getValue()).int32l(defl.getTotalIn());
  }
}
