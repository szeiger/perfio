package perfio;

import java.io.IOException;
import java.util.zip.Checksum;

public class CheckedBufferedInput extends BufferedInput {
  private final BufferedInput parent;
  private final Checksum checksum;
  private long checked = 0;

  public CheckedBufferedInput(BufferedInput parent, Checksum checksum) {
    super(parent.buf, parent.pos, parent.lim, Long.MAX_VALUE, null, parent.bigEndian, parent.linebuf, parent.ms, parent.bb, parent);
    this.parent = parent;
    this.checksum = checksum;
  }

  private CheckedBufferedInput(CheckedBufferedInput viewParent) {
    super(null, 0, 0, 0, viewParent, viewParent.bigEndian, viewParent.linebuf, viewParent.ms, viewParent.bb, null);
    this.parent = viewParent.parent;
    this.checksum = viewParent.checksum;
  }

  @Override
  BufferedInput createEmptyView() { return new CheckedBufferedInput(this); }

  @Override
  void copyBufferFrom(BufferedInput b) {
    super.copyBufferFrom(b);
    var bb = (CheckedBufferedInput)b;
    checked = bb.checked;
  }

  @Override
  void bufferClosed(boolean closeUpstream) throws IOException {
    update();
  }

  private void update() {
    //System.out.println("update() "+show());
    var tbuf = totalBuffered + parentTotalOffset;
    if(tbuf > checked) {
      var bstart = lim - (int)(tbuf - checked);
      var bend = lim;
      checked = tbuf;
      if(buf != null) checksum.update(buf, bstart, bend-bstart);
      else checksum.update(bb.slice(bstart, bend-bstart));
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
      var rem1 = lim-pos;
      totalBuffered += (rem1-rem0);
      clampToLimit();
    }
  }

  /// Ensure that the checksum reflects all data that has been read from this
  /// CheckedHeapBufferedInput. This method is safe to call after closing, in which case
  /// it does nothing.
  ///
  /// @return the checksum
  public Checksum updateChecksum() throws IOException {
    if(!isClosed()) checkState();
    update();
    return checksum;
  }
}
