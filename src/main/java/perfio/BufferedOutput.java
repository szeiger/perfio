package perfio;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import static perfio.BufferUtil.*;


/// BufferedOutput provides buffered streaming writes to an OutputStream or similar data sink.
///
/// The API is not thread-safe. Access from multiple threads must be synchronized externally.
/// The number of bytes written is tracked as a 64-bit signed long value [#totalBytesWritten()].
/// The behaviour after writing more than [Long#MAX_VALUE] (8 exabytes) is undefined.
///
/// A freshly created BufferedOutput always uses [ByteOrder#BIG_ENDIAN]. This can be changed with
/// [#order(ByteOrder)]. Unless specified explicitly, the initial buffer size is 32768.
public abstract sealed class BufferedOutput implements Closeable, Flushable permits CacheRootBufferedOutput, NestedBufferedOutput {

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
  /// @param initialBufferSize Initial buffer size. The buffer is expanded later if necessary.
  public static ArrayBufferedOutput growing(int initialBufferSize) {
    var buf = new byte[Math.max(initialBufferSize, MinBufferSize)];
    return new ArrayBufferedOutput(buf, true, 0, 0, buf.length, initialBufferSize, false);
  }

  /// Write data to an internal byte array buffer that can be accessed directly or copied after
  /// closing the BufferedOutput (similar to [ByteArrayOutputStream]) using the default initial
  /// buffer size. The size is limited to 2 GB (the maximum size of a byte array).
  /// @see #growing(int)
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

  private static final int MinBufferSize = 16;
  private static final int DefaultBufferSize = 32768;

  static final byte SHARING_EXCLUSIVE = (byte)0; // buffer is not shared
  static final byte SHARING_LEFT      = (byte)1; // buffer is shared with next block
  static final byte SHARING_RIGHT     = (byte)2; // buffer is shared with previous blocks only


  // ======================================================= non-static parts:

  byte[] buf;
  boolean bigEndian;
  int start, pos, lim;
  final boolean fixed;
  long totalLimit;

  BufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, boolean fixed, long totalLimit) {
    this.buf = buf;
    this.bigEndian = bigEndian;
    this.start = start;
    this.pos = pos;
    this.lim = lim;
    this.fixed = fixed;
    this.totalLimit = totalLimit;
  }

  long totalFlushed = 0L;
  BufferedOutput next = this; // prefix list as a double-linked ring
  BufferedOutput prev = this;
  boolean closed = false;
  boolean truncate = true;
  BufferedOutput root = this;
  CacheRootBufferedOutput cacheRoot = null;
  byte sharing = SHARING_EXCLUSIVE;

  /// Change the byte order of this BufferedOutput.
  public BufferedOutput order(ByteOrder order) {
    bigEndian = order == ByteOrder.BIG_ENDIAN;
    return this;
  }

  /// Return the byte order of this BufferedOutput.
  public final ByteOrder order() { return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN; }

  void checkState() throws IOException {
    if(closed) throw new IOException("BufferedOutput has already been closed");
  }

  private int available() { return lim - pos; }

  /// Return the total number of bytes written to this BufferedOutput.
  public final long totalBytesWritten() { return totalFlushed + (pos - start); }

  int fwd(int count) throws IOException {
    // The goal here is to allow HotSpot to elide range checks in some scenarios (e.g.
    // `BufferedOutputUncheckedBenchmark`). The condition should be `available() < count`,
    // i.e. `lim - pos < count` but this does not elide range checks. `pos + count > lim` does,
    // but it would produce an incorrect result due to signed arithmetic if `pos + count`
    // overflows. Using the `Integer.compareUnsigned` intrinsic does not elide the range checks.
    // The double condition does, with no performance impact in the benchmarks.
    if(pos+count > lim || pos+count < 0) {
      flushAndGrow(count);
      if(pos+count > lim || pos+count < 0) throw new EOFException();
    }
    var p = pos;
    pos += count;
    return p;
  }

  int tryFwd(int count) throws IOException {
    if(pos+count > lim || pos+count < 0) flushAndGrow(count);
    var p = pos;
    pos += Math.min(count, available());
    return p;
  }

  /// Write the contents of the given array region.
  public final BufferedOutput write(byte[] a, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, a.length);
    var p = fwd(len);
    System.arraycopy(a, off, buf, p, len);
    return this;
  }

  /// Write the contents of the given array.
  public final BufferedOutput write(byte[] a) throws IOException { return write(a, 0, a.length); }

  /// Write a signed 8-bit integer (`byte`).
  public final BufferedOutput int8(byte b) throws IOException {
    var p = fwd(1);
    buf[p] = b;
    return this;
  }

  /// Write the lower 8 bits of the given `int` as an unsigned 8-bit integer.
  public final BufferedOutput uint8(int b) throws IOException { return int8((byte)b); }

  /// Write a signed 16-bit integer (`short`) in the current [#order()].
  public final BufferedOutput int16(short s) throws IOException {
    var p = fwd(2);
    (bigEndian ? BA_SHORT_BIG : BA_SHORT_LITTLE).set(buf, p, s);
    return this;
  }

  /// Write an unsigned 16-bit integer (`char`) in the current [#order()].
  public final BufferedOutput uint16(char c) throws IOException {
    var p = fwd(2);
    (bigEndian ? BA_CHAR_BIG : BA_CHAR_LITTLE).set(buf, p, c);
    return this;
  }

  /// Write a signed 32-bit integer (`int`) in the current [#order()].
  public final BufferedOutput int32(int i) throws IOException {
    var p = fwd(4);
    (bigEndian ? BA_INT_BIG : BA_INT_LITTLE).set(buf, p, i);
    return this;
  }

  /// Write the lower 32 bits of the given `long` as an unsigned 32-bit integer in the current [#order()].
  public final BufferedOutput uint32(long i) throws IOException { return int32((int)i); }

  /// Write a signed 64-bit integer (`long`) in the current [#order()].
  public final BufferedOutput int64(long l) throws IOException {
    var p = fwd(8);
    (bigEndian ? BA_LONG_BIG : BA_LONG_LITTLE).set(buf, p, l);
    return this;
  }

  /// Write a 32-bit IEEE-754 floating point value (`float`) in the current [#order()].
  public final BufferedOutput float32(float f) throws IOException {
    var p = fwd(4);
    (bigEndian ? BA_FLOAT_BIG : BA_FLOAT_LITTLE).set(buf, p, f);
    return this;
  }

  /// Write a 64-bit IEEE-754 floating point value (`double`) in the current [#order()].
  public final BufferedOutput float64(double d) throws IOException {
    var p = fwd(8);
    (bigEndian ? BA_DOUBLE_BIG : BA_DOUBLE_LITTLE).set(buf, p, d);
    return this;
  }

  /// Write a [String] in the given [Charset].
  ///
  /// Encoding is performed as a standalone operation with the default replacement characters. It is not possible
  /// to write surrogate pairs that are split across 2 strings. Use [TextOutput] if you need this feature.
  ///
  /// The encoded data is written verbatim without a length prefix or terminator.
  public final int string(String s, Charset charset) throws IOException {
    if(charset == StandardCharsets.UTF_8) return stringUtf8(s, 0);
    else if(charset == StandardCharsets.ISO_8859_1) return stringLatin1(s, 0);
    return stringFallback(s, charset, 0);
  }

  /// Write a [String] in UTF-8 encoding.
  ///
  /// Note that this is the standard UTF-8 format. It is not compatible with [DataInput] /
  /// [DataOutput] which use a non-standard variant.
  ///
  /// @see #string(String, Charset)
  public final int string(String s) throws IOException { return stringUtf8(s, 0); }

  private int stringFallback(String s, Charset charset, int extra) throws IOException {
    var b = s.getBytes(charset);
    var p = fwd(b.length + extra);
    System.arraycopy(b, 0, buf, p, b.length);
    return b.length + extra;
  }

  private int stringLatin1(String s, int extra) throws IOException {
    var l = s.length();
    if(l == 0) return 0;
    else {
      if(StringInternals.isLatin1(s)) {
        var p = fwd(l + extra);
        var v = StringInternals.value(s);
        System.arraycopy(v, 0, buf, p, v.length);
        return l + extra;
      } else return stringFallback(s, StandardCharsets.ISO_8859_1, extra);
    }
  }

  private int stringUtf8(String s, int extra) throws IOException {
    var l = s.length();
    if(l == 0) return 0;
    else {
      if(StringInternals.isLatin1(s)) {
        var v = StringInternals.value(s);
        if(!StringInternals.hasNegatives(v, 0, v.length)) {
          var p = fwd(l + extra);
          System.arraycopy(v, 0, buf, p, v.length);
          return l + extra;
        } else return stringFallback(s, StandardCharsets.UTF_8, extra);
      } else return stringFallback(s, StandardCharsets.UTF_8, extra);
    }
  }

  /// Write a zero-terminated [String] in the given [Charset].
  /// @see #string(String, Charset)
  public final int zstring(String s, Charset charset) throws IOException {
    int l = (charset == StandardCharsets.UTF_8) ? stringUtf8(s, 1)
        : (charset == StandardCharsets.ISO_8859_1) ? stringLatin1(s, 1)
        : stringFallback(s, charset, 1);
    if(l == 0) {
      int8((byte)0);
      return 1;
    } else {
      buf[pos-1] = 0;
      return l;
    }
  }

  /// Write a zero-terminated [String] in UTF-8 encoding.
  /// @see #string(String, Charset)
  public final int zstring(String s) throws IOException { return zstring(s, StandardCharsets.UTF_8); }

  void flushAndGrow(int count) throws IOException {
    checkState();
    if(!fixed) {
      //println(s"$show.flushAndGrow($count)")
      if(prev == root) {
        root.flushBlocks(true);
        if(lim < pos + count) {
          if(pos == start) growBufferClear(count);
          else growBufferCopy(count);
        }
      } else growBufferCopy(count);
    }
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

  /// Insert this block and its prefix list before the given block. Must only be called on a root block.
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
    cacheRoot.returnToCache(this);
  }

  /// Unlink this block.
  void unlinkOnly() {
    prev.next = next;
    next.prev = prev;
  }

  /// Flush and unlink all closed blocks and optionally flush the root block. Must only be called on the root block.
  abstract void flushBlocks(boolean forceFlush) throws IOException;

  /// Force flush to the upstream after flushBlocks. Must only be called on the root block.
  abstract void flushUpstream() throws IOException;

  /// Called at the end of the first [#close()].
  void closeUpstream() throws IOException {}

  /// Flush the written data as far as possible. Note that not all data may be flushed if there is a previous
  /// BufferedOutput created with [#reserve(long)] that has not been fully written and closed yet.
  public final void flush() throws IOException {
    checkState();
    if(prev == root) {
      root.flushBlocks(true);
      root.flushUpstream();
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
    if(!closed) {
      if(!truncate) checkUnderflow();
      closed = true;
      lim = pos;
      if(root == this) {
        for(var b = next; b != this; b = b.next) {
          if(!b.closed) {
            if(!b.truncate) b.checkUnderflow();
            b.closed = true;
          }
        }
        flushBlocks(true);
      } else {
        if(prev == root) root.flushBlocks(false);
      }
      closeUpstream();
    }
  }

  /// Reserve a short block (fully allocated) by splitting this block into two shared blocks.
  private BufferedOutput reserveShort(int length) throws IOException {
    var p = fwd(length);
    var len = p - start;
    var b = cacheRoot.getSharedBlock();
    b.reinit(buf, bigEndian, start, p, pos, SHARING_LEFT, length, -len, false, root, null);
    b.insertBefore(this);
    totalFlushed += (pos - start);
    start = pos;
    if(sharing == SHARING_EXCLUSIVE) sharing = SHARING_RIGHT;
    return b;
  }

  /// Reserve a long block (flushed on demand) by allocating a new block. Must not be called on a shared block.
  private BufferedOutput reserveLong(long max) throws IOException {
    var len = pos - start;
    var b = cacheRoot.getExclusiveBlock();
    var l = lim;
    if(l - pos > max) l = (int)(max + pos);
    buf = b.reinit(buf, bigEndian, start, pos, l, sharing, max-len, -len, false, root, null);
    b.insertBefore(this);
    totalFlushed = totalFlushed + len + max;
    sharing = SHARING_EXCLUSIVE;
    pos = 0;
    lim = buf.length;
    return b;
  }

  /// Leave a fixed-size gap at the current position that can be filled later after continuing to
  /// write to this BufferedOutput. `this.totalBytesWritten()` is immediately increased by the
  /// requested size. Attempting to write more than the requested amount of data to the returned
  /// BufferedOutput or closing it before writing all of the data throws an IOException.
  ///
  /// @param length The number of bytes to skip in this BufferedOutput and return as a new
  ///               BufferedOutput.
  /// @return       A new BufferedOutput used to fill the gap.
  public final BufferedOutput reserve(long length) throws IOException {
    checkState();
    if(fixed) return reserveShort((int)Math.min(length, available()));
    else if(length < cacheRoot.initialBufferSize) return reserveShort((int)length);
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
    var b = cacheRoot.getExclusiveBlock();
    b.reinit(b.buf, bigEndian, 0, 0, b.buf.length, SHARING_EXCLUSIVE, max, 0L, true, b, this);
    b.next = b;
    b.prev = b;
    return b;
  }

  /// Same as `defer(Long.MAX_VALUE)`
  /// @see #defer(long)
  public final BufferedOutput defer() throws IOException { return defer(Long.MAX_VALUE); }

  /// Append a nested root block.
  void appendNested(BufferedOutput b) throws IOException {
    var btot = b.totalBytesWritten();
    var rem = totalLimit - btot;
    if(btot > rem) throw new EOFException();
    if(fixed || b.totalBytesWritten() <= available()) mergeNested(b);
    else {
      var tmpbuf = buf;
      var tmpstart = start;
      var tmppos = pos;
      var tmpsharing = sharing;
      var len = tmppos - tmpstart;
      var blen = b.pos - b.start;
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
      b.closed = true;
      b.sharing = tmpsharing;
      var bp = b.prev;
      bp.insertAllBefore(this);
      if(bp.prev == this) flushBlocks(false);
      //System.out.println("root.numBlocks: "+root.numBlocks());
    }
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

  private void mergeNested(BufferedOutput r) throws IOException {
    for(var b = r.next; true;) {
      var blen = b.pos - b.start;
      if(blen > 0) {
        var p = fwd(blen);
        System.arraycopy(b.buf, b.start, buf, p, blen);
      }
      var bn = b.next;
      cacheRoot.returnToCache(b);
      if(b == r) return;
      b = bn;
    }
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
}


final class NestedBufferedOutput extends BufferedOutput {
  private BufferedOutput parent = null;

  NestedBufferedOutput(byte[] buf, boolean fixed, CacheRootBufferedOutput cacheRoot) {
    super(buf, false, 0, 0, 0, fixed, Long.MAX_VALUE);
    this.cacheRoot = cacheRoot;
  }

  /// Re-initialize this block and return its old buffer.
  byte[] reinit(byte[] buf, boolean bigEndian, int start, int pos, int lim, byte sharing,
    long totalLimit, long totalFlushed, boolean truncate, BufferedOutput root, BufferedOutput parent) {
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
    this.root = root;
    this.parent = parent;
    closed = false;
    return b;
  }

  void flushBlocks(boolean forceFlush) {}

  void flushUpstream() {}

  @Override
  void closeUpstream() throws IOException {
    if(parent != null) parent.appendNested(this);
  }
}


sealed abstract class CacheRootBufferedOutput extends BufferedOutput permits FlushingBufferedOutput, AccumulatingBufferedOutput {
  final int initialBufferSize;

  CacheRootBufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, int initialBufferSize, boolean fixed, long totalLimit) {
    super(buf, bigEndian, start, pos, lim, fixed, totalLimit);
    this.initialBufferSize = initialBufferSize;
    this.cacheRoot = this;
  }

  private BufferedOutput cachedExclusive, cachedShared = null; // single-linked lists (via `next`) of blocks cached for reuse

  void closeUpstream() throws IOException {
    cachedExclusive = null;
    cachedShared = null;
  }

  void returnToCache(BufferedOutput b) {
    if(b.sharing == SHARING_LEFT) {
      b.buf = null;
      b.next = cachedShared;
      cachedShared = b;
    } else {
      b.next = cachedExclusive;
      cachedExclusive = b;
    }
  }

  /// Get a cached or new exclusive block.
  NestedBufferedOutput getExclusiveBlock() {
    if(cachedExclusive == null)
      return new NestedBufferedOutput(new byte[cacheRoot.initialBufferSize], false, cacheRoot);
    else {
      var b = cachedExclusive;
      cachedExclusive = b.next;
      return (NestedBufferedOutput)b;
    }
  }

  /// Get a cached or new shared block.
  NestedBufferedOutput getSharedBlock() {
    if(cachedShared == null)
      return new NestedBufferedOutput(null, true, cacheRoot);
    else {
      var b = cachedShared;
      cachedShared = b.next;
      return (NestedBufferedOutput)b;
    }
  }
}


final class FlushingBufferedOutput extends CacheRootBufferedOutput {
  private final OutputStream out;

  FlushingBufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, int initialBufferSize, boolean fixed, long totalLimit, OutputStream out) {
    super(buf, bigEndian, start, pos, lim, initialBufferSize, fixed, totalLimit);
    this.out = out;
  }

  void flushUpstream() throws IOException { out.flush(); }

  void flushBlocks(boolean forceFlush) throws IOException {
    while(next != this) {
      var b = next;
      var blen = b.pos - b.start;
      if(!b.closed) {
        if((forceFlush && blen > 0) || blen > initialBufferSize/2) {
          writeToOutput(b.buf, b.start, blen);
          b.totalFlushed += blen;
          if(b.sharing == SHARING_LEFT) b.start = b.pos;
          else {
            b.pos = 0;
            b.start = 0;
            b.lim -= blen;
          }
        }
        return;
      }
      if(b.sharing == SHARING_LEFT) {
        var n = b.next;
        n.start = b.start;
        n.totalFlushed -= blen;
      } else {
        if(blen < initialBufferSize/2 && maybeMergeToRight(b)) {}
        else if(blen > 0) writeToOutput(b.buf, b.start, blen);
      }
      b.unlinkAndReturn();
    }
    var len = pos-start;
    if((forceFlush && len > 0) || len > initialBufferSize/2) {
      writeToOutput(buf, start, len);
      totalFlushed += len;
      pos = start;
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
