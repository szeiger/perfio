package perfio;

import java.io.IOException;

/// A BufferedOutput that processes blocks with a filter and writes them to a parent.
/// Subclasses must implement at least [#finalizeBlock(BufferedOutput, boolean)], and possibly
/// [#finish()]. Asynchronous filters also need to override [#processAndClose(BufferedOutput)].
public abstract class FilteringBufferedOutput extends TopLevelBufferedOutput {
  protected final BufferedOutput parent;
  private final boolean flushPartial;

  /// An exclusive block from the cache whose buffer is used by this block. We swap it back into
  /// that block to either return it to the cache or insert it into the parent at the end (both of
  /// which require a standard NestedBufferedOutput block).
  private NestedBufferedOutput cachedBlock;

  // TODO allow reuse of FilteringBufferedOutput objects

  protected FilteringBufferedOutput(BufferedOutput parent, boolean flushPartial) {
    super(null, parent.bigEndian, 0, 0, 0, parent.topLevel.initialBufferSize, false, Long.MAX_VALUE, parent.cache);
    cachedBlock = cache.getExclusiveBlock();
    buf = cachedBlock.buf;
    lim = buf.length;
    cachedBlock.buf = null;
    this.parent = parent;
    this.flushPartial = flushPartial;
  }

  boolean preferSplit() { return !flushPartial; }

  @Override void closeUpstream() throws IOException {
    super.closeUpstream();
    finish();
    parent.close();
  }

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
  /// be discarded), blocking the current thread if necessary. Otherwise, it may return without
  /// producing a closed block.
  ///
  /// Alternatively, the method may write the output data directly to the [#parent] and return
  /// `true`.
  ///
  /// If [#flushPartial] is `true`, this method may also be called on blocks in [#STATE_OPEN] (and
  /// it most not change this state).
  ///
  /// The method is always called on the main thread when a block may be flushed. Blocks are
  /// flushed in the correct output order. No new block will be checked until the previous one has
  /// been closed.
  ///
  /// If [#processAndClose(BufferedOutput)] has set a non-null [#filterState], this method must
  /// reset it to null when it marks a block as closed.
  ///
  /// @return true if all data was written to the parent and the buffer may be discarded,
  ///   otherwise false.
  abstract boolean finalizeBlock(BufferedOutput b, boolean blocking) throws IOException;

  /// Switch the given block to [#STATE_PROCESSING]. This method can be overridden in subclasses
  /// to initiate asynchronous processing of a block. The necessary state to keep track of the
  /// asynchronous processing can be stored in the block's [#filterState].
  ///
  /// The method is always called on the main thread. Blocks may be closed out of order.
  @Override void processAndClose(BufferedOutput b) { b.state = STATE_PROCESSING; }

  /// Close the filter and clean up resources without closing or flushing the parent.
  void finish() throws IOException {}

  void flushBlocks(boolean forceFlush) throws IOException {
    assert(state != STATE_CLOSED);
    while(tryFlush(forceFlush)) {}
  }

  private boolean tryFlush(boolean forceFlush) throws IOException {
    var b = next;
    if(b.state == STATE_PROCESSING || (flushPartial && b.state == STATE_OPEN)) {
      if(finalizeBlock(b, forceFlush)) {
        b.totalFlushed += (b.pos - b.start);
        b.pos = b.lim;
        b.start = b.lim;
      }
      b = next;
    }
    if(b.state != STATE_CLOSED) return false;
    if(b != this) b.unlinkOnly();
    assert b != this || forceFlush;
    if(b.pos > b.start) insertIntoParent(b);
    else if(b != this) returnBlock(b);
    return (b != this);
  }

  private void insertIntoParent(BufferedOutput b) throws IOException {
    int blen = b.pos - b.start;
    var emptyParent = parent.pos == parent.start && !parent.fixed;
    if(emptyParent) {
      //System.out.println("Insert into empty parent");
      if(b == this) {
        //System.out.println("Inserting "+b.showThis());
        copyStateToCached();
        b = cachedBlock;
        cachedBlock = null; //--
        buf = null; //--
        prev = null; //--
        next = null; //--
      }
      b.rootBlock = parent.rootBlock;
      b.topLevel = parent.topLevel;
      parent.rootBlock.totalFlushed += blen;
      b.insertBefore(parent);
      b.state = STATE_OPEN;
      parent.topLevel.processAndClose(b);
      if(b.prev == parent.rootBlock) parent.rootBlock.flushBlocks(false);
      //System.out.println(parent.rootBlock.showList());

      //System.out.println(parent.rootBlock.showList());
    } else if(parent.available() >= b.pos - b.start) {
      //System.out.println("Copy to parent");
      var p = parent.fwd(blen);
      System.arraycopy(b.buf, b.start, parent.buf, p, blen);
      b.totalFlushed += blen;
      b.pos = b.lim;
      b.start = b.lim;
      returnBlock(b);
    } else {
      //System.out.println("Insert & swap");
      //TODO
      parent.write(b.buf, b.start, blen);
      b.totalFlushed += blen;
      b.pos = b.lim;
      b.start = b.lim;
      returnBlock(b);
    }
  }

  private void copyStateToCached() {
    var to = cachedBlock;
    to.buf = this.buf;
    to.start = this.start;
    to.pos = this.pos;
    to.lim = this.lim;
    assert(this.sharing != SHARING_LEFT);
    to.sharing = SHARING_EXCLUSIVE;
    to.totalFlushed = 0;
    assert(this.state == STATE_CLOSED);
  }

  private void returnBlock(BufferedOutput b) {
    if(b == this) {
      b = cachedBlock;
      b.buf = buf;
    }
    cache.returnToCache(b);
  }
}
