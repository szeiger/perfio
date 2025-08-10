package perfio;

import java.io.IOException;
import java.util.zip.Checksum;

/// A [BufferedOutput] that maintains a trace of the data being written.
/// 
/// Note that unlike [java.util.zip.CheckedOutputStream] this class buffers the output, and the
/// trace is usually trailing behind the current position. If you need an up-to-date trace before
/// closing the stream, you have to call [#updateTrace()] first.
public abstract class TracingBufferedOutput extends FilteringBufferedOutput {

  /// Create a [TracingBufferedOutput] that updates a [Checksum], similar to
  /// [java.util.zip.CheckedOutputStream].
  ///
  /// @param checksum The Checksum to update
  /// @param flushPartial When set to `true`, the first open block can be partially flushed.
  public static TracingBufferedOutput checked(BufferedOutput parent, Checksum checksum, boolean flushPartial) {
    return new TracingBufferedOutput(parent, flushPartial) {
      protected void trace(byte[] buf, int off, int len) { checksum.update(buf, off, len); }
    };
  }

  /// Create a [TracingBufferedOutput] that updates a [Checksum], similar to
  /// [java.util.zip.CheckedOutputStream]. Same as `checked(parent, checksum, true)`.
  ///
  /// @param checksum The Checksum to update
  public static TracingBufferedOutput checked(BufferedOutput parent, Checksum checksum) {
    return checked(parent, checksum, true);
  }

  private long flushedUpstream = 0;

  protected TracingBufferedOutput(BufferedOutput parent, boolean flushPartial) { super(parent, flushPartial); }

  private void update(BufferedOutput b) {
    var pending = totalBytesWritten()-flushedUpstream;
    if(pending > 0) {
      var l = (int)Math.min(pending, b.pos-b.start);
      trace(b.buf, b.pos-l, l);
      flushedUpstream += l;
    }
  }

  /// This method is called to update the trace to include the specified byte array segment.
  protected abstract void trace(byte[] buf, int off, int len);

  protected void filterBlock(BufferedOutput b) throws IOException {
    update(b);
    if(b.state == STATE_CLOSED) appendBlockToParent(b);
    //System.out.println("filterBlock: "+(totalBytesWritten()-flushedUpstream));
  }

  /// Ensure that the trace reflects all data that has been written to this TracingBufferedOutput,
  /// regardless of whether it has been flushed. Unlike flushing, which is only done on a
  /// best-effort basis, this method fails if there are multiple open blocks (created with
  /// [#reserve(long)]). This method is safe to call after closing, in which case it does nothing.
  ///
  /// @throws IOException if there are multiple open blocks
  public void updateTrace() throws IOException {
    if(state == STATE_CLOSED) return;
    if(prev != this && prev.state == STATE_OPEN)
      throw new IOException("Buffer has multiple open blocks");
    flushBlocks(false);
    if(!flushPartial) update(this);
    //System.out.println("updateTrace: "+(totalBytesWritten()-flushedUpstream));
  }
}
