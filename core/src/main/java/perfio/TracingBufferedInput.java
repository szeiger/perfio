package perfio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Checksum;

/// A [BufferedInput] that maintains a trace of the read data.
/// 
/// Note that unlike [java.util.zip.CheckedInputStream] this class buffers the input, and the trace
/// is usually trailing behind the current position. If you need an up-to-date trace before
/// closing the stream, you have to call [#updateTrace()] first.
public abstract class TracingBufferedInput extends BufferedInput {

  /// Create a [TracingBufferedInput] that updates a [Checksum], similar to
  /// [java.util.zip.CheckedInputStream].
  /// 
  /// @param checksum The Checksum to update
  public static TracingBufferedInput checked(BufferedInput parent, Checksum checksum) {
    return new TracingBufferedInput(parent) {
      @Override
      protected void trace(byte[] buf, int off, int len) { checksum.update(buf, off, len); }
      protected void trace(ByteBuffer bb) { checksum.update(bb); }
    };
  }
  
  private final BufferedInput parent;
  private long traced = 0;
  private final TracingBufferedInput root;

  protected TracingBufferedInput(BufferedInput parent) {
    super(parent.buf, parent.pos, parent.lim, Long.MAX_VALUE, null, parent.bigEndian, parent.linebuf, parent.ms, parent.bb, parent);
    this.parent = parent;
    this.root = this;
  }

  private TracingBufferedInput(TracingBufferedInput viewParent) {
    super(null, 0, 0, 0, viewParent, viewParent.bigEndian, viewParent.linebuf, viewParent.ms, viewParent.bb, null);
    this.parent = viewParent.parent;
    this.root = viewParent.root;
  }

  /// This method is called to update the trace to include the specified byte array segment
  /// when reading from a `byte[]`-based BufferedInput. The default implementation wraps the
  /// byte array in a [ByteBuffer] and calls [#trace(ByteBuffer)]. It can be overridden to handle
  /// this case more efficiently.
  protected void trace(byte[] buf, int off, int len) {
    trace(ByteBuffer.wrap(buf, off, len));
  }

  /// This method is called to uodate the trace to include the specified ByteBuffer's contents.
  protected abstract void trace(ByteBuffer bb);

  @Override
  BufferedInput createEmptyView() {
    return new TracingBufferedInput(this) {
      protected void trace(ByteBuffer bb) {}
    };
  }

  @Override
  void copyBufferFrom(BufferedInput b) {
    super.copyBufferFrom(b);
    var bb = (TracingBufferedInput)b;
    traced = bb.traced;
  }

  @Override
  void bufferClosed(boolean closeUpstream) throws IOException {
    update();
    parent.pos = pos;
  }

  private void update() {
    //System.out.println("update() "+show());
    var tbuf = totalBuffered + parentTotalOffset;
    if(tbuf > traced) {
      var bstart = lim - (int)(tbuf - traced);
      var bend = lim;
      traced = tbuf;
      if(buf != null) root.trace(buf, bstart, bend-bstart);
      else root.trace(bb.slice(bstart, bend-bstart));
    }
  }

  @Override
  protected void prepareAndFillBuffer(int count) throws IOException {
    update();
    //System.out.println("prepareAndFillBuffer("+count+") "+show());
    if(totalBuffered < totalReadLimit) {
      var rem0 = lim-pos;
      parent.pos = pos;
      parent.prepareAndFillBuffer(count);
      pos = parent.pos;
      lim = parent.lim;
      buf = parent.buf;
      bb = parent.bb;
      var rem1 = lim-pos;
      totalBuffered += (rem1-rem0);
      clampToLimit();
    }
  }

  /// Ensure that the trace reflects all data that has been read from this
  /// TracingBufferedInput. This method is safe to call after closing, in which case
  /// it does nothing.
  public void updateTrace() throws IOException {
    if(!isClosed()) checkState();
    update();
  }
}
