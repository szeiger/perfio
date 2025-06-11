package perfio;

import perfio.internal.MemoryAccessor;
import perfio.internal.StringInternals;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import static perfio.internal.BufferUtil.growBuffer;


/// BufferedOutput provides buffered streaming writes to an OutputStream or similar data sink.
///
/// The API is not thread-safe. Access from multiple threads must be synchronized externally.
/// The number of bytes written is tracked as a 64-bit signed long value [#totalBytesWritten()].
/// The behaviour after writing more than [Long#MAX_VALUE] (8 exabytes) is undefined.
///
/// A freshly created BufferedOutput always uses [ByteOrder#BIG_ENDIAN]. This can be changed with
/// [#order(ByteOrder)]. Unless specified explicitly, the initial buffer size is 32768.
public abstract class BufferedOutput extends WritableBuffer<BufferedOutput> implements Closeable, Flushable {

  /// Write data to an [OutputStream].
  ///
  /// @param out               Stream to write to.
  /// @param initialBufferSize Initial buffer size. The buffer is expanded later if necessary.
  public static BufferedOutput of(OutputStream out, int initialBufferSize) {
    var buf = new byte[Math.max(initialBufferSize, MinBufferSize)];
    return new FlushingBufferedOutput(buf, true, 0, 0, buf.length, initialBufferSize, false, Long.MAX_VALUE, out);
  }

  /// Write data to an [OutputStream] using the default initial buffer size.
  ///
  /// @param out               Stream to write to.
  /// @see #of(OutputStream, int)
  public static BufferedOutput of(OutputStream out) { return of(out, DefaultBufferSize); }

  /// Write data to an internal byte array buffer that can be accessed directly or copied after
  /// closing the BufferedOutput (similar to [ByteArrayOutputStream]). The size is limited to 2 GB
  /// (the maximum size of a byte array).
  ///
  /// @param initialRootBufferSize Initial size of the root buffer. The buffer is expanded later if
  ///   necessary. This size is automatically expanded to `initialBufferSize` if the specified size
  ///   is lower.
  /// @param initialBufferSize Initial buffer size for additional buffers that may need to be
  ///   created. This size is automatically expanded to [#MinBufferSize] if the specified size is
  ///   lower.
  public static ArrayBufferedOutput growing(int initialRootBufferSize, int initialBufferSize) {
    var ibs = Math.max(initialBufferSize, MinBufferSize);
    var buf = new byte[Math.max(initialRootBufferSize, ibs)];
    return new ArrayBufferedOutput(buf, true, 0, 0, buf.length, ibs, false);
  }

  /// Write data to an internal byte array buffer that can be accessed directly or copied after
  /// closing the BufferedOutput (similar to [ByteArrayOutputStream]) using the default initial
  /// buffer size. The size is limited to 2 GB (the maximum size of a byte array).
  ///
  /// @param initialRootBufferSize Initial size of the root buffer. The buffer is expanded later if
  ///   necessary. This size is automatically expanded to `initialBufferSize` if the specified size
  ///   is lower.
  /// @see #growing(int, int)
  public static ArrayBufferedOutput growing(int initialRootBufferSize) {
    return growing(initialRootBufferSize, DefaultBufferSize);
  }

  /// Write data to an internal byte array buffer that can be accessed directly or copied after
  /// closing the BufferedOutput (similar to [ByteArrayOutputStream]) using the default initial
  /// buffer size and default initial root buffer size. The size is limited to 2 GB (the maximum
  /// size of a byte array).
  /// @see #growing(int, int)
  public static ArrayBufferedOutput growing() { return growing(DefaultBufferSize); }

  /// Write data to an internal buffer that can be read as an [InputStream] or [BufferedInput]
  /// after closing the BufferedOutput.
  ///
  /// @param initialBufferSize Initial buffer size. The buffer is expanded later if necessary.
  public static AccumulatingBufferedOutput ofBlocks(int initialBufferSize) {
    return new BlockBufferedOutput(true, initialBufferSize);
  }

  /// Write data to an internal buffer that can be read as an [InputStream] or [BufferedInput]
  /// after closing the BufferedOutput using the default initial buffer size.
  /// @see #ofBlocks(int)
  public static AccumulatingBufferedOutput ofBlocks() { return ofBlocks(DefaultBufferSize); }

  /// Write data to a buffered pipe that can be read concurrently from another thread as an
  /// [InputStream] or [BufferedInput]. Blocks of data are buffered and then transferred to the
  /// reader. Note that this BufferedOutput and the opposing BufferedInput or InputStream are not
  /// thread-safe, just like any other BufferedOutput/BufferedInput. Only their communication is.
  /// This allows you to use one thread for writing and one thread for reading. Doing both from
  /// the same thread is not recommended as it may deadlock when the internal buffer runs full or
  /// empty.
  ///
  /// @param initialBufferSize Initial buffer size. The buffer is expanded later if necessary.
  public static PipeBufferedOutput pipe(int initialBufferSize) {
    return new PipeBufferedOutput(true, initialBufferSize, 2);
  }

  /// Write data to a buffered pipe that can be read concurrently from another thread as an
  /// [InputStream] or [BufferedInput] using the default initial buffer size.
  /// @see #pipe(int)
  public static PipeBufferedOutput pipe() { return pipe(DefaultBufferSize); }


  /// Write data to a given region of an existing byte array. The BufferedOutput is limited to the
  /// initial size.
  ///
  /// @param initialBufferSize Initial buffer size for additional temporary buffers that may be
  ///                          created when using [#defer(long)], otherwise unused.
  public static ArrayBufferedOutput ofArray(byte[] buf, int off, int len, int initialBufferSize) {
    Objects.checkFromIndexSize(off, len, buf.length);
    return new ArrayBufferedOutput(buf, true, off, off, len+off, initialBufferSize, true);
  }

  /// Write data to a given byte array. Same as `ofArray(buf, 0, buf.length)`.
  /// @see #ofArray(byte[], int, int, int)
  public static ArrayBufferedOutput ofArray(byte[] buf) { return ofArray(buf, 0, buf.length, DefaultBufferSize); }

  /// Write data to a given region of an existing byte array.
  /// @see #ofArray(byte[], int, int, int)
  public static ArrayBufferedOutput ofArray(byte[] buf, int start, int len) { return ofArray(buf, start, len, DefaultBufferSize); }

  /// Write data to a file.
  ///
  /// @param path              File to write.
  /// @param initialBufferSize Initial buffer size. The buffer is expanded later if necessary.
  /// @param option            Optional set of OpenOptions. When empty, the default set (`CREATE`,
  ///                          `TRUNCATE_EXISTING`, `WRITE`) is used.
  public static BufferedOutput ofFile(Path path, int initialBufferSize, OpenOption... option) throws IOException {
    var out = Files.newOutputStream(path, option);
    return of(out, initialBufferSize);
  }

  /// Write data to a file.
  ///
  /// @param path              File to write.
  /// @param option            Optional set of OpenOptions. When empty, the default set (`CREATE`,
  ///                          `TRUNCATE_EXISTING`, `WRITE`) is used.
  /// @see #ofFile(Path, int, OpenOption...)
  public static BufferedOutput ofFile(Path path, OpenOption... option) throws IOException {
    return ofFile(path, DefaultBufferSize);
  }

  public static final int MinBufferSize = 16;
  public static final int DefaultBufferSize = 32768;

  static final byte SHARING_EXCLUSIVE = (byte)0; // buffer is not shared
  static final byte SHARING_LEFT      = (byte)1; // buffer is shared with next block
  static final byte SHARING_RIGHT     = (byte)2; // buffer is shared with previous blocks only
  
  static final byte STATE_OPEN        = (byte)0;
  static final byte STATE_CLOSED      = (byte)1;


  // ======================================================= non-static parts:

  final boolean fixed;
  long totalLimit;

  BufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, boolean fixed, long totalLimit, TopLevelBufferedOutput topLevel, TopLevelBufferedOutput cache) {
    this.buf = buf;
    this.bigEndian = bigEndian;
    this.start = start;
    this.pos = pos;
    this.lim = lim;
    this.fixed = fixed;
    this.totalLimit = totalLimit;
    this.topLevel = topLevel == null ? (TopLevelBufferedOutput) this : topLevel;
    this.cache = cache == null ? (TopLevelBufferedOutput) this : cache;
  }

  long totalFlushed = 0L; // Bytes already flushed upstream or held in prefix blocks
  BufferedOutput next = this; // prefix list as a double-linked ring
  BufferedOutput prev = this;
  boolean truncate = true;

  /// The root block of the prefix ring. This is either the same as [#topLevel]
  /// or a [NestedBufferedOutput] created with [#defer()].
  BufferedOutput rootBlock = this;

  /// The top-level BufferedOutput which is responsible for flushing the written data.
  TopLevelBufferedOutput topLevel;

  /// The top-level BufferedOutput that manages the cache.
  final TopLevelBufferedOutput cache;

  Object filterState;

  byte sharing = SHARING_EXCLUSIVE;
  byte state = STATE_OPEN;

  /// Change the byte order of this BufferedOutput.
  public BufferedOutput order(ByteOrder order) {
    bigEndian = order == ByteOrder.BIG_ENDIAN;
    return this;
  }

  void checkState() throws IOException {
    if(state != STATE_OPEN) throw new IOException("BufferedOutput has already been closed");
  }

  /// Return the total number of bytes written to this BufferedOutput.
  public final long totalBytesWritten() { return totalFlushed + (pos - start); }

  protected void flushAndGrow(int count) throws IOException {
    //System.out.println("***** flushAndGrow("+count+") in "+showThis());
    checkState();
    if(fixed) return;
    if(topLevel.preferSplit() && lim-pos <= topLevel.initialBufferSize/2 && count <= topLevel.initialBufferSize) { //TODO try to flush first?
      // switch to a new buffer if this one is sufficiently filled
      newBuffer();
    } else if(prev == rootBlock) {
      rootBlock.flushBlocks(rootBlock == topLevel); //TODO should this really forceFlush?
      if(lim < pos + count) {
        if(pos == start) growBufferClear(count);
        else growBufferCopy(count);
      }
    } else growBufferCopy(count);
  }

  /// Switch to a new buffer, moving the current one into the prefix list
  private void newBuffer() throws IOException {
    var pre = this.prev;
    var b = cache.getExclusiveBlock();
    totalFlushed += (pos-start);
    buf = b.reinit(buf, bigEndian, start, pos, lim, sharing, 0L, 0L, true, rootBlock, null, topLevel);
    b.state = STATE_CLOSED;
    b.insertBefore(this);
    start = 0;
    pos = 0;
    lim = buf.length;
    sharing = SHARING_EXCLUSIVE;
    if(pre == rootBlock) rootBlock.flushBlocks(false);
  }

  private IOException underflow(long len, long exp) {
    return new IOException("Can't close BufferedOutput created by reserve("+exp+"): Only "+len+" bytes written");
  }

  private void checkUnderflow() throws IOException {
    if(fixed) {
      if(pos != lim) throw underflow(pos-start, lim-start);
    } else {
      if(totalBytesWritten() != totalLimit) throw underflow(totalLimit, totalBytesWritten());
    }
  }

  /// Merge the contents of this block into the next one using this block's buffer.
  final void mergeToRight() {
    var n = next;
    var nlen = n.pos - n.start;
    System.arraycopy(n.buf, n.start, buf, pos, nlen);
    var len = pos - start;
    n.totalFlushed -= len;
    var tmpbuf = buf;
    buf = n.buf;
    n.buf = tmpbuf;
    n.start = start;
    n.lim = tmpbuf.length;
    n.pos = pos + nlen;
  }

  /// Switch a potentially shared block to exclusive after re-allocating its buffer.
  void unshare() {
    if(sharing == SHARING_RIGHT) {
      sharing = SHARING_EXCLUSIVE;
      if(prev.sharing == SHARING_LEFT) prev.sharing = SHARING_RIGHT;
    }
  }

  private void growBufferClear(int count) {
    var tlim = (int)Math.min(totalLimit - totalBytesWritten(), count);
    var buflen = growBuffer(buf.length, tlim, 1);
    if(buflen > buf.length) buf = new byte[buflen];
    lim = buf.length;
    pos = 0;
    start = 0;
    unshare();
  }

  private void growBufferCopy(int count) {
    var tlim = (int)Math.min(totalLimit - totalBytesWritten(), pos + count);
    var buflen = growBuffer(buf.length, tlim, 2);
    //System.out.println("grow "+buf.length+", "+tlim+", "+buflen);
    if(buflen > buf.length) buf = Arrays.copyOf(buf, buflen);
    lim = buf.length;
    unshare();
  }

  /// Insert this block before the given block.
  void insertBefore(BufferedOutput b) {
    prev = b.prev;
    next = b;
    b.prev.next = this;
    b.prev = this;
  }

  /// Insert this block and its prefix list before the given block.
  void insertAllBefore(BufferedOutput b) {
    var n = next;
    n.prev = b.prev;
    b.prev.next = n;
    next = b;
    b.prev = this;
  }

  /// Unlink this block and return it to the cache.
  void unlinkAndReturn() {
    prev.next = next;
    next.prev = prev;
    cache.returnToCache((NestedBufferedOutput)this);
  }

  /// Unlink this block.
  void unlinkOnly() {
    //System.out.println("unlinkOnly "+this.showThis());
    prev.next = next;
    next.prev = prev;
  }

  /// Flush and unlink all closed blocks and optionally flush the root block. Must only be called on the root block.
  abstract void flushBlocks(boolean forceFlush) throws IOException;

  /// Force flush to the upstream after flushBlocks. Must only be called on the root block.
  void flushUpstream() throws IOException {}

  /// Called at the end of the first [#close()].
  void closeUpstream() throws IOException {}

  /// Flush the written data as far as possible. Note that not all data may be flushed if there is a previous
  /// BufferedOutput created with [#reserve(long)] that has not been fully written and closed yet.
  public final void flush() throws IOException {
    checkState();
    if(rootBlock == topLevel) {
      rootBlock.flushBlocks(true);
      rootBlock.flushUpstream();
    }
  }

  /// Close this BufferedOutput and mark it as closed. Calling [#close()] again has no effect,
  /// calling most other methods after closing results in an [IOException].
  ///
  /// If this is a root BufferedOutput based on an [OutputStream] or similar data sink, any
  /// outstanding data is flushed to the sink which is then closed as well. In case of a
  /// [ArrayBufferedOutput], the data becomes available for reading once it is closed.
  ///
  /// Closing a nested BufferedOutput created with [#reserve(long)] before writing all the
  /// requested data results in an IOException.
  ///
  /// In all cases any nested BufferedOutputs that have not been closed yet are implicitly
  /// closed when closing this BufferedOutput.
  public final void close() throws IOException {
    if(state != STATE_OPEN) return;
    if(!truncate) checkUnderflow();
    state = STATE_CLOSED;
    lim = pos;
    // We always unlink SHARING_LEFT when closing so we don't have to merge blocks later when
    // flushing. They are always created by reserveShort() and they cannot be root blocks.
    if(sharing == SHARING_LEFT) {
      var n = next;
      n.start = start;
      n.totalFlushed -= (pos - start);
      var pre = prev;
      var r = rootBlock;
      unlinkAndReturn();
      if(pre == r) r.flushBlocks(false);
    } else {
      if(rootBlock == this) {
        for(var b = next; b != this;) {
          var bn = b.next;
          if(b.state == STATE_OPEN) {
            if(!b.truncate) b.checkUnderflow();
            if(b.sharing == SHARING_LEFT) {
              bn.start = b.start;
              bn.totalFlushed -= (b.pos - b.start);
              b.unlinkAndReturn();
            } else {
              b.lim = b.pos;
              b.state = STATE_CLOSED;
            }
          }
          b = bn;
        }
        if(this == topLevel) flushBlocks(true);
      } else if(prev == rootBlock) rootBlock.flushBlocks(false);
      assert state == STATE_CLOSED;
      assert(pos == lim);
      closeUpstream();
    }
  }

  /// Reserve a short block (fully allocated) by splitting this block into two shared blocks.
  private BufferedOutput reserveShort(int length) throws IOException {
    var p = fwd(length);
    var len = p - start;
    var b = cache.getSharedBlock();
    b.reinit(buf, bigEndian, start, p, pos, SHARING_LEFT, length, -len, false, rootBlock, null, topLevel);
    b.insertBefore(this);
    totalFlushed += (pos - start);
    start = pos;
    if(sharing == SHARING_EXCLUSIVE) sharing = SHARING_RIGHT;
    return b;
  }

  /// Reserve a long block (flushed on demand) by allocating a new block. Must not be called on a shared block.
  private BufferedOutput reserveLong(long max) throws IOException {
    var len = pos - start;
    var b = cache.getExclusiveBlock();
    var l = lim;
    if(l - pos > max) l = (int)(max + pos);
    buf = b.reinit(buf, bigEndian, start, pos, l, sharing, max-len, -len, false, rootBlock, null, topLevel);
    b.insertBefore(this);
    totalFlushed = totalFlushed + len + max;
    sharing = SHARING_EXCLUSIVE;
    pos = 0;
    lim = buf.length;
    return b;
  }

  /// Leave a fixed-size gap at the current position that can be filled later after continuing to
  /// write to this BufferedOutput. [#totalBytesWritten()] is immediately increased by the
  /// requested size. Attempting to write more than the requested amount of data to the returned
  /// BufferedOutput or closing it before writing all of the data throws an IOException.
  ///
  /// @param length The number of bytes to skip in this BufferedOutput and return as a new
  ///               BufferedOutput.
  /// @return       A new BufferedOutput used to fill the gap.
  public final BufferedOutput reserve(long length) throws IOException {
    checkState();
    if(fixed) return reserveShort((int)Math.min(length, available()));
    else if(length < topLevel.initialBufferSize) return reserveShort((int)length); //TODO always use reserveShort when it fits?
    else return reserveLong(length);
  }

  /// Create a new BufferedOutput `b` that gets appended to `this` when it is closed. If `this` has a limited
  /// size and appending `b` would exceed the limit, an [EOFException] is thrown when attempting to close `b`.
  /// Attempting to write more than the requested maximum length to `b` results in an [IOException].
  ///
  /// @param max The size limit of the returned BufferedOutput
  /// @return    A new BufferedOutput
  public final BufferedOutput defer(long max) throws IOException {
    checkState();
    var b = cache.getExclusiveBlock();
    b.reinit(b.buf, bigEndian, 0, 0, b.buf.length, SHARING_EXCLUSIVE, max, 0L, true, b, this, topLevel);
    b.next = b;
    b.prev = b;
    return b;
  }

  /// Same as `defer(Long.MAX_VALUE)`
  /// @see #defer(long)
  public final BufferedOutput defer() throws IOException { return defer(Long.MAX_VALUE); }

  /// Append a nested root block.
  void appendNested(BufferedOutput r) throws IOException {
    //System.out.println("appendNested "+this+" "+r);
    if(!appendNestedMerge(r)) appendNestedSwap(r);
  }

  private boolean appendNestedMerge(BufferedOutput r) throws IOException {
    var b = r.next;
    while(true) {
      var blen = b.pos - b.start;
      if(lim - pos < blen) return false;
      System.arraycopy(b.buf, b.start, buf, pos, blen);
      pos += blen;
      var bn = b.next;
      if(b == r) {
        cache.returnToCache((NestedBufferedOutput)r);
        return true;
      }
      b.unlinkAndReturn();
      r.totalFlushed -= blen;
      b = bn;
    }
  }

  private void appendNestedSwap(BufferedOutput b) throws IOException {
    var tmpbuf = buf;
    var tmpstart = start;
    var tmppos = pos;
    var tmpsharing = sharing;
    var len = tmppos - tmpstart;
    var blen = b.pos - b.start;
    if(totalBytesWritten() + b.totalBytesWritten() > totalLimit) throw new EOFException();
    totalFlushed += b.totalFlushed + len;
    b.totalFlushed += blen - len;
    buf = b.buf;
    start = b.start;
    pos = b.pos;
    lim = buf.length;
    sharing = b.sharing;
    b.buf = tmpbuf;
    b.start = tmpstart;
    b.pos = tmppos;
    b.lim = tmppos;
    b.sharing = tmpsharing;
    b.rootBlock = rootBlock;
    var bp = b.prev;
    bp.insertAllBefore(this);
    assert b.state != STATE_OPEN;
    if(bp.prev == this) flushBlocks(false);
    //System.out.println("root.numBlocks: "+root.numBlocks());
  }

  private int numBlocks() {
    var b = next;
    int i = 1;
    while(b != this) {
      i++;
      b = b.next;
    }
    return i;
  }

  /// Create a TextOutput for reading text from this BufferedOutput.
  ///
  /// @param cs        The Charset for decoding text.
  /// @param eol       The line separator.
  public TextOutput text(Charset cs, String eol) { return TextOutput.of(this, cs, eol); }

  /// Same as `text(cs, System.lineSeparator())`
  /// @see #text(Charset, String)
  public TextOutput text(Charset cs) { return text(cs, System.lineSeparator()); }

  /// Same as `text(StandardCharsets.UTF_8, System.lineSeparator())`
  /// @see #text(Charset, String)
  public TextOutput text() { return text(StandardCharsets.UTF_8, System.lineSeparator()); }

  final String showThis() {
    var b = new StringBuilder();
    if(this == topLevel) b.append('*');
    if(this == rootBlock) b.append('+');
    return b.append(System.identityHashCode(this))
      .append(':').append(showSharing())
      .append(':').append(showState())
      .append(",start=").append(start)
      .append(",pos=").append(pos)
      .append(",lim=").append(lim)
      .append(",len=").append(pos-start)
      .toString();
  }

  final String showContents() {
    var b = new StringBuilder("[");
    for(int i=start; i<pos; i++) {
      if(i > start) b.append(',');
      b.append(Integer.toHexString(buf[i]&0xff));
    }
    return b.append(']').toString();
  }

  final String showList() {
    var n = rootBlock.next;
    var b = new StringBuilder();
    while(true) {
      if(n == this) b.append('<');
      b.append(n.showThis());
      if(n == this) b.append('>');
      if(n == rootBlock) return b.toString();
      b.append(" -> ");
      n = n.next;
    }
  }

  final String showSharing() {
    return switch(sharing) {
      case SHARING_EXCLUSIVE -> "E";
      case SHARING_LEFT -> "L";
      case SHARING_RIGHT -> "R";
      default -> String.valueOf(sharing);
    };
  }

  final String showState() {
    return switch(state) {
      case STATE_OPEN -> "O";
      case STATE_CLOSED -> "C";
      default -> String.valueOf(state);
    };
  }
}


final class NestedBufferedOutput extends BufferedOutput {
  /// The parent from which this block was created by [#defer()], otherwise null.
  private BufferedOutput parent = null;
  
  boolean nocache;

  NestedBufferedOutput(byte[] buf, boolean fixed, TopLevelBufferedOutput topLevel) {
    super(buf, false, 0, 0, 0, fixed, Long.MAX_VALUE, topLevel, topLevel);
  }

  /// Re-initialize this block and return its old buffer.
  byte[] reinit(byte[] buf, boolean bigEndian, int start, int pos, int lim, byte sharing,
      long totalLimit, long totalFlushed, boolean truncate, BufferedOutput rootBlock,
      BufferedOutput parent, TopLevelBufferedOutput topLevel) {
    var b = this.buf;
    this.buf = buf;
    this.bigEndian = bigEndian;
    this.start = start;
    this.pos = pos;
    this.lim = lim;
    this.sharing = sharing;
    this.totalLimit = totalLimit;
    this.totalFlushed = totalFlushed;
    this.truncate = truncate;
    this.rootBlock = rootBlock;
    this.parent = parent;
    this.state = STATE_OPEN;
    this.topLevel = topLevel;
    return b;
  }

  void flushBlocks(boolean forceFlush) {}

  @Override
  void closeUpstream() throws IOException {
    if(parent != null) parent.appendNested(this);
  }
}


abstract class TopLevelBufferedOutput extends BufferedOutput {
  final int initialBufferSize;

  TopLevelBufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, int initialBufferSize, boolean fixed, long totalLimit, TopLevelBufferedOutput cache) {
    super(buf, bigEndian, start, pos, lim, fixed, totalLimit, null, cache);
    this.initialBufferSize = initialBufferSize;
  }

  private NestedBufferedOutput cachedExclusive, cachedShared = null; // single-linked lists (via `next`) of blocks cached for reuse

  /// Prefer splitting the current block over growing if it's sufficiently filled
  abstract boolean preferSplit();

  void closeUpstream() throws IOException {
    cachedExclusive = null;
    cachedShared = null;
  }

  void returnToCache(NestedBufferedOutput b) {
    if(!b.nocache) {
      b.topLevel = null;
      b.rootBlock = null;
      if(b.sharing == SHARING_LEFT) {
        b.buf = null;
        b.next = cachedShared;
        cachedShared = b;
      } else {
        //System.out.println("Returning exclusive block to "+this);
        b.next = cachedExclusive;
        cachedExclusive = b;
      }
    }
  }

  /// Get a cached or new exclusive block.
  NestedBufferedOutput getExclusiveBlock() {
    if(cachedExclusive == null) {
      //System.out.println("New exclusive block in "+this);
      return new NestedBufferedOutput(new byte[initialBufferSize], false, this);
    } else {
      //System.out.println("Cached exclusive block "+cachedExclusive.showThis());
      var b = cachedExclusive;
      cachedExclusive = (NestedBufferedOutput)b.next;
      return b;
    }
  }

  /// Get a cached or new shared block.
  NestedBufferedOutput getSharedBlock() {
    if(cachedShared == null) {
      //System.out.println("New shared block");
      return new NestedBufferedOutput(null, true, this);
    } else {
      //System.out.println("Cached shared block");
      var b = cachedShared;
      cachedShared = (NestedBufferedOutput)b.next;
      return b;
    }
  }
}


final class FlushingBufferedOutput extends TopLevelBufferedOutput {
  private final OutputStream out;

  FlushingBufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, int initialBufferSize, boolean fixed, long totalLimit, OutputStream out) {
    super(buf, bigEndian, start, pos, lim, initialBufferSize, fixed, totalLimit, null);
    this.out = out;
  }

  boolean preferSplit() { return false; }

  @Override
  void flushUpstream() throws IOException { out.flush(); }

  @Override
  void closeUpstream() throws IOException {
    super.closeUpstream();
    out.close();
  }

  void flushBlocks(boolean forceFlush) throws IOException {
    var b = next;
    while(true) {
      var bn = b.next;
      var blen = b.pos - b.start;
      if(b.state != BufferedOutput.STATE_CLOSED) {
        if((forceFlush && blen > 0) || !b.fixed && blen > initialBufferSize/2) {
          writeToOutput(b.buf, b.start, blen);
          b.totalFlushed += blen;
          if(b.fixed) b.start = b.pos;
          else {
            b.pos = 0;
            b.start = 0;
            // `b.lim -= blen` would be the conservative choice, but we can immediately extend
            // the limit as long as we're under the totalLimit so we don't have to leave the inner
            // loop again to grow it later.
            b.lim = (int)Math.min(b.totalLimit - b.totalFlushed, b.lim);
          }
        }
        return;
      }
      if(b == this) {
        if(blen > 0) {
          writeToOutput(b.buf, b.start, blen);
          totalFlushed += blen;
        }
        return;
      }
      if(blen < initialBufferSize/2 && maybeMergeToRight(b)) {}
      else if(blen > 0) writeToOutput(b.buf, b.start, blen);
      b.unlinkAndReturn();      
      b = bn;
    }
  }

  private boolean maybeMergeToRight(BufferedOutput b) {
    var n = b.next;
    if(!n.fixed && (b.pos + (n.pos - n.start) < initialBufferSize)) {
      b.mergeToRight();
      return true;
    } else return false;
  }

  /// Write the buffer to the output. Must only be called on the root block.
  private void writeToOutput(byte[] buf, int off, int len) throws IOException {
    out.write(buf, off, len);
  }
}
