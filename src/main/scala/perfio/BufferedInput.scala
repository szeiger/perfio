package perfio

import java.io.{EOFException, IOException, InputStream}
import java.lang.foreign.MemorySegment
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Path
import java.nio.{ByteBuffer, ByteOrder}
import scala.annotation.tailrec

object BufferedInput {
  def apply(in: InputStream, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedInput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    val bb = createByteBuffer(buf, byteOrder)
    new HeapBufferedInput(buf, bb, buf.length, buf.length, Long.MaxValue, in, buf.length/2, null)
  }

  def fromArray(buf: Array[Byte], byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): BufferedInput = {
    val bb = createByteBuffer(buf, byteOrder)
    new HeapBufferedInput(buf, bb, 0, buf.length, Long.MaxValue, null, 0, null)
  }

  def fromMemorySegment(ms: MemorySegment, closeable: AutoCloseable = null): BufferedInput = {
    val len = ms.byteSize()
    if(len > MaxDirectBufferSize) create(ms.asSlice(0, MaxDirectBufferSize).asByteBuffer(), ms.byteSize(), ms, closeable)
    else create(ms.asByteBuffer(), ms.byteSize(), null, closeable)
  }

  private[this] def create(bb: ByteBuffer, totalReadLimit: Long, ms: MemorySegment, closeable: AutoCloseable): DirectBufferedInput =
    new DirectBufferedInput(bb, bb.position(), bb.limit(), totalReadLimit, ms, closeable, null, Array(new Array[Byte](1024)))

  def fromByteBuffer(bb: ByteBuffer): BufferedInput =
    if(bb.isDirect) create(bb, Long.MaxValue, null, null)
    else new HeapBufferedInput(bb.array(), bb, bb.position(), bb.limit(), Long.MaxValue, null, 0, null)

  def fromMappedFile(file: Path): BufferedInput = fromMemorySegment(ForeignSupport.mapRO(file), null)

  private[this] val MinBufferSize = 16
  private[perfio] var MaxDirectBufferSize = Int.MaxValue-15 //modified by unit tests

  @inline private[perfio] def createByteBuffer(buf: Array[Byte], byteOrder: ByteOrder): ByteBuffer = {
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
sealed abstract class BufferedInput protected (
  protected[this] var buf: Array[Byte],
  // We have to use a ByteBuffer to get efficient implementations of methods like int32() without using
  // jdk.internal.misc.Unsafe directly.
  protected[this] var bb: ByteBuffer,
  protected[this] var pos: Int, // first used byte in buf
  protected[this] var lim: Int, // last used byte + 1 in buf
  protected[this] var totalReadLimit: Long, // max number of bytes that may be returned
  protected[this] val closeable: AutoCloseable,
  parent: BufferedInput
) extends AutoCloseable { self =>
  import BufferedInput._

  protected[this] var totalBuffered = (lim-pos).toLong // total number of bytes read from input
  protected[this] var excessRead = 0 // number of bytes read into buf beyond lim if totalReadLimit was reached
  private[this] var state = STATE_LIVE
  private[this] var activeView: BufferedInput = null
  private[this] var activeViewInitialBuffered = 0
  private[this] var detachOnClose, skipOnClose = false
  protected[this] var parentTotalOffset = 0L

  private def reinitView(buf: Array[Byte], bb: ByteBuffer, pos: Int, lim: Int, totalReadLimit: Long, skipOnClose: Boolean, parentTotalOffset: Long): Unit = {
    this.buf = buf
    this.bb = bb
    this.pos = pos
    this.lim = lim
    this.totalReadLimit = totalReadLimit
    this.skipOnClose = skipOnClose
    this.state = STATE_LIVE
    this.excessRead = 0
    this.totalBuffered = lim-pos
    this.parentTotalOffset = parentTotalOffset
  }

  protected[this] def createEmptyView(): BufferedInput

  protected[this] def available: Int = lim - pos

  protected[this] def checkState(): Unit = {
    if(state != STATE_LIVE) {
      if(state == STATE_CLOSED) throwClosed
      else if(state == STATE_ACTIVE_VIEW) throwActiveView
    }
  }

  @inline private[this] def throwActiveView: Nothing = throw new IOException("Cannot use BufferedInput while a view is active")

  @inline private[this] def throwClosed: Nothing = throw new IOException("BufferedInput has already been closed")

  @inline protected[this] final def throwFormatError(msg: String): Nothing = throw new IOException(msg)

  protected[this] def prepareAndFillBuffer(count: Int): Unit

  /** Request `count` bytes to be available to read in the buffer. Less may be available if the end of the input
   * is reached. This method may change the `buf` and `bb` references when requesting more than
   * [[BufferedInput!.MinBufferSize]] / 2 bytes. */
  protected[this] def request(count: Int): Unit =
    if(available < count) prepareAndFillBuffer(count)

  /** Request `count` bytes to be available to read in the buffer, advance the buffer to the position after these
   * bytes and return the previous position. Throws EOFException if the end of the input is reached before the
   * requested number of bytes is available. This method may change the buffer references. */
  protected[this] def fwd(count: Int): Int = {
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
  def skip(bytes: Long): Long

//  def readByteArray(len: Int): Array[Byte] = {
//    val a = new Array[Byte](len)
//    readFully(a)
//    a
//  }

  /** Read a non-terminated string of the specified encoded length. */
  def string(len: Int, charset: Charset = StandardCharsets.UTF_8): String

  /** Read a \0-terminated string of the specified encoded length (including the \0). */
  def zstring(len: Int, charset: Charset = StandardCharsets.UTF_8): String

  def hasMore: Boolean =
    (pos < lim) || {
      prepareAndFillBuffer(1)
      pos < lim
    }

  def int8(): Byte

  def uint8(): Int = int8() & 0xFF

  def int16(): Short = {
    val p = fwd(2)
    bb.getShort(p)
  }

  def uint16(): Char = {
    val p = fwd(2)
    bb.getChar(p)
  }

  def int32(): Int = {
    val p = fwd(4)
    bb.getInt(p)
  }

  def uint32(): Long = int32() & 0xFFFFFFFFL

  def int64(): Long = {
    val p = fwd(8)
    bb.getLong(p)
  }

  def float32(): Float = {
    val p = fwd(4)
    bb.getFloat(p)
  }

  def float64(): Double = {
    val p = fwd(8)
    bb.getDouble(p)
  }

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
      if(parent == null && closeable != null) closeable.close()
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
    if(activeView == null) activeView = createEmptyView()
    val vLim = if(t <= totalBuffered) (lim - totalBuffered + t).toInt else lim
    activeView.reinitView(buf, bb, pos, vLim, t - tbread, skipRemaining, parentTotalOffset + tbread)
    activeViewInitialBuffered = vLim - pos
    state = STATE_ACTIVE_VIEW
    totalBuffered -= activeViewInitialBuffered
    pos = lim
    activeView
  }
}

private class HeapBufferedInput(
  _buf: Array[Byte],
  _bb: ByteBuffer,
  _pos: Int,
  _lim: Int,
  _totalReadLimit: Long,
  in: InputStream,
  minRead: Int,
  _parent: BufferedInput
) extends BufferedInput(_buf, _bb, _pos, _lim, _totalReadLimit, in, _parent) {
  import BufferedInput._

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

  protected[this] def prepareAndFillBuffer(count: Int): Unit = {
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

  protected[this] def createEmptyView(): BufferedInput = new HeapBufferedInput(null, null, 0, 0, 0, in, minRead, this)

  def int8(): Byte = {
    val p = fwd(1)
    buf(p)
  }

  def string(len: Int, charset: Charset = StandardCharsets.UTF_8): String =
    if(len == 0) {
      checkState()
      ""
    } else {
      val p = fwd(len)
      new String(buf, p, len, charset)
    }

  def zstring(len: Int, charset: Charset = StandardCharsets.UTF_8): String =
    if(len == 0) {
      checkState()
      ""
    } else {
      val p = fwd(len)
      if(buf(pos-1) != 0) throwFormatError("Missing \\0 terminator in string")
      if(len == 1) "" else new String(buf, p, len-1, charset)
    }

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
        val remTotal = totalReadLimit - totalBuffered
        rem = rem min remTotal
        rem -= trySkipIn(rem) //TODO have a minSkip similar to minRead?
        if(rem > 0) {
          request((rem min buf.length).toInt)
          if(available > 0) skip(rem, base + bytes - rem)
          else base + bytes - rem
        } else base + bytes
      } else base + skipAv
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
}

private class DirectBufferedInput(
  _bb: ByteBuffer,
  _pos: Int,
  _lim: Int,
  _totalReadLimit: Long,
  ms: MemorySegment,
  _closeable: AutoCloseable,
  _parent: BufferedInput,
  linebuf: Array[Array[Byte]]
) extends BufferedInput(null, _bb, _pos, _lim, _totalReadLimit, _closeable, _parent) {
  import BufferedInput._

  protected[this] def prepareAndFillBuffer(count: Int): Unit = {
    checkState()
    if(ms != null && totalBuffered < totalReadLimit) {
      val a = available
      val newStart = parentTotalOffset+totalBuffered-a
      val newLen = (ms.byteSize()-newStart) min MaxDirectBufferSize
      bb = ms.asSlice(newStart, newLen).asByteBuffer().order(bb.order())
      pos = 0
      lim = newLen.toInt
      totalBuffered += newLen - a
      if(totalBuffered >= totalReadLimit) {
        excessRead = (totalBuffered-totalReadLimit).toInt
        lim -= excessRead
        totalBuffered -= excessRead
      }
    }
  }

  protected[this] def createEmptyView(): BufferedInput = new DirectBufferedInput(null, 0, 0, 0L, ms, null, this, linebuf)

  private[this] def extendBuffer(len: Int): Array[Byte] = {
    var buflen = linebuf(0).length
    while(buflen < len) buflen *= 2
    new Array[Byte](buflen)
  }

  private[this] def makeString(start: Int, len: Int, charset: Charset): String = {
    var lb = linebuf(0)
    if(lb.length < len) {
      lb = extendBuffer(len)
      linebuf(0) = lb
    }
    bb.get(start, lb, 0, len)
    new String(lb, 0, len, charset)
  }

  def int8(): Byte = {
    val p = fwd(1)
    bb.get(p)
  }

  def string(len: Int, charset: Charset = StandardCharsets.UTF_8): String =
    if(len == 0) {
      checkState()
      ""
    } else {
      val p = fwd(len)
      makeString(p, len, charset)
    }

  def zstring(len: Int, charset: Charset = StandardCharsets.UTF_8): String =
    if(len == 0) {
      checkState()
      ""
    } else {
      val p = fwd(len)
      if(bb.get(pos-1) != 0) throwFormatError("Missing \\0 terminator in string")
      if(len == 1) "" else makeString(p, len-1, charset)
    }

  def skip(bytes: Long): Long = {
    checkState()
    if(bytes > 0) {
      //println(s"skip($bytes): pos=$pos, lim=$lim, totalBuffered=$totalBuffered, parentTotalOffset=$parentTotalOffset, totalReadLimit=$totalReadLimit")
      val skipAv = (bytes min available).toInt
      pos += skipAv
      var rem = bytes - skipAv
      val remTotal = totalReadLimit - totalBuffered
      rem = rem min remTotal
      //println(s"  skipAv=$skipAv, remTotal=$remTotal, rem=$rem")
      if(rem > 0 && ms != null) {
        val newStart = parentTotalOffset+totalBuffered+rem
        val newLen = (ms.byteSize()-newStart) min MaxDirectBufferSize
        bb = ms.asSlice(newStart, newLen).asByteBuffer().order(bb.order())
        totalBuffered += rem + newLen
        pos = 0
        lim = newLen.toInt
        skipAv + rem
      } else skipAv
    } else 0
  }
}
