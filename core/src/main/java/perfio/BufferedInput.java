package perfio;

import perfio.internal.BufferUtil;
import perfio.internal.LineBuffer;

import java.io.*;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/// BufferedInput provides buffered streaming reads from an InputStream or similar data source.
///
/// The API is not thread-safe. Access from multiple threads must be synchronized externally.
/// The number of bytes read is tracked as a 64-bit signed long value [#totalBytesRead()]. The
/// behaviour after reading more than [Long#MAX_VALUE] (8 exabytes) is undefined.
///
/// A BufferedInput created from a ByteBuffer defaults to the same byte order as the ByteBuffer,
/// otherwise [ByteOrder#BIG_ENDIAN]. This can be changed with [#order(ByteOrder)].
public abstract class BufferedInput extends ReadableBuffer implements Closeable {

  /// Read data from an [InputStream] using the [#DEFAULT_BUFFER_SIZE] and [ByteOrder#BIG_ENDIAN].
  /// Same as `of(in, DEFAULT_BUFFER_SIZE)`.
  ///
  /// @param in         InputStream from which to read data.
  /// @see #of(InputStream, int)
  public static BufferedInput of(InputStream in) { return of(in, DEFAULT_BUFFER_SIZE); }

  /// Read data from an [InputStream] using the default [ByteOrder#BIG_ENDIAN].
  ///
  /// @param in                InputStream from which to read data.
  /// @param initialBufferSize Initial size of the buffer in bytes. It is automatically extended if necessary. This also
  ///                          affects the minimum block size to read from the InputStream. BufferedInput tries to read
  ///                          at least half of initialBufferSize at once.
  public static BufferedInput of(InputStream in, int initialBufferSize) {
    var buf = new byte[Math.max(initialBufferSize, MIN_BUFFER_SIZE)];
    return new StreamingHeapBufferedInput(buf, buf.length, buf.length, Long.MAX_VALUE, in, buf.length/2, null, true);
  }

  /// Same as `ofArray(buf, 0, buf.length)`.
  /// @see #ofArray(byte[], int, int)
  public static BufferedInput ofArray(byte[] buf) { return ofArray(buf, 0, buf.length); }

  /// Read data from part of a byte array using the default [ByteOrder#BIG_ENDIAN].
  ///
  /// @param buf        Array from which to read data.
  /// @param off        Starting point within the array.
  /// @param len        Length of the data within the array.
  public static BufferedInput ofArray(byte[] buf, int off, int len) {
    Objects.checkFromIndexSize(off, len, buf.length);
    return new StreamingHeapBufferedInput(buf, off, off+len, Long.MAX_VALUE, null, 0, null, true);
  }

  /// Read data from a [MemorySegment] using the default [ByteOrder#BIG_ENDIAN]. Use
  /// [MemorySegment#asSlice(long,long)] for reading only part of a segment.
  ///
  /// @param ms         Segment from which to read.
  /// @param closeable  An optional [Closeable] object to close when the BufferedInput is closed, otherwise null.
  ///                   This can be used to deallocate a segment which is not managed by the garbage-collector.
  public static BufferedInput ofMemorySegment(MemorySegment ms, Closeable closeable) {
    var len = ms.byteSize();
    return len > MaxDirectBufferSize
        ? create(ms.asSlice(0, MaxDirectBufferSize), ms, closeable)
        : create(ms, ms, closeable);
  }

  /// Same as `ofMemorySegment(ms, null)`.
  /// @see #ofMemorySegment(MemorySegment, Closeable)
  public static BufferedInput ofMemorySegment(MemorySegment ms) { return ofMemorySegment(ms, null); }

  /// Read data from a [ByteBuffer]. The BufferedInput will start at the current position and end at the current
  /// limit of the ByteBuffer. The initial byte order is also taken from the ByteBuffer. The ByteBuffer's position
  /// may be temporarily modified during initialization so it should not be used concurrently. After this method
  /// returns, the ByteBuffer is treated as read-only and never modified (including its position, limit and byte order).
  public static BufferedInput ofByteBuffer(ByteBuffer bb) {
    if(bb.isDirect()) {
      var p = bb.position();
      bb.position(0);
      var ms = MemorySegment.ofBuffer(bb);
      bb.position(p);
      return new DirectBufferedInput(bb, p, bb.limit(), bb.limit(), ms, null, null, new LineBuffer());
    }
    else return new StreamingHeapBufferedInput(bb.array(), bb.position(), bb.limit(), Long.MAX_VALUE, null, 0, null, bb.order() == ByteOrder.BIG_ENDIAN);
  }

  /// Read data from a file which is memory-mapped for efficient reading.
  ///
  /// @param file           File to read.
  public static BufferedInput ofMappedFile(Path file) throws IOException {
    return ofMemorySegment(BufferUtil.mapReadOnlyFile(file), null);
  }

  private static DirectBufferedInput create(MemorySegment bbSegment, MemorySegment ms, Closeable closeable) {
    var bb = bbSegment.asByteBuffer().order(ByteOrder.BIG_ENDIAN);
    return new DirectBufferedInput(bb, bb.position(), bb.limit(), ms.byteSize(), ms, closeable, null, new LineBuffer());
  }

  public static final int MIN_BUFFER_SIZE = BufferUtil.VECTOR_LENGTH * 2;
  public static final int DEFAULT_BUFFER_SIZE = BufferUtil.DEFAULT_BUFFER_SIZE;

  static int MaxDirectBufferSize = Integer.MAX_VALUE-15; //modified by unit tests

  private static final int STATE_LIVE        = 0;
  private static final int STATE_CLOSED      = 1;
  private static final int STATE_ACTIVE_VIEW = 2;


  // ======================================================= non-static parts:

  long totalReadLimit; // max number of bytes that may be returned
  private final BufferedInput viewParent;
  final BufferedInput viewRoot;
  final LineBuffer linebuf; // null in array-based buffers
  final MemorySegment ms;
  final Closeable closeable;
  
  BufferedInput(byte[] buf, int pos, int lim, long totalReadLimit, BufferedInput viewParent, boolean bigEndian, LineBuffer linebuf, MemorySegment ms, ByteBuffer bb, Closeable closeable) {
    this.buf = buf;
    this.pos = pos;
    this.lim = lim;
    this.totalReadLimit = totalReadLimit;
    this.viewParent = viewParent;
    this.bigEndian = bigEndian;
    this.totalBuffered = lim - pos;
    this.viewRoot = viewParent == null ? this : viewParent.viewRoot;
    this.linebuf = linebuf;
    this.ms = ms;
    this.bb = bb;
    this.closeable = closeable;
  }

  long totalBuffered; // total number of bytes read from input
  private int excessRead = 0; // number of bytes read into buf beyond lim if totalReadLimit was reached
  private int state = STATE_LIVE;
  private BufferedInput activeView = null;
  private int activeViewInitialBuffered = 0;
  private boolean detachOnClose, skipOnClose = false;
  private CloseableView closeableView = null;
  long parentTotalOffset = 0L;
  long bbStart = 0L; // offset of bb within ms

  abstract BufferedInput createEmptyView();

  void copyBufferFrom(BufferedInput b) {
    buf = b.buf;
    bb = b.bb;
    bbStart = b.bbStart;
  }

  void clearBuffer() {
    buf = null;
    bb = null;
  }
  
  String show() {
    return "pos="+pos+", lim="+lim+", totalReadLim="+totalReadLimit+", totalBuf="+totalBuffered+", excessRead="+excessRead+", parentTotalOff="+parentTotalOffset;
  }

  private void reinitView(int pos, int lim, long totalReadLimit, boolean skipOnClose, long parentTotalOffset, boolean bigEndian) {
    this.pos = pos;
    this.lim = lim;
    this.totalReadLimit = totalReadLimit;
    this.skipOnClose = skipOnClose;
    this.state = STATE_LIVE;
    this.excessRead = 0;
    this.totalBuffered = lim-pos;
    this.parentTotalOffset = parentTotalOffset;
    this.bigEndian = bigEndian;
    copyBufferFrom(viewParent);
  }

  void checkState() throws IOException {
    if(state != STATE_LIVE) {
      if(state == STATE_CLOSED) throw new IOException("BufferedInput has already been closed");
      else if(state == STATE_ACTIVE_VIEW) throw new IOException("Cannot use BufferedInput while a view is active");
    }
  }
  
  boolean isClosed() { return state == STATE_CLOSED; }

  /// Change the byte order of this BufferedInput.
  /// @return this BufferedInput
  public BufferedInput order(ByteOrder order) {
    bigEndian = order == ByteOrder.BIG_ENDIAN;
    return this;
  }

  /// The total number of bytes read from this BufferedInput, including child views but excluding the parent.
  public long totalBytesRead() throws IOException {
    checkState();
    return totalBuffered - available();
  }

  /// Fill the buffer as much as possible without blocking (starting at [#lim]), but at least
  /// until `count` bytes are available starting at [#pos] even if this requires blocking or
  /// growing the buffer. Less data may only be made available when the end of the input has been
  /// reached or nothing more can be read without exceeding the [#totalReadLimit]. The fields
  /// [#totalBuffered] and [#lim] are updated accordingly.
  ///
  /// If [#totalBuffered] would exceed [#totalReadLimit] after filling the buffer, any excess
  /// bytes must be counted as [#excessRead] and subtracted from [#totalBuffered] and [#lim].
  ///
  /// This method may change the buffer references.
  protected abstract void prepareAndFillBuffer(int count) throws IOException;

  // `bytes` and `skip` are implemented in terms of `request(1)`. This should always
  // be correct, and it works great for [SwitchingHeapBufferedInput] (because there is no more
  // direct way to transfer the data, and requesting 1 byte will never create a seam), but it is
  // not ideal for implementations that can copy/skip directly from the source.

  public void bytes(byte[] a, int off, int len) throws IOException {
    var tot = totalBytesRead() + len;
    if(tot < 0 || tot > totalReadLimit) throw new EOFException();
    while(len > 0) {
      tryFwd(1);
      if(available() == 0) throw new EOFException();
      var l = Math.min(len, available());
      if(l > 0) {
        if(buf != null) System.arraycopy(buf, pos, a, off, l);
        else bb.get(pos, a, off, l);
        pos += l;
        off += l;
        len -= l;
      }
    }
  }

  public final byte[] bytes(int len) throws IOException {
    var a = new byte[len];
    bytes(a, 0, len);
    return a;
  }

  /// Skip over n bytes, or until the end of the input if it occurs first.
  ///
  /// @return The number of skipped bytes. It is equal to the requested number unless the end of the input was reached.
  public long skip(final long bytes) throws IOException {
    checkState();
    final var limited = Math.min(bytes, totalReadLimit - totalBytesRead());
    var rem = limited;
    while(rem > 0) {
      tryFwd(1);
      if(available() == 0) return limited - rem;
      var l = Math.min(rem, available());
      if(l > 0) {
        pos += (int)l;
        rem -= l;
      }
    }
    return limited;
  }

  /// Read a non-terminated UTF-8 string.
  public final String string(int len) throws IOException { return string(len, StandardCharsets.UTF_8); }

  /// Read a non-terminated string of the specified encoded length.
  public String string(int len, Charset charset) throws IOException {
    if(len == 0) {
      checkState();
      return "";
    } else {
      var p = fwd(len);
      return buf != null ? new String(buf, p, len, charset) : makeDirectString(p, len, charset);
    }
  }

  /// Read a \0-terminated UTF-8 string.
  public final String zstring(int len) throws IOException { return zstring(len, StandardCharsets.UTF_8); }

  /// Read a \0-terminated string of the specified encoded length (including the \0).
  public String zstring(int len, Charset charset) throws IOException {
    if(len == 0) {
      checkState();
      return "";
    } else {
      var p = fwd(len);
      if(buf != null) {
        if(buf[pos-1] != 0) throw new IOException("Missing \\0 terminator in string");
        return len == 1 ? "" : new String(buf, p, len-1, charset);
      } else {
        if(bb.get(pos-1) != 0) throw new IOException("Missing \\0 terminator in string");
        return len == 1 ? "" : makeDirectString(p, len-1, charset);
      }
    }
  }

  private String makeDirectString(int start, int len, Charset charset) {
    var lb = linebuf.get(len);
    bb.get(start, lb, 0, len);
    return new String(lb, 0, len, charset);
  }

  public boolean hasMore() throws IOException {
    if(pos < lim) return true;
    prepareAndFillBuffer(1);
    return pos < lim;
  }

  /// Same as `close(true)`.
  /// 
  /// @see #close(boolean)
  public final void close() throws IOException { close(true); }

  /// Close this BufferedInput and mark it as closed. Calling this method again has no
  /// effect, calling most other methods after closing results in an [IOException].
  ///
  /// If this object is a view of another BufferedInput, this operation transfers control back
  /// to the parent.
  /// 
  /// @param closeUpstream If this is a root BufferedInput based on a data source that supports
  ///   closing (like an [InputStream]) or a [FilteringBufferedInput], this flag determines
  ///   whether to close the source as well. It has no effect on views (which never close their
  ///   parent).
  public final void close(boolean closeUpstream) throws IOException {
    if(state != STATE_CLOSED) {
      if(activeView != null) activeView.markClosed();
      if(viewParent != null) {
        if(skipOnClose) skip(Long.MAX_VALUE);
        viewParent.closedView(pos, lim + excessRead, totalBuffered + excessRead, detachOnClose);
      }
      if(viewParent == null) bufferClosed(closeUpstream);
      markClosed();
    }
  }

  /// Called at the end of the first [#close()].
  void bufferClosed(boolean closeUpstream) throws IOException {
    if(closeable != null && closeUpstream) closeable.close();
  }
  
  final void markClosed() {
    pos = lim;
    state = STATE_CLOSED;
    clearBuffer();
    if(closeableView != null) closeableView.markClosed();
  }

  private void closedView(int vpos, int vlim, long vTotalBuffered, boolean vDetach) {
    if(vTotalBuffered == activeViewInitialBuffered) { // view has not advanced the buffer
      pos = vpos;
      totalBuffered += vTotalBuffered;
    } else {
      copyBufferFrom(activeView);
      pos = vpos;
      lim = vlim;
      totalBuffered += vTotalBuffered;
      clampToLimit();
    }
    if(vDetach) activeView = null;
    state = STATE_LIVE;
  }

  /// Prevent reuse of this BufferedInput if it is a view. This ensures that this BufferedInput stays closed when
  /// a new view is created from the parent.
  ///
  /// @return this BufferedInput
  public BufferedInput detach() throws IOException {
    checkState();
    detachOnClose = true;
    return this;
  }

  /// Create a BufferedInput as a view that starts at the current position and is limited to reading the specified
  /// number of bytes. It will report EOF when the limit is reached or this BufferedInput reaches EOF. Views can be
  /// nested to arbitrary depths with no performance overhead or double buffering.
  ///
  /// This BufferedInput cannot be used until the view is closed. Calling close() on this BufferedInput is allowed and
  /// will also close the view. Any other method throws an IOException until the view is closed. Views of the same
  /// BufferedInput are reused. Calling [#limitedView(long, boolean)()] again will return the same object reinitialized
  /// to the new state unless the view was previously detached.
  ///
  /// @param limit         Maximum number of bytes in the view.
  /// @param skipRemaining Skip over the remaining bytes up to the limit in this BufferedInput if the view is closed
  ///                      without reading it fully.
  public BufferedInput limitedView(long limit, boolean skipRemaining) throws IOException {
    checkState();
    var tbread = totalBuffered - available();
    var t = Math.min((tbread + limit), totalReadLimit);
    if(t < 0) t = totalReadLimit; // overflow due to huge limit
    if(activeView == null) activeView = createEmptyView();
    var vLim = t <= totalBuffered ? (int)(lim - totalBuffered + t) : lim;
    activeView.reinitView(pos, vLim, t - tbread, skipRemaining, parentTotalOffset + tbread, bigEndian);
    activeViewInitialBuffered = vLim - pos;
    state = STATE_ACTIVE_VIEW;
    totalBuffered -= activeViewInitialBuffered;
    pos = lim;
    return activeView;
  }

  /// Same as `limitedView(limit, false)`.
  /// @see #limitedView(long, boolean)
  public BufferedInput limitedView(long limit) throws IOException { return limitedView(limit, false); }

  /// Create a vectorized or scalar [LineTokenizer] depending on JVM and hardware support.
  ///
  /// The LineTokenizer is treated like a view, i.e. no other access of this BufferedInput (except closing it) is
  /// allowed until the LineTokenizer is closed with [LineTokenizer#close(boolean)].
  ///
  /// With the default values for `eol` and `preEol` a LineTokenizer will recognize both LF (Unix) and CRLF (Windows)
  /// line endings. Automatic recognition of pure CR line endings (classic MacOS) at the same time is not supported but
  /// can be configured manually by setting `eol` to `'\r'` and `preEol` to `-1`.
  ///
  /// @param charset   Charset for decoding strings. Decoding is applied to individual lines after splitting.
  /// @param eol       End-of-line character.
  /// @param preEol    Optional character that is removed if it occurs before an EOL. Set to -1 to
  ///                  disable this feature.
  public LineTokenizer lines(Charset charset, byte eol, byte preEol) throws IOException {
    return BufferUtil.VECTOR_ENABLED
        ? LineTokenizer.vectorized(this, charset, eol, preEol)
        : LineTokenizer.scalar(this, charset, eol, preEol);
  }

  /// Same as `lines(charset, (byte)'\n', (byte)'\r')`.
  /// @see #lines(Charset, byte, byte)
  public LineTokenizer lines(Charset charset) throws IOException { return lines(charset, (byte)'\n', (byte)'\r'); }

  /// Same as `lines(StandardCharsets.UTF_8, (byte)'\n', (byte)'\r')`.
  /// @see #lines(Charset, byte, byte)
  public LineTokenizer lines() throws IOException { return lines(StandardCharsets.UTF_8, (byte)'\n', (byte)'\r'); }

  /// Create a view that is identical to this buffer (including [#totalBytesRead()]) so that this
  /// buffer can be protected against accidental access while reading from the view.
  BufferedInput identicalView(CloseableView forView) throws IOException {
    checkState();
    if(activeView == null) activeView = createEmptyView();
    activeView.reinitView(pos, lim, totalReadLimit, false, parentTotalOffset, bigEndian);
    activeViewInitialBuffered = 0;
    state = STATE_ACTIVE_VIEW;
    pos = lim;
    closeableView = forView;
    return activeView;
  }

  void lock(CloseableView forView) throws IOException {
    checkState();
    state = STATE_ACTIVE_VIEW;
    pos = lim;
    closeableView = forView;
  }

  void unlock() {
    state = STATE_LIVE;
    closeableView = null;
  }

  /// Shift the remaining buffer data to the left and/or reallocate the buffer to make room for
  /// `count` bytes past the current `pos`.
  void shiftOrGrowBuf(int count) {
    var a = available();
    // Buffer shifts must be aligned to the vector size, otherwise VectorizedLineTokenizer
    // performance will tank after rebuffering even when all vector reads are aligned.
    var offset = a > 0 ? pos % BufferUtil.VECTOR_LENGTH : 0;
    if(count + offset > buf.length) {
      var buflen = buf.length;
      while(buflen < count + offset) buflen *= 2;
      var buf2 = new byte[buflen];
      if(a > 0) System.arraycopy(buf, pos, buf2, offset, a);
      buf = buf2;
    } else if(a > 0 && pos != offset) {
      System.arraycopy(buf, pos, buf, offset, a);
    }
    pos = offset;
    lim = a + offset;
  }
  
  void clampToLimit() {
    if(totalBuffered > totalReadLimit) {
      excessRead = (int)(totalBuffered - totalReadLimit);
      lim -= excessRead;
      totalBuffered -= excessRead;
    }
  }
}
