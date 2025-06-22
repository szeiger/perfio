package perfio;

import java.io.IOException;
import java.util.zip.Checksum;

/// A [BufferedOutput] that maintains a checksum of the data being written.
/// 
/// Note that unlike [java.util.zip.CheckedOutputStream] this class buffers the output. If you
/// need to read an up-to-date checksum before closing, you have to call [#updateChecksum()].
public class CheckedBufferedOutput extends FilteringBufferedOutput {
  private final Checksum checksum;
  private long flushedUpstream = 0;

  public CheckedBufferedOutput(BufferedOutput parent, Checksum checksum, boolean flushPartial) {
    super(parent, flushPartial);
    this.checksum = checksum;
  }

  public CheckedBufferedOutput(BufferedOutput parent, Checksum checksum) {
    this(parent, checksum, true);
  }

  private void update(BufferedOutput b) {
    var pending = totalBytesWritten()-flushedUpstream;
    if(pending > 0) {
      var l = (int)Math.min(pending, b.pos-b.start);
      checksum.update(b.buf, b.pos-l, l);
      flushedUpstream += l;
    }
  }

  protected void filterBlock(BufferedOutput b) throws IOException {
    update(b);
    if(b.state == STATE_CLOSED) appendBlockToParent(b);
    //System.out.println("filterBlock: "+(totalBytesWritten()-flushedUpstream));
  }

  /// Ensure that the checksum reflects all data that has been written to this
  /// CheckedBufferedOutput, regardless of whether it has been flushed. Unlike flushing, which
  /// is only done on a best-effort basis, this method fails if there are multiple open blocks
  /// (created with [#reserve(long)]). This method is safe to call after closing, in which case
  /// it does nothing.
  ///
  /// @return the checksum
  /// @throws IOException if there are multiple open blocks
  public Checksum updateChecksum() throws IOException {
    if(state == STATE_CLOSED) return checksum;
    if(prev != this && prev.state == STATE_OPEN)
      throw new IOException("Buffer has multiple open blocks");
    flushBlocks(false);
    if(!flushPartial) update(this);
    //System.out.println("updateChecksum: "+(totalBytesWritten()-flushedUpstream));
    return checksum;
  }
}
