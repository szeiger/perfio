package perfio;

import java.io.IOException;

public abstract class FilteringBufferedInput extends SwitchingHeapBufferedInput {
  private final BufferedInput parent, parentView;
  protected final FilteringBufferedInput rootFilter;
  private final int blockSize;
  private OutputBlock cache;

  /// Constructor to be called by subclasses.
  ///
  /// @param parent The parent buffer to read from.
  /// @param blockSize The block size of this filter, if it is the root filter. When multiple
  ///   filters are stacked, the lowest one contains the shared cache and determines the block
  ///   size. Consequently, this argument is ignored if the parent is already a
  ///   [FilteringBufferedInput] or a view of one.
  FilteringBufferedInput(BufferedInput parent, int blockSize) throws IOException {
    //TODO auto buffer size from parent
    super(new It(), parent.bigEndian);
    this.parent = parent;
    this.parentView = parent.identicalView((It)it);
    if(parent.viewRoot instanceof FilteringBufferedInput i) {
      this.rootFilter = i.rootFilter;
      this.blockSize = i.rootFilter.blockSize;
    } else {
      this.rootFilter = this;
      this.blockSize = blockSize;
    }
    ((It)it).outer = this;
  }
  
  @Override
  void bufferClosed(boolean closeUpstream) throws IOException {
    parentView.close();
    if(closeUpstream) parent.close();
  }

  private static class It extends BufferIterator implements CloseableView  {
    private FilteringBufferedInput outer;
    private OutputBlock to;

    public Object next() throws IOException {
      to = outer.rootFilter.allocBlock();
      outer.filterBlock(outer.parentView, to);
      return to.start == to.pos ? null : to;
    }

    public byte[] buffer() { return to.buf; }
    public int start() { return to.start; }
    public int end() { return to.pos; }

    public void returnBuffer(Object id) { outer.rootFilter.releaseBlock((OutputBlock)id); }

    @Override public void markClosed() { outer.markClosed(); }
  }

  // Must only be called on the rootFilter
  private OutputBlock allocBlock() {
    OutputBlock b;
    if(cache == null) {
      b = new OutputBlock();
      b.buf = new byte[blockSize];
    } else {
      b = cache;
      cache = b.next;
      b.next = null;
      b.pos = 0;
    }
    b.lim = b.buf.length;
    b.bigEndian = bigEndian;
    return b;
  }

  // Must only be called on the rootFilter
  private void releaseBlock(OutputBlock b) {
    b.next = cache;
    cache = b;
  }

  private static final class OutputBlock extends WritableBuffer<OutputBlock> {
    private OutputBlock next;
    protected void flushAndGrow(int count) {}
  }

  /// Read data from `from` and produce at least 1 byte of filtered output in `to`.
  /// Producing an empty block indicates the end of the input.
  protected abstract void filterBlock(BufferedInput from, WritableBuffer<?> to) throws IOException;
}
