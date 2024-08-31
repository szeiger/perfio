package perfio

import java.io.{Closeable, EOFException, IOException, InputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.{ByteBuffer, ByteOrder}
import scala.annotation.tailrec

object BufferedInput {
  def apply(in: InputStream, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedInput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    val bb = createByteBuffer(buf, byteOrder)
    new BufferedInput(buf, bb, buf.length, buf.length, Long.MaxValue, in, buf.length/2, null)
  }

  def fromArray(buf: Array[Byte], byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): BufferedInput = {
    val bb = createByteBuffer(buf, byteOrder)
    new BufferedInput(buf, bb, 0, buf.length, Long.MaxValue, null, 0, null)
  }

  private[this] val MinBufferSize = 16

  @inline private def createByteBuffer(buf: Array[Byte], byteOrder: ByteOrder): ByteBuffer = {
    val bb = ByteBuffer.wrap(buf)
    bb.order(byteOrder)
    bb
  }

  private val STATE_LIVE = 0
  private val STATE_CLOSED = 1
  private val STATE_ACTIVE_VIEW = 2
}

/** BufferedInput provides buffered streaming reads from an InputStream or similar input source.
 *
 * The API is not thread-safe. Access from multiple threads must be synchronized externally.
 * The number of bytes read is tracked as a 64-bit signed Long value. The behaviour after
 * reading more than Long.MaxValue (8 exabytes) is undefined.
 */
class BufferedInput private (
  private var buf: Array[Byte],
  // We have to use a ByteBuffer to get efficient implementations of methods like int32() without using
  // jdk.internal.misc.Unsafe directly.
  private[this] var bb: ByteBuffer,
  private[this] var pos: Int, // first used byte in buf
  private[this] var lim: Int, // last used byte + 1 in buf
  private[this] var totalReadLimit: Long, // max number of bytes that may be returned
  in: InputStream,
  minRead: Int,
  parent: BufferedInput
) extends Closeable { self =>
  import BufferedInput._

  private[this] var totalBuffered = (lim-pos).toLong // total number of bytes read from input
  private[this] var excessRead = 0 // number of bytes read into buf beyond lim if totalReadLimit was reached
  private[this] var state = STATE_LIVE
  private[this] var activeView: BufferedInput = null
  private[this] var activeViewInitialBuffered = 0
  private[this] var detachOnClose, skipOnClose = false

  private def reinitView(buf: Array[Byte], bb: ByteBuffer, pos: Int, lim: Int, totalReadLimit: Long, skipOnClose: Boolean): Unit = {
    this.buf = buf
    this.bb = bb
    this.pos = pos
    this.lim = lim
    this.totalReadLimit = totalReadLimit
    this.skipOnClose = skipOnClose
    this.state = STATE_LIVE
    this.excessRead = 0
    this.totalBuffered = lim-pos
  }

  private[this] def available: Int = lim - pos

  private[this] def checkState(): Unit = {
    if(state != STATE_LIVE) {
      if(state == STATE_CLOSED) throwClosed
      else if(state == STATE_ACTIVE_VIEW) throwActiveView
    }
  }

  @inline private[this] def throwActiveView: Nothing = throw new IOException("Cannot use BufferedInput while a view is active")

  @inline private[this] def throwClosed: Nothing = throw new IOException("BufferedInput has already been closed")

  @inline private[this] def throwFormatError(msg: String): Nothing = throw new IOException(msg)

  private[this] def fillBuffer(): Int = {
    val read = in.read(buf, lim, buf.length - lim)
    if(read > 0) {
      totalBuffered += read
      lim += read
      if(totalBuffered >= totalReadLimit) {
        excessRead = (totalBuffered-totalReadLimit).toInt
        lim -= excessRead
        totalBuffered -= excessRead
      }
    }
    read
  }

  private[this] def prepareAndFillBuffer(count: Int): Unit = {
    checkState()
    if(in != null && totalBuffered < totalReadLimit) {
      if(pos + count > buf.length || pos >= buf.length-minRead) {
        val a = available
        if(count > buf.length) {
          var buflen = buf.length
          while(buflen < count) buflen *= 2
          val buf2 = new Array[Byte](buflen)
          if(a > 0) System.arraycopy(buf, pos, buf2, 0, a)
          buf = buf2
          bb = createByteBuffer(buf, bb.order())
        } else if (a > 0 && pos > 0) {
          System.arraycopy(buf, pos, buf, 0, a)
        }
        pos = 0
        lim = a
      }
      while(fillBuffer() >= 0 && available < count) {}
    }
  }

  /** Request `count` bytes to be available to read in the buffer. Less may be available if the end of the input
   * is reached. This method may change the `buf` and `bb` references when requesting more than
   * [[BufferedInput!.MinBufferSize]] / 2 bytes. */
  private[this] def request(count: Int): Unit =
    if(available < count) prepareAndFillBuffer(count)

  /** Request `count` bytes to be available to read in the buffer, advance the buffer to the position after these
   * bytes and return the previous position. Throws EOFException if the end of the input is reached before the
   * requested number of bytes is available. This method may change the `buf` and `bb` references when requesting more
   * than [[BufferedInput!.MinBufferSize]] / 2 bytes. */
  private[this] def fwd(count: Int): Int = {
    if(available < count) {
      prepareAndFillBuffer(count)
      if(available < count) throw new EOFException()
    }
    val p = pos
    pos += count
    p
  }

  /** The total number of bytes read from this BufferedInput, including child views but excluding the parent. */
  def totalBytesRead: Long = {
    checkState()
    totalBuffered - available
  }

//  def readFully(a: Array[Byte]): Unit = {
//    var copied = a.length min available
//    if(copied > 0) {
//      System.arraycopy(buf, pos, a, 0, copied)
//      pos += copied
//    }
//    var rem = a.length - copied
//    while(rem > 0) {
//      if(rem > bufReadLimit) {
//        val r = in.read(a, copied, rem)
//        if(r <= 0) throw new EOFException()
//        totalBuffered += r
//        rem -= r
//        copied += r
//      } else {
//        ensure(rem)
//        System.arraycopy(buf, pos, a, copied, rem)
//        pos += rem
//        return
//      }
//    }
//  }

  /** Skip over n bytes, or until the end of the input if it occurs first.
   *
   * @return The number of skipped bytes. It is equal to the requested number unless the end of the input was reached.
   */
  def skip(bytes: Long): Long = {
    checkState()
    skip(bytes, 0L)
  }

  @tailrec private[this] def skip(bytes: Long, base: Long): Long = {
    if(bytes > 0) {
      val skipAv = (bytes min available).toInt
      pos += skipAv
      var rem = bytes - skipAv
      if(rem > 0 && in != null) {
        val remTotal = totalReadLimit - (totalBuffered - available)
        rem = rem min remTotal
        rem -= trySkipIn(rem) //TODO have a minSkip similar to minRead?
        if(rem > 0) {
          request((rem min buf.length).toInt)
          if(available > 0) skip(rem, base + bytes - rem)
          else base + bytes - rem
        } else base + bytes
      } else base + bytes
    } else base
  }

  // repeatedly try in.skip(); may return early even when more data is available
  private[this] def trySkipIn(b: Long): Long = {
    var skipped = 0L
    while(skipped < b) {
      val l = in.skip(b-skipped)
      if(l <= 0) return skipped
      totalBuffered += l
      skipped += l
    }
    skipped
  }

//  def readByteArray(len: Int): Array[Byte] = {
//    val a = new Array[Byte](len)
//    readFully(a)
//    a
//  }

  /** Read a non-terminated string of the specified encoded length. */
  def string(len: Int, charset: Charset = StandardCharsets.UTF_8): String =
    if(len == 0) {
      checkState()
      ""
    } else {
      val p = fwd(len) // may update buf so it must be evaluated first
      new String(buf, p, len, charset)
    }

  /** Read a \0-terminated string of the specified encoded length (including the \0). */
  def zstring(len: Int, charset: Charset = StandardCharsets.UTF_8): String =
    if(len == 0) {
      checkState()
      ""
    } else {
      val p = fwd(len) // may update buf so it must be evaluated first
      if(buf(pos-1) != 0) throwFormatError("Missing \\0 terminator in string")
      if(len == 1) "" else new String(buf, p, len-1, charset)
    }

  def hasMore: Boolean =
    (pos < lim) || {
      prepareAndFillBuffer(1)
      pos < lim
    }

  def int8(): Byte = buf(fwd(1))

  def uint8(): Int = int8() & 0xFF

  def int16(): Short = bb.getShort(fwd(2))

  def uint16(): Char = bb.getChar(fwd(2))

  def int32(): Int = bb.getInt(fwd(4))

  def uint32(): Long = int32() & 0xFFFFFFFFL

  def int64(): Long = bb.getLong(fwd(8))

  def float32(): Float = bb.getFloat(fwd(4))

  def float64(): Double = bb.getDouble(fwd(8))

  /** Close this BufferedInput and any open views based on it. Calling any other method after closing will result in an
   * IOException. If this object is a view of another BufferedInput, this operation transfers control back to the
   * parent, otherwise it closes the underlying InputStream or other input source. */
  def close(): Unit = {
    if(state != STATE_CLOSED) {
      if(activeView != null) activeView.markClosed()
      if(parent != null) {
        if(skipOnClose) skip(Long.MaxValue)
        parent.closedView(buf, bb, pos, lim + excessRead, totalBuffered + excessRead, detachOnClose)
      }
      markClosed()
      if(parent == null && in != null) in.close()
    }
  }

  private def markClosed(): Unit = {
    pos = lim
    state = STATE_CLOSED
    buf = null
    bb = null
  }

  private def closedView(vbuf: Array[Byte], vbb: ByteBuffer, vpos: Int, vlim: Int, vTotalBuffered: Long, vDetach: Boolean): Unit = {
    if(vTotalBuffered == activeViewInitialBuffered) { // view has not advanced the buffer
      pos = vpos
      totalBuffered += vTotalBuffered
    } else {
      buf = vbuf
      bb = vbb
      pos = vpos
      lim = vlim
      totalBuffered += vTotalBuffered
      if(totalBuffered >= totalReadLimit) {
        excessRead = (totalBuffered-totalReadLimit).toInt
        lim -= excessRead
      }
    }
    if(vDetach) activeView = null
    state = STATE_LIVE
  }

  /** Prevent reuse of this BufferedInput if it is a view. This ensures that this BufferedInput stays closed when
   * a new view is created from the parent. */
  def detach(): BufferedInput = {
    checkState()
    detachOnClose = true
    this
  }

  /** Create a BufferedInput as a view that starts at the current position and is limited to reading the specified
   * number of bytes. It will report EOF when the limit is reached or this BufferedInput reaches EOF. Views can be
   * nested to arbitrary depths with no performance overhead or double buffering.
   *
   * This BufferedInput cannot be used until the view is closed. Calling close() on this BufferedInput is allowed and
   * will also close the view. Any other method throws an IOException until the view is closed. Views of the same
   * BufferedInput are reused. Calling delimitedView() again will return the same object reinitialized to the new state
   * unless the view was previously detached.
   *
   * @param limit         Maximum number of bytes in the view.
   * @param skipRemaining Skip over the remaining bytes up to the limit in this BufferedInput if the view is closed
   *                      without reading it fully.
   */
  def delimitedView(limit: Long, skipRemaining: Boolean = false): BufferedInput = {
    checkState()
    val tbread = totalBuffered - available
    var t = (tbread + limit) min totalReadLimit
    if(t < 0) t = totalReadLimit // overflow due to huge limit
    if(activeView == null) activeView = new BufferedInput(null, null, 0, 0, 0, in, minRead, this)
    val vLim = if(t <= totalBuffered) (lim - totalBuffered + t).toInt else lim
    activeView.reinitView(buf, bb, pos, vLim, t - tbread, skipRemaining)
    activeViewInitialBuffered = vLim - pos
    state = STATE_ACTIVE_VIEW
    totalBuffered -= activeViewInitialBuffered
    pos = lim
    activeView
  }
}
