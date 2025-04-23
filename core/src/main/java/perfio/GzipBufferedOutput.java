package perfio;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class GzipBufferedOutput extends FilteringBufferedOutput {
  private final Deflater defl = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
  private final CRC32 crc = new CRC32();

  public GzipBufferedOutput(BufferedOutput parent) throws IOException {
    super(parent, true);
    writeGzipHeader(parent);
  }

  @Override void finish() throws IOException {
    if(!defl.finished()) {
      try {
        defl.finish();
        while(!defl.finished()) deflate();
        writeGzipTrailer(parent);
      } finally { defl.end(); }
    }
  }
  
  private void deflate() throws IOException {
    parent.ensureAvailable(1);
    var l = defl.deflate(parent.buf, parent.pos, parent.lim - parent.pos);
    parent.pos += l;
  }

  //TODO add a syncFlush mode that uses this
  private void flushDeflater() throws IOException {
    if(!defl.finished()) {
      while(true) {
        parent.ensureAvailable(1); //TODO Make this work if we've reached the limit; not really a problem for gzip because it always has to write a trailer anyway
        var l = defl.deflate(parent.buf, parent.pos, parent.lim - parent.pos, Deflater.SYNC_FLUSH);
        if(l == 0) break;
        parent.pos += l;
      }
    }
  }

  protected void filterBlock(BufferedOutput b) throws IOException {
    defl.setInput(b.buf, b.start, b.pos - b.start);
    while(!defl.needsInput()) deflate();
    crc.update(b.buf, b.start, b.pos - b.start);
    if(b.state != STATE_OPEN) releaseBlock(b);
  }

  private void writeGzipHeader(BufferedOutput b) throws IOException {
    b.int64l(0x88b1f).int16l((short)0xff00);
  }

  private void writeGzipTrailer(BufferedOutput b) throws IOException {
    b.int32l((int)crc.getValue()).int32l(defl.getTotalIn());
  }
}
