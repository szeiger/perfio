package perfio;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class ParallelGzipBufferedOutput extends AsyncFilteringBufferedOutput {
  private final CRC32 crc = new CRC32();

  public ParallelGzipBufferedOutput(BufferedOutput parent, int depth, int minPartitionSize, int maxPartitionSize) throws IOException {
    super(parent, false, depth, true, minPartitionSize, maxPartitionSize, true, null);
    GzipUtil.writeHeader(parent);
  }

  public ParallelGzipBufferedOutput(BufferedOutput parent, int depth, int partitionSize) throws IOException {
    this(parent, depth, partitionSize, partitionSize);
  }

  public ParallelGzipBufferedOutput(BufferedOutput parent) throws IOException { this(parent, -1, 65536); }

  @Override protected void finish() throws IOException {
    parent.int16l((short)3);
    GzipUtil.writeTrailer(parent, crc.getValue(), (int)totalBytesWritten());
  }

  @Override protected void filterBlock(BufferedOutput b) throws IOException {
    // Ideally we'd compute CRCs of each block in parallel and then merge them, but it's very
    // fast compared to the deflate compression and Java doesn't expose the existing
    // `crc32_combine()` from zlib for our use.
    crc.update(b.buf, b.start, b.pos - b.start);
    super.filterBlock(b);
  }

  protected void filterAsync(Task t) {
    var defl = (Deflater)(t.data != null ? t.data : (t.data = new Deflater(Deflater.DEFAULT_COMPRESSION, true)));
    try {
      var o = t.to;
      if(!t.isEmpty()) {
        if(t.isNew()) defl.setInput(t.buf, t.start, t.length());
        o.pos += defl.deflate(o.buf, o.pos, o.buf.length-o.pos);
      }
      if(defl.needsInput()) {
        if(t.isLast()) {
          int l;
          while((l = defl.deflate(o.buf, o.pos, o.buf.length-o.pos, Deflater.SYNC_FLUSH)) > 0) o.pos += l;
          if(o.pos < o.buf.length) {
            defl.end();
            t.consume();
          }
        } else t.consume();
      }
    } catch(Error | RuntimeException ex) {
      try { defl.end(); } finally { throw ex; }
    }
  }
}
