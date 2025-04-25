package perfio;

import java.io.EOFException;
import java.io.IOException;

/// Base class for filtering [BufferedOutput] implementations that process the output data and
/// append it to a parent buffer.
/// 
/// Synchronous filters need to implement at least [#filterBlock(BufferedOutput)] and potentially
/// override [#finish()]. Asynchronous filters must also override [#flushPending()] (but most
/// asynchronous filters will want to extend [AsyncFilteringBufferedOutput] instead).
public abstract class FilteringBufferedOutput extends TopLevelBufferedOutput {
  protected final BufferedOutput parent;
  private final boolean flushPartial;

  /// An exclusive block from the cache whose buffer is used by this block. We swap it back into
  /// that block to either return it to the cache or insert it into the parent at the end (both of
  /// which require a standard NestedBufferedOutput block).
  private NestedBufferedOutput cachedBlock;

  protected FilteringBufferedOutput(BufferedOutput parent, boolean flushPartial) {
    super(null, parent.bigEndian, 0, 0, 0, parent.topLevel.initialBufferSize, false, Long.MAX_VALUE, parent.cache);
    cachedBlock = cache.getExclusiveBlock();
    buf = cachedBlock.buf;
    lim = buf.length;
    cachedBlock.buf = null;
    this.parent = parent;
    this.flushPartial = flushPartial;
  }

  @Override boolean preferSplit() { return !flushPartial; }

  @Override void closeUpstream() throws IOException {
    super.closeUpstream();
    finish();
    parent.close();
  }

  ///// Close this FilteringBufferedOutput without closing the parent.
  //public void end() throws IOException {
  //  if(state == STATE_OPEN) {
  //    super.closeUpstream();
  //    finish();
  //  }
  //}

  /// Close the filter and clean up resources without closing or flushing the parent.
  void finish() throws IOException {}

  void flushBlocks(boolean forceFlush) throws IOException {
    while(true) {
      var b = next;
      if(b.state == STATE_CLOSED) {
        var blen = b.pos - b.start;
        b.unlinkOnly();
        if(blen != 0) filterBlock(b);
        else releaseBlock(b);
        if(b == this) break;
      } else if(flushPartial) {
        var blen = b.pos - b.start;
        if(blen != 0) {
          filterBlock(b);
          b.totalFlushed += (b.pos - b.start);
          b.pos = b.lim;
          b.start = b.lim;
        }
        break;
      } else break;
    }
    if(forceFlush) flushPending();
  }

  /// Process the given non-empty closed block `b`. This method is called in output order for all
  /// closed blocks after unlinking them.
  ///
  /// A synchronous implementation of this method will do one of the following:
  /// - Write the filtered data directly to the [#parent] and release the block with
  ///   [#releaseBlock(BufferedOutput)].
  /// - Potentially request new blocks to hold output data with [#allocBlock()], append them to
  ///   the parent by calling [#appendBlockToParent(BufferedOutput)], and release any blocks that
  ///   were not appended to the parent with [#releaseBlock(BufferedOutput)].
  ///
  /// An asynchronous filter may instead start filtering the block on another thread, and collect
  /// any finished blocks to write them to the parent. It must also override [#flushPending()] to
  /// write pending blocks on demand. Note that all [#BufferedOutput] methods may only be called
  /// on the main thread. In particular, you must not allocate a new block from a background
  /// thread. Instead, wait for the next call to [#filterBlock(BufferedOutput)] or
  /// [#flushPending()] to get a new block and then continue processing.
  ///
  /// Any block passed to [#filterBlock(BufferedOutput)] or allocated with [#allocBlock()] should
  /// either be passed to [#appendBlockToParent(BufferedOutput)] or released with
  /// [#releaseBlock(BufferedOutput)] at some point.
  ///
  /// If the filter is created with `flushPartial = true`, this method may also be called on
  /// an open block. Such a block must *not* be appended to the parent or released.
  ///
  /// Since the block was already unlinked before this method is called, an asynchronous
  /// implementation may use block links to keep a list of all pending blocks. It may also use
  /// [#filterState] to store additional information (for example,
  /// a [#java.util.concurrent.CompletableFuture]).
  protected abstract void filterBlock(BufferedOutput b) throws IOException;

  /// This method is called on [#flush()] or [#close()]. The default implementation does nothing
  /// and is suitable for a synchronous filter implementation. An asynchronous filter must override
  /// it to wait until all blocks previously passed to [#filterBlock(BufferedOutput)] have been
  /// processed, and write them to the parent.
  protected void flushPending() throws IOException {}

  /// Release a block back to the cache.
  protected final void releaseBlock(BufferedOutput b) {
    b.filterState = null;
    if(b == this) {
      b = cachedBlock;
      b.buf = buf;
      cachedBlock = null;
    }
    cache.returnToCache(b);
  }

  /// Get a block from the cache.
  protected final BufferedOutput allocBlock() { return cache.getExclusiveBlock(); }

  /// Append a block to the parent.
  protected final void appendBlockToParent(BufferedOutput b) throws IOException {
    int blen = b.pos - b.start;
    if(parent.rootBlock.totalBytesWritten() + blen > parent.totalLimit) throw new EOFException();
    var emptyParent = parent.pos == parent.start && !parent.fixed;
    if(emptyParent) {
      //System.out.println("Insert into empty parent");
      b.filterState = null;
      if(b == this) b = copyStateToCached();
      b.rootBlock = parent.rootBlock;
      b.topLevel = parent.topLevel;
      parent.rootBlock.totalFlushed += blen;
      b.insertBefore(parent);
      b.state = STATE_CLOSED;
      if(b.prev == parent.rootBlock) parent.rootBlock.flushBlocks(false);
      //System.out.println(parent.rootBlock.showList());
    } else if(parent.available() >= b.pos - b.start || parent.fixed) {
      //System.out.println("Copy to parent");
      var p = parent.fwd(blen);
      System.arraycopy(b.buf, b.start, parent.buf, p, blen);
      releaseBlock(b);
    } else appendBlockToParentSwap(b);
  }

  private NestedBufferedOutput copyStateToCached() {
    var to = cachedBlock;
    to.buf = this.buf;
    to.start = this.start;
    to.pos = this.pos;
    to.lim = this.lim;
    assert(this.sharing != SHARING_LEFT);
    to.sharing = SHARING_EXCLUSIVE;
    to.totalFlushed = 0;
    assert(this.state == STATE_CLOSED);
    clearLocal();
    return to;
  }
  
  private void clearLocal() {
    cachedBlock = null;
    buf = null;
    prev = null;
    next = null;
  }

  private void appendBlockToParentSwap(BufferedOutput b) throws IOException {
    //System.out.println("Insert & swap");
    BufferedOutput bt;
    if(b == this) {
      bt = cachedBlock;
      clearLocal();
    } else bt = b;
    var tmpbuf = parent.buf;
    var tmpstart = parent.start;
    var tmppos = parent.pos;
    var tmpsharing = parent.sharing;
    var len = tmppos - tmpstart;
    parent.rootBlock.totalFlushed += len;
    bt.totalFlushed = 0;
    parent.buf = b.buf;
    parent.start = b.start;
    parent.pos = b.pos;
    parent.lim = parent.buf.length;
    parent.sharing = b.sharing;
    bt.buf = tmpbuf;
    bt.start = tmpstart;
    bt.pos = tmppos;
    bt.sharing = tmpsharing;
    bt.rootBlock = parent.rootBlock;
    bt.topLevel = parent.topLevel;
    bt.insertBefore(parent);
    bt.state = STATE_CLOSED;
    if(bt.prev == parent.rootBlock) parent.rootBlock.flushBlocks(false);
  }
}
