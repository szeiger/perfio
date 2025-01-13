package perfio;

import java.io.IOException;

/// A BufferedOutput that processes blocks with a filter and writes them to a parent.
/// Subclasses must implement at least [#checkClosed(BufferedOutput, boolean)], and possibly
/// [#finish()]. Asynchronous filters also need to override [#processAndClose(BufferedOutput)].
public abstract class FilteringBufferedOutput extends TopLevelBufferedOutput {
  private final BufferedOutput parent;

  protected FilteringBufferedOutput(BufferedOutput parent) {
    super(new byte[parent.topLevel.initialBufferSize], parent.bigEndian, 0, 0, parent.topLevel.initialBufferSize,
        parent.topLevel.initialBufferSize, false, Long.MAX_VALUE);
    this.parent = parent;
  }

  boolean preferSplit() { return true; }

  @Override void closeUpstream() throws IOException {
    super.closeUpstream();
    finish();
    parent.close();
  }

  // TODO allow reuse of FilteringBufferedOutput objects
  // TODO get root block from cache

  /// Close this FilteringBufferedOutput without closing the parent.
  public void end() throws IOException {
    super.closeUpstream();
    finish();
  }

  /// Switch the given block `b` from [#STATE_PROCESSING] to [#STATE_CLOSED]. If the filtered data
  /// does not fit into `b`, it may be split off into a new, closed block inserted *before* `b`
  /// (which may be this top-level block).
  ///
  /// If `blocking == true`, the method must not return without producing a closed block that makes
  /// progress (i.e. the block must not be empty unless `b` leads to an empty output and can thus
  /// be discarded), blocking the current thread if necessary. Otherwise, it should return without
  /// producing a closed block.
  ///
  /// The method may optionally write the output data directly to the [#parent], in which case it
  /// must adjust the closed block `b` accordingly:
  /// ```
  /// b.totalFlushed += (b.pos - b.start);
  /// b.pos = b.lim;
  /// b.start = b.lim;
  /// ```
  ///
  /// The method is always called on the main thread when a block may be flushed. Blocks are
  /// flushed in the correct output order. No new block will be checked until the previous one has
  /// been closed.
  ///
  /// If [#processAndClose(BufferedOutput)] has set a non-null [#filterState], this method must
  /// reset it to null when it marks a block as closed.
  abstract void finalizeBlock(BufferedOutput b, boolean blocking) throws IOException;

  /// Switch the given block to [#STATE_PROCESSING]. This method can be overridden in subclasses
  /// to initiate asynchronous processing of a block. The necessary state to keep track of the
  /// asynchronous processing can be stored in the block's [#filterState].
  ///
  /// The method is always called on the main thread. Blocks may be closed out of order.
  @Override void processAndClose(BufferedOutput b) { b.state = STATE_PROCESSING; }

  /// Close the filter and clean up resources without closing the parent.
  void finish() throws IOException {}

  void flushBlocks(boolean forceFlush) throws IOException {
    while(true)
      if(!tryFlush(forceFlush) || (next == this)) return;
  }

  private boolean tryFlush(boolean forceFlush) throws IOException {
    var b = next;
    if(b.state == STATE_PROCESSING) {
      finalizeBlock(b, forceFlush);
      b = next;
    }
    if(b.state != STATE_CLOSED) return false;
    int blen = b.pos - b.start;
    //TODO insert block into parent instead of copying (requires shared cache)
    if(blen > 0) {
      parent.write(b.buf, b.start, blen);
      b.totalFlushed += blen;
      b.pos = b.lim;
      b.start = b.lim;
    }
    if(b != b.topLevel) b.unlinkAndReturn();
    return true;
  }
}
