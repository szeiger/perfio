package perfio;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class ParallelGzipBufferedOutput extends AsyncFilteringBufferedOutput {
  private final CRC32 crc = new CRC32();
  private int totalIn;

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
    GzipUtil.writeTrailer(parent, crc.getValue(), totalIn);
  }

  @Override protected void filterBlock(BufferedOutput b) throws IOException {
    crc.update(b.buf, b.start, b.pos - b.start);
    totalIn += (b.pos - b.start);
    super.filterBlock(b);
  }

  protected void filterAsync(Task t) {
    var defl = (Deflater)(t.data != null ? t.data : (t.data = new Deflater(Deflater.DEFAULT_COMPRESSION, true)));
    var o = t.to;
    if(t.start != t.end) {
      if(t.state != Task.STATE_OVERFLOWED) defl.setInput(t.buf, t.start, t.end - t.start);
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
  }
}
