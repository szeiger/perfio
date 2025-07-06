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
public abstract sealed class BufferedInput implements Closeable permits HeapBufferedInput, DirectBufferedInput {

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
      return new DirectBufferedInput(bb, ms, p, bb.limit(), bb.limit(), ms, null, null, new LineBuffer());
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
    return new DirectBufferedInput(bb, bbSegment, bb.position(), bb.limit(), ms.byteSize(), ms, closeable, null, new LineBuffer());
  }

  public static final int MIN_BUFFER_SIZE = BufferUtil.VECTOR_LENGTH * 2;
  public static final int DEFAULT_BUFFER_SIZE = BufferUtil.DEFAULT_BUFFER_SIZE;

  static int MaxDirectBufferSize = Integer.MAX_VALUE-15; //modified by unit tests

  private static final int STATE_LIVE        = 0;
  private static final int STATE_CLOSED      = 1;
  private static final int STATE_ACTIVE_VIEW = 2;


  // ======================================================= non-static parts:

  int pos; // first used byte in buffer
  int lim; // last used byte + 1 in buffer
  long totalReadLimit; // max number of bytes that may be returned
  private final BufferedInput viewParent;
  final BufferedInput viewRoot;

  BufferedInput(int pos, int lim, long totalReadLimit, BufferedInput viewParent, boolean bigEndian) {
    this.pos = pos;
    this.lim = lim;
    this.totalReadLimit = totalReadLimit;
    this.viewParent = viewParent;
    this.bigEndian = bigEndian;
    this.totalBuffered = lim - pos;
    this.viewRoot = viewParent == null ? this : viewParent.viewRoot;
  }

  long totalBuffered; // total number of bytes read from input
  boolean bigEndian;
  int excessRead = 0; // number of bytes read into buf beyond lim if totalReadLimit was reached
  private int state = STATE_LIVE;
  private BufferedInput activeView = null;
  private int activeViewInitialBuffered = 0;
  private boolean detachOnClose, skipOnClose = false;
  long parentTotalOffset = 0L;
  CloseableView closeableView = null;

  abstract BufferedInput createEmptyView();
  abstract void clearBuffer();
  abstract void copyBufferFrom(BufferedInput b);

  /// Fill the buffer as much as possible without blocking (starting at [#lim]), but at least
  /// until `count` bytes are available starting at [#pos] even if this requires blocking or
  /// growing the buffer. Less data may only be made available when the end of the input has been
  /// reached or nothing more can be read without exceeding the [#totalReadLimit]. The fields
  /// [#totalBuffered] and [#lim] are updated accordingly.
  ///
  /// If [#totalBuffered] would exceed [#totalReadLimit] after filling the buffer, any excess
  /// bytes must be counted as [#excessRead] and subtracted from [#totalBuffered] and [#lim].
  ///
  /// This method may change the buffer references when requesting more than
  /// [#MIN_BUFFER_SIZE] / 2 bytes.
  abstract void prepareAndFillBuffer(int count) throws IOException;

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

  int available() { return lim - pos; }

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

  /// Request `count` bytes to be available to read in the buffer. Less may be available if the end of the input
  /// is reached. This method may change the buffer references when requesting more than
  /// [#MIN_BUFFER_SIZE] / 2 bytes.
  void request(int count) throws IOException { if(available() < count) prepareAndFillBuffer(count); }

  /// Request `count` bytes to be available to read in the buffer, advance the buffer to the position after these
  /// bytes and return the previous position. Throws EOFException if the end of the input is reached before the
  /// requested number of bytes is available. This method may change the buffer references.
  final int fwd(int count) throws IOException {
    if(available() < count) {
      prepareAndFillBuffer(count);
      if(available() < count) throw new EOFException();
    }
    var p = pos;
    pos += count;
    return p;
  }

  /// The total number of bytes read from this BufferedInput, including child views but excluding the parent.
  public long totalBytesRead() throws IOException {
    checkState();
    return totalBuffered - available();
  }

  public abstract void bytes(byte[] a, int off, int len) throws IOException;

  public byte[] bytes(int len) throws IOException {
    var a = new byte[len];
    bytes(a, 0, len);
    return a;
  }

  /// Skip over n bytes, or until the end of the input if it occurs first.
  ///
  /// @return The number of skipped bytes. It is equal to the requested number unless the end of the input was reached.
  public abstract long skip(long bytes) throws IOException;

  /// Read a non-terminated UTF-8 string.
  public final String string(int len) throws IOException { return string(len, StandardCharsets.UTF_8); }

  /// Read a non-terminated string of the specified encoded length.
  public abstract String string(int len, Charset charset) throws IOException;

  /// Read a \0-terminated UTF-8 string.
  public final String zstring(int len) throws IOException { return zstring(len, StandardCharsets.UTF_8); }

  /// Read a \0-terminated string of the specified encoded length (including the \0).
  public abstract String zstring(int len, Charset charset) throws IOException;

  public boolean hasMore() throws IOException {
    if(pos < lim) return true;
    prepareAndFillBuffer(1);
    return pos < lim;
  }

  /// Read a signed 8-bit integer (`byte`).
  public abstract byte int8() throws IOException;

  /// Read an unsigned 8-bit integer into the lower 8 bits of an `int`.
  public final int uint8() throws IOException { return int8() & 0xFF; }

  /// Read a signed 16-bit integer (`short`) in the current byte [#order(ByteOrder)].
  public abstract short int16() throws IOException;
  /// Read a signed 16-bit integer (`short`) in the native byte order.
  public abstract short int16n() throws IOException;
  /// Read a signed 16-bit integer (`short`) in big endian byte order.
  public abstract short int16b() throws IOException;
  /// Read a signed 16-bit integer (`short`) in little endian byte order.
  public abstract short int16l() throws IOException;

  /// Read an unsigned 16-bit integer (`char`) in the current byte [#order(ByteOrder)].
  public abstract char uint16() throws IOException;
  /// Read an unsigned 16-bit integer (`char`) in the native byte order.
  public abstract char uint16n() throws IOException;
  /// Read an unsigned 16-bit integer (`char`) in big endian byte order.
  public abstract char uint16b() throws IOException;
  /// Read an unsigned 16-bit integer (`char`) in little endian byte order.
  public abstract char uint16l() throws IOException;

  /// Read a signed 32-bit integer (`int`) in the current byte [#order(ByteOrder)].
  public abstract int int32() throws IOException;
  /// Read a signed 32-bit integer (`int`) in the native byte order.
  public abstract int int32n() throws IOException;
  /// Read a signed 32-bit integer (`int`) in big endian byte order.
  public abstract int int32b() throws IOException;
  /// Read a signed 32-bit integer (`int`) in little endian byte order.
  public abstract int int32l() throws IOException;

  /// Read an unsigned 32-bit integer into the lower 32 bits of a `long` in the current byte [#order(ByteOrder)].
  public final long uint32() throws IOException { return int32() & 0xFFFFFFFFL; }
  /// Read an unsigned 32-bit integer into the lower 32 bits of a `long` in the native byte order.
  public final long uint32n() throws IOException { return int32n() & 0xFFFFFFFFL; }
  /// Read an unsigned 32-bit integer into the lower 32 bits of a `long` in big endian byte order.
  public final long uint32b() throws IOException { return int32b() & 0xFFFFFFFFL; }
  /// Read an unsigned 32-bit integer into the lower 32 bits of a `long` in little endian byte order.
  public final long uint32l() throws IOException { return int32l() & 0xFFFFFFFFL; }

  /// Read a signed 64-bit integer (`long`) in the current byte [#order(ByteOrder)].
  public abstract long int64() throws IOException;
  /// Read a signed 64-bit integer (`long`) in the native byte order.
  public abstract long int64n() throws IOException;
  /// Read a signed 64-bit integer (`long`) in big endian byte order.
  public abstract long int64b() throws IOException;
  /// Read a signed 64-bit integer (`long`) in little endian byte order.
  public abstract long int64l() throws IOException;

  /// Read a 32-bit IEEE-754 floating point value (`float`) in the current byte [#order(ByteOrder)].
  public abstract float float32() throws IOException;
  /// Read a 32-bit IEEE-754 floating point value (`float`) in the native byte order.
  public abstract float float32n() throws IOException;
  /// Read a 32-bit IEEE-754 floating point value (`float`) in big endian byte order.
  public abstract float float32b() throws IOException;
  /// Read a 32-bit IEEE-754 floating point value (`float`) in little endian byte order.
  public abstract float float32l() throws IOException;

  /// Read a 64-bit IEEE-754 floating point value (`double`) in the current byte [#order(ByteOrder)].
  public abstract double float64() throws IOException;
  /// Read a 64-bit IEEE-754 floating point value (`double`) in the native byte order.
  public abstract double float64n() throws IOException;
  /// Read a 64-bit IEEE-754 floating point value (`double`) in big endian byte order.
  public abstract double float64b() throws IOException;
  /// Read a 64-bit IEEE-754 floating point value (`double`) in little endian byte order.
  public abstract double float64l() throws IOException;

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
  void bufferClosed(boolean closeUpstream) throws IOException {}
  
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
      if(totalBuffered >= totalReadLimit) {
        excessRead = (int)(totalBuffered-totalReadLimit);
        lim -= excessRead;
        totalBuffered -= excessRead;
      }
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
        ? VectorizedLineTokenizer.of(this, charset, eol, preEol)
        : ScalarLineTokenizer.of(this, charset, eol, preEol);
  }

  /// Same as `lines(charset, (byte)'\n', (byte)'\r')`.
  /// @see #lines(Charset, byte, byte)
  public LineTokenizer lines(Charset charset) throws IOException { return lines(charset, (byte)'\n', (byte)'\r'); }

  /// Same as `lines(StandardCharsets.UTF_8, (byte)'\n', (byte)'\r')`.
  /// @see #lines(Charset, byte, byte)
  public LineTokenizer lines() throws IOException { return lines(StandardCharsets.UTF_8, (byte)'\n', (byte)'\r'); }

  /// Create a view that is identical to this buffer (including [#totalBytesRead()]) so that this
  /// buffer can be protected against accidental access while reading from the view.
  BufferedInput identicalView() throws IOException {
    checkState();
    if(activeView == null) activeView = createEmptyView();
    activeView.reinitView(pos, lim, totalReadLimit, false, parentTotalOffset, bigEndian);
    activeViewInitialBuffered = 0;
    state = STATE_ACTIVE_VIEW;
    pos = lim;
    return activeView;
  }

  void lock() throws IOException {
    checkState();
    state = STATE_ACTIVE_VIEW;
    pos = lim;
  }

  void unlock() {
    state = STATE_LIVE;
  }
}
