package perfio;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
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
/// The number of bytes read is tracked as a 64-bit signed long value. The behaviour after
/// reading more than [Long#MAX_VALUE] (8 exabytes) is undefined.
public abstract sealed class BufferedInput implements Closeable permits HeapBufferedInput, DirectBufferedInput {

  /// Read data from an [InputStream] using the default buffer size and byte order.
  /// Same as `of(in, ByteOrder.BIG_ENDIAN, 32768)`.
  ///
  /// @param in         InputStream from which to read data.
  /// @see #of(InputStream, ByteOrder, int)
  public static BufferedInput of(InputStream in) { return of(in, ByteOrder.BIG_ENDIAN, 32768); }

  /// Read data from an {@link InputStream} using the default buffer size.
  /// Same as `of(in, order, 32768)`.
  ///
  /// @param in         InputStream from which to read data.
  /// @param order      Byte order used for reading multi-byte values. Default: Big endian.
  /// @see #of(InputStream, ByteOrder, int)
  public static BufferedInput of(InputStream in, ByteOrder order) { return of(in, order, 32768); }

  /// Read data from an [InputStream] using the default byte order.
  /// Same as `of(in, ByteOrder.BIG_ENDIAN, initialBufferSize)`.
  ///
  /// @param in                InputStream from which to read data.
  /// @param initialBufferSize Initial size of the buffer in bytes. It is automatically extended if necessary. This also
  ///                          affects the minimum block size to read from the InputStream. BufferedInput tries to read
  ///                          at least half of initialBufferSize at once.
  /// @see #of(InputStream, ByteOrder, int)
  public static BufferedInput of(InputStream in, int initialBufferSize) {
    return of(in, ByteOrder.BIG_ENDIAN, initialBufferSize);
  }

  /// Read data from an [InputStream].
  ///
  /// @param in                InputStream from which to read data.
  /// @param order             Byte order used for reading multi-byte values. Default: Big endian.
  /// @param initialBufferSize Initial size of the buffer in bytes. It is automatically extended if necessary. This also
  ///                          affects the minimum block size to read from the InputStream. BufferedInput tries to read
  ///                          at least half of initialBufferSize at once.
  public static BufferedInput of(InputStream in, ByteOrder order, int initialBufferSize) {
    var buf = new byte[Math.max(initialBufferSize, MIN_BUFFER_SIZE)];
    var bb = ByteBuffer.wrap(buf).order(order);
    return new HeapBufferedInput(buf, bb, buf.length, buf.length, Long.MAX_VALUE, in, buf.length/2, null);
  }

  /// Same as `ofArray(buf, 0, buf.length, ByteOrder.BIG_ENDIAN)`.
  /// @see #ofArray(byte[], int, int, ByteOrder)
  public static BufferedInput ofArray(byte[] buf) { return ofArray(buf, 0, buf.length, ByteOrder.BIG_ENDIAN); }

  /// Same as `ofArray(buf, 0, buf.length, order)`.
  /// @see #ofArray(byte[], int, int, ByteOrder)
  public static BufferedInput ofArray(byte[] buf, ByteOrder order) { return ofArray(buf, 0, buf.length, order); }

  /// Same as `ofArray(buf, off, len, ByteOrder.BIG_ENDIAN)`.
  /// @see #ofArray(byte[], int, int, ByteOrder)
  public static BufferedInput ofArray(byte[] buf, int off, int len) { return ofArray(buf, off, len, ByteOrder.BIG_ENDIAN); }

  /// Read data from part of a byte array with the given byte order.
  ///
  /// @param buf        Array from which to read data.
  /// @param off        Starting point within the array.
  /// @param len        Length of the data within the array, or -1 to read until the end of the array.
  /// @param order      Byte order used for reading multi-byte values. Default: Big endian.
  public static BufferedInput ofArray(byte[] buf, int off, int len, ByteOrder order) {
    Objects.checkFromIndexSize(off, len, buf.length);
    var bb = ByteBuffer.wrap(buf).order(order).position(off).limit(off+len);
    return new HeapBufferedInput(buf, bb, off, off+len, Long.MAX_VALUE, null, 0, null);
  }

  /// Read data from a [MemorySegment]. Use [MemorySegment#asSlice(long,long)] for reading only part of a segment.
  ///
  /// @param ms         Segment from which to read.
  /// @param closeable  An optional [Closeable] object to close when the BufferedInput is closed, otherwise null.
  ///                   This can be used to deallocate a segment which is not managed by the garbage-collector.
  /// @param order      Byte order used for reading multi-byte values.
  public static BufferedInput ofMemorySegment(MemorySegment ms, Closeable closeable, ByteOrder order) {
    var len = ms.byteSize();
    return len > MaxDirectBufferSize
        ? create(ms.asSlice(0, MaxDirectBufferSize), ms, closeable, order)
        : create(ms, ms, closeable, order);
  }

  /// Same as `ofMemorySegment(ms, null, ByteOrder.BIG_ENDIAN)`.
  /// @see #ofMemorySegment(MemorySegment, Closeable, ByteOrder)
  public static BufferedInput ofMemorySegment(MemorySegment ms) { return ofMemorySegment(ms, null, ByteOrder.BIG_ENDIAN); }

  /// Read data from a [ByteBuffer]. The BufferedInput will start at the current position and end at the current
  /// limit of the ByteBuffer. The initial byte order is also taken from the ByteBuffer. The ByteBuffer's position
  /// may be temporarily modified during initialization so it should not be used concurrently. After this method
  /// returns the ByteBuffer is treated as read-only and never modified (including its position, limit and byte order).
  public static BufferedInput ofByteBuffer(ByteBuffer bb) {
    if(bb.isDirect()) {
      var p = bb.position();
      bb.position(0);
      var ms = MemorySegment.ofBuffer(bb);
      bb.position(p);
      return new DirectBufferedInput(bb, ms, p, bb.limit(), bb.limit(), ms, null, null, newLinebuf());
    }
    else return new HeapBufferedInput(bb.array(), bb, bb.position(), bb.limit(), Long.MAX_VALUE, null, 0, null);
  }

  /// Read data from a file which is memory-mapped for efficient reading.
  ///
  /// @param file           File to read.
  /// @param order          Byte order used for reading multi-byte values.
  public static BufferedInput ofMappedFile(Path file, ByteOrder order) throws IOException {
    return ofMemorySegment(BufferUtil.mapReadOnlyFile(file), null, order);
  }

  /// Same as `ofMappedFile(file, ByteOrder.BIG_ENDIAN)`.
  /// @see #ofMappedFile(Path, ByteOrder)
  public static BufferedInput ofMappedFile(Path file) throws IOException {
    return ofMappedFile(file, ByteOrder.BIG_ENDIAN);
  }

  private static byte[][] newLinebuf() { return new byte[][]{ new byte[1024] }; }

  private static DirectBufferedInput create(MemorySegment bbSegment, MemorySegment ms, Closeable closeable, ByteOrder order) {
    var bb = bbSegment.asByteBuffer().order(order);
    return new DirectBufferedInput(bb, bbSegment, bb.position(), bb.limit(), ms.byteSize(), ms, closeable, null, newLinebuf());
  }

  private static final int MIN_BUFFER_SIZE = BufferUtil.VECTOR_LENGTH * 2;
  static int MaxDirectBufferSize = Integer.MAX_VALUE-15; //modified by unit tests

  private static final int STATE_LIVE        = 0;
  private static final int STATE_CLOSED      = 1;
  private static final int STATE_ACTIVE_VIEW = 2;


  // ======================================================= non-static parts:

  ByteBuffer bb; // always available
  int pos; // first used byte in buf/bb
  int lim; // last used byte + 1 in buf/bb
  long totalReadLimit; // max number of bytes that may be returned
  private final Closeable closeable;
  private final BufferedInput parent;

  BufferedInput(ByteBuffer bb, int pos, int lim, long totalReadLimit, Closeable closeable, BufferedInput parent) {
    this.bb = bb;
    this.pos = pos;
    this.lim = lim;
    this.totalReadLimit = totalReadLimit;
    this.closeable = closeable;
    this.parent = parent;

    this.totalBuffered = lim - pos;
    this.bigEndian = bb != null && bb.order() == ByteOrder.BIG_ENDIAN;
  }

  long totalBuffered; // total number of bytes read from input
  boolean bigEndian;
  int excessRead = 0; // number of bytes read into buf beyond lim if totalReadLimit was reached
  private int state = STATE_LIVE;
  private BufferedInput activeView = null;
  private int activeViewInitialBuffered = 0;
  private boolean detachOnClose, skipOnClose = false;
  long parentTotalOffset = 0L;
  CloseableView closeableView= null;

  abstract BufferedInput createEmptyView();
  abstract void clearBuffer();
  abstract void copyBufferFrom(BufferedInput b);
  abstract void prepareAndFillBuffer(int count) throws IOException;

  private void reinitView(ByteBuffer bb, int pos, int lim, long totalReadLimit, boolean skipOnClose, long parentTotalOffset) {
    this.bb = bb;
    this.pos = pos;
    this.lim = lim;
    this.totalReadLimit = totalReadLimit;
    this.skipOnClose = skipOnClose;
    this.state = STATE_LIVE;
    this.excessRead = 0;
    this.totalBuffered = lim-pos;
    this.parentTotalOffset = parentTotalOffset;
    bigEndian = bb.order() == ByteOrder.BIG_ENDIAN;
    copyBufferFrom(parent);
  }

  int available() { return lim - pos; }

  void checkState() throws IOException {
    if(state != STATE_LIVE) {
      if(state == STATE_CLOSED) throw new IOException("BufferedInput has already been closed");
      else if(state == STATE_ACTIVE_VIEW) throw new IOException("Cannot use BufferedInput while a view is active");
    }
  }

  /// Change the byte order of this BufferedInput.
  /// @return this BufferedInput
  public BufferedInput order(ByteOrder order) {
    bb.order(order);
    bigEndian = order == ByteOrder.BIG_ENDIAN;
    return this;
  }

  /// Request `count` bytes to be available to read in the buffer. Less may be available if the end of the input
  /// is reached. This method may change the `buf` and `bb` references when requesting more than
  /// [#MIN_BUFFER_SIZE] / 2 bytes.
  void request(int count) throws IOException { if(available() < count) prepareAndFillBuffer(count); }

  /// Request `count` bytes to be available to read in the buffer, advance the buffer to the position after these
  /// bytes and return the previous position. Throws EOFException if the end of the input is reached before the
  /// requested number of bytes is available. This method may change the buffer references.
  int fwd(int count) throws IOException {
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

  public abstract byte int8() throws IOException;

  public final int uint8() throws IOException { return int8() & 0xFF; }

  public abstract short int16() throws IOException;

  public abstract char uint16() throws IOException;

  public abstract int int32() throws IOException;

  public final long uint32() throws IOException { return int32() & 0xFFFFFFFFL; }

  public abstract long int64() throws IOException;

  public abstract float float32() throws IOException;

  public abstract double float64() throws IOException;

  /// Close this BufferedInput and any open views based on it. Calling any other method after closing will result in an
  /// IOException. If this object is a view of another BufferedInput, this operation transfers control back to the
  /// parent, otherwise it closes the underlying InputStream or other input source.
  public void close() throws IOException {
    if(state != STATE_CLOSED) {
      if(activeView != null) activeView.markClosed();
      if(parent != null) {
        if(skipOnClose) skip(Long.MAX_VALUE);
        parent.closedView(bb, pos, lim + excessRead, totalBuffered + excessRead, detachOnClose);
      }
      markClosed();
      if(parent == null && closeable != null) closeable.close();
    }
  }

  private void markClosed() {
    pos = lim;
    state = STATE_CLOSED;
    bb = null;
    clearBuffer();
    if(closeableView != null) closeableView.markClosed();
  }

  private void closedView(ByteBuffer vbb, int vpos, int vlim, long vTotalBuffered, boolean vDetach) {
    if(vTotalBuffered == activeViewInitialBuffered) { // view has not advanced the buffer
      pos = vpos;
      totalBuffered += vTotalBuffered;
    } else {
      copyBufferFrom(activeView);
      bb = vbb;
      pos = vpos;
      lim = vlim;
      totalBuffered += vTotalBuffered;
      if(totalBuffered >= totalReadLimit) {
        excessRead = (int)(totalBuffered-totalReadLimit);
        lim -= excessRead;
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
  /// BufferedInput are reused. Calling delimitedView() again will return the same object reinitialized to the new state
  /// unless the view was previously detached.
  ///
  /// @param limit         Maximum number of bytes in the view.
  /// @param skipRemaining Skip over the remaining bytes up to the limit in this BufferedInput if the view is closed
  ///                      without reading it fully.
  public BufferedInput delimitedView(long limit, boolean skipRemaining) throws IOException {
    checkState();
    var tbread = totalBuffered - available();
    var t = Math.min((tbread + limit), totalReadLimit);
    if(t < 0) t = totalReadLimit; // overflow due to huge limit
    if(activeView == null) activeView = createEmptyView();
    var vLim = t <= totalBuffered ? (int)(lim - totalBuffered + t) : lim;
    activeView.reinitView(bb, pos, vLim, t - tbread, skipRemaining, parentTotalOffset + tbread);
    activeViewInitialBuffered = vLim - pos;
    state = STATE_ACTIVE_VIEW;
    totalBuffered -= activeViewInitialBuffered;
    pos = lim;
    return activeView;
  }

  /// Same as `delimitedView(limit, false)`.
  /// @see #delimitedView(long, boolean)
  public BufferedInput delimitedView(long limit) throws IOException { return delimitedView(limit, false); }

  /// Create a vectorized or scalar [LineTokenizer] depending on JVM and hardware support.
  ///
  /// The LineTokenizer is treated like a view, i.e. no other access of this BufferedReader (except closing it) is
  /// allowed until the LineTokenizer is closed with [#close()] (thus closing this BufferedInput as well)
  /// or [#end()] (to keep this BufferedInput open for further reading).
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
    activeView.reinitView(bb, pos, lim, totalReadLimit, false, parentTotalOffset);
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
