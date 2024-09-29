package perfio

import java.io.{EOFException, IOException, OutputStream}
import java.lang.invoke.{MethodHandles, VarHandle}
import java.nio.{ByteBuffer, ByteOrder}
import java.util.Arrays
import scala.reflect.ClassTag

object BufferedOutput {
  def apply(out: OutputStream, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    new BufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, out, initialBufferSize, false, Long.MaxValue)
  }

  def growing(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): FullyBufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    new FullyBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, initialBufferSize)
  }

  //TODO make this work with block merging or remove the method?
  def fixed(buf: Array[Byte], start: Int = 0, len: Int = -1, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedOutput = {
    new BufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, start, start, if(len == -1) buf.length else len+start, null, initialBufferSize, true, Long.MaxValue)
  }

  private[this] val MinBufferSize = 16

  private val LONG_BIG = vh[Long](true)
  private val INT_BIG = vh[Int](true)
  private val SHORT_BIG = vh[Short](true)
  private val CHAR_BIG = vh[Char](true)
  private val DOUBLE_BIG = vh[Double](true)
  private val FLOAT_BIG = vh[Float](true)

  private val LONG_LITTLE = vh[Long](false)
  private val INT_LITTLE = vh[Int](false)
  private val SHORT_LITTLE = vh[Short](false)
  private val CHAR_LITTLE = vh[Char](false)
  private val DOUBLE_LITTLE = vh[Double](false)
  private val FLOAT_LITTLE = vh[Float](false)

  @inline private[this] def vh[T](@inline be: Boolean)(implicit @inline ct: ClassTag[Array[T]]) =
    MethodHandles.byteArrayViewVarHandle(ct.runtimeClass, if(be) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

  private val SHARING_EXCLUSIVE = 0.toByte // buffer is not shared
  private val SHARING_LEFT = 1.toByte      // buffer is shared with next block
  private val SHARING_RIGHT = 2.toByte     // buffer is shared with previous blocks only
}

class BufferedOutput(
  protected var buf: Array[Byte],
  private[this] var bigEndian: Boolean,
  protected var start: Int, // first used byte in buf
  protected var pos: Int,
  protected var lim: Int,
  out: OutputStream,
  initialBufferSize: Int,
  private var fixed: Boolean, // buffer cannot be reallocated or grown beyond lim
  private var totalLimit: Long
) extends AutoCloseable {
  import BufferedOutput.{SHARING_EXCLUSIVE, SHARING_LEFT, SHARING_RIGHT}

  private var totalFlushed = 0L
  private var next, prev: BufferedOutput = this // prefix list as a double-linked ring
  private[this] var cachedExclusive, cachedShared: BufferedOutput = null // single-linked lists (via `next`) of blocks cached for reuse
  private var closed = false
  private var root: BufferedOutput = this
  private var sharing: Byte = SHARING_EXCLUSIVE

  /** Re-initialize this block and return its old buffer. */
  private def reinit(buf: Array[Byte], bigEndian: Boolean, start: Int, pos: Int, lim: Int, sharing: Byte, totalLimit: Long, totalFlushed: Long): Array[Byte] = {
    val b = this.buf
    this.buf = buf
    this.bigEndian = bigEndian
    this.start = start
    this.pos = pos
    this.lim = lim
    this.sharing = sharing
    this.totalLimit = totalLimit
    this.totalFlushed = totalFlushed
    closed = false
    b
  }

  /** Change the byte order of this BufferedOutput. */
  def order(order: ByteOrder): this.type = {
    bigEndian = order == ByteOrder.BIG_ENDIAN
    this
  }

  private[this] def throwClosed(): Nothing = throw new IOException("BufferedOutput has already been closed")

  private[this] def throwUnderflow(len: Long, exp: Long): Nothing =
    throw new IOException(s"Can't close BufferedOutput created by reserve($exp): Only $len bytes written")

  protected[this] def checkState(): Unit = {
    if(closed) throwClosed()
  }

  @inline private[this] def available: Int = lim - pos

  def totalBytesWritten: Long = totalFlushed + (pos - start)

  private[this] def fwd(count: Int): Int = {
    if(available < count) {
      flushAndGrow(count)
      if(available < count) throw new EOFException()
    }
    val p = pos
    pos += count
    p
  }

  def int8(b: Byte): this.type = {
    val p = fwd(1)
    buf(p) = b
    this
  }

  def uint8(b: Int): this.type = int8(b.toByte)

  def int16(s: Short): this.type = {
    val p = fwd(2)
    (if(bigEndian) BufferedOutput.SHORT_BIG else BufferedOutput.SHORT_LITTLE).set(buf, p, s)
    //bb.putShort(p, s)
    this
  }

  def uint16(c: Char): this.type = {
    val p = fwd(2)
    (if(bigEndian) BufferedOutput.CHAR_BIG else BufferedOutput.CHAR_LITTLE).set(buf, p, c)
    //bb.putChar(p, c)
    this
  }

  def int32(i: Int): this.type = {
    val p = fwd(4)
    (if(bigEndian) BufferedOutput.INT_BIG else BufferedOutput.INT_LITTLE).set(buf, p, i)
    //bb.putInt(p, i)
    this
  }

  def uint32(i: Long): this.type = int32(i.toInt)

  def int64(l: Long): this.type = {
    val p = fwd(8)
    (if(bigEndian) BufferedOutput.LONG_BIG else BufferedOutput.LONG_LITTLE).set(buf, p, l)
    //bb.putLong(p, l)
    this
  }

  def float32(f: Float): this.type = {
    val p = fwd(4)
    (if(bigEndian) BufferedOutput.FLOAT_BIG else BufferedOutput.FLOAT_LITTLE).set(buf, p, f)
    //bb.putFloat(p, f)
    this
  }

  def float64(d: Double): this.type = {
    val p = fwd(8)
    (if(bigEndian) BufferedOutput.DOUBLE_BIG else BufferedOutput.DOUBLE_LITTLE).set(buf, p, d)
    //bb.putDouble(p, d)
    this
  }

  private[this] def flushAndGrow(count: Int): Unit = {
    checkState()
    if(!fixed) {
      if(out != null && (prev eq root)) {
        root.flushBlocks(true)
        if(lim < start+count) {
          if(pos == start) growBufferClear(count)
          else growBufferCopy(count)
        }
      } else growBufferCopy(count)
    }
  }

  /** Write the buffer to the output. Must only be called on the root block. */
  protected[this] def writeToOutput(buf: Array[Byte], off: Int, len: Int): Unit = {
    //println(s"Writing $len bytes")
    out.write(buf, off, len)
  }

  /** Unlink a block. Must only be called on the root block. */
  private[this] def unlink(b: BufferedOutput): Unit = {
    next = b.next
    next.prev = this
    if(b.sharing == SHARING_LEFT) {
      b.buf = null
      b.next = cachedShared
      cachedShared = b
    } else {
      b.next = cachedExclusive
      cachedExclusive = b
    }
  }

  /** Flush and unlink all closed blocks and optionally flush the root block. Returns true if all
   *  prefix blocks were flushed. Must only be called on the root block and only if out != null. */
  private def flushBlocks(forceFlush: Boolean): Boolean = {
    while(next ne this) {
      val b = next
      val blen = b.pos - b.start
      if(!b.closed) {
        if((forceFlush && blen > 0) || blen > initialBufferSize/2) {
          writeToOutput(b.buf, b.start, blen)
          b.totalFlushed += blen
          if(b.sharing == SHARING_LEFT) b.start = b.pos
          else {
            b.pos = 0
            b.start = 0
            b.lim -= blen
          }
        }
        return false
      }
      if(b.sharing == SHARING_LEFT && blen < initialBufferSize) {
        if(b.pos != b.lim) throwUnderflow(b.pos-b.start, b.lim-b.start)
        val ns = b.nextShared
        //if(ns.sharing == SHARING_RIGHT) ns.sharing = SHARING_EXCLUSIVE
        ns.start = b.start
        ns.totalFlushed -= blen
      } else {
        val n = b.next
        if(blen < initialBufferSize/2 && b.sharing != SHARING_LEFT && n.sharing != SHARING_LEFT && !n.fixed && b.mergeToRight) ()
        else {
          if(blen > 0) writeToOutput(b.buf, b.start, blen)
          //val ns = b.nextShared
          //if(ns.sharing == SHARING_RIGHT) ns.sharing = SHARING_EXCLUSIVE
        }
      }
      unlink(b)
    }
    val len = pos-start
    if((forceFlush && len > 0) || len > initialBufferSize/2) {
      writeToOutput(buf, start, len)
      totalFlushed += len
      pos = start
    }
    true
  }

  private def nextShared: BufferedOutput = {
    val a = buf
    var b = next
    while(b.buf ne a) b = b.next
    b
  }

  private def prevShared: BufferedOutput = {
    val a = this.buf
    val r = root
    var b = prev
    while(true) {
      if(b.buf eq a) return b
      if(b eq r) return null
      b = b.next
    }
    null
  }

  /** Merge the contents of this block into the next one. Returns false if there was not enough space to merge. */
  private def mergeToRight(): Boolean = {
    val n = next
    val nlen = n.pos - n.start
    //println(s"Trying to merge ${this.show} into ${n.show}")
    if(pos + nlen < initialBufferSize) {
      //println(s"Merging ${this.show} into ${n.show}")
      System.arraycopy(n.buf, n.start, buf, pos, nlen)
      val len = pos - start
      n.totalFlushed -= len
      val tmpbuf = buf
      buf = n.buf
      n.buf = tmpbuf
      n.start = start
      n.lim = tmpbuf.length
      n.pos = pos + nlen

      //println(s"Merged ${this.show} into ${n.show}")
      true
    }
    else false
  }

  private def show: String =
    s"[buf=$buf, start=$start, pos=$pos, lim=$lim, closed=$closed, sharing=$sharing, fixed=$fixed, totalFlushed=$totalFlushed, totalLimit=$totalLimit]"

  /** Switch a potentially shared block to exclusive after re-allocating its buffer */
  private[this] def unshare(): Unit = {
    assert(sharing != SHARING_LEFT)
    if(sharing == SHARING_RIGHT) {
      sharing = SHARING_EXCLUSIVE
      val ps = prevShared
      if(ps != null) ps.sharing = SHARING_RIGHT
    }
  }

  private[this] def growBufferClear(count: Int): Unit = {
    val tlim = ((totalLimit - totalFlushed) min count).toInt
    val buflen = BufferUtil.growBuffer(buf.length, tlim, 1)
    //println(s"$show.growBufferClear($count): $lim, $buflen")
    if(buflen > buf.length) buf = new Array[Byte](buflen)
    lim = tlim min buf.length
    pos = 0
    start = 0
    unshare()
  }

  private[this] def growBufferCopy(count: Int): Unit = {
    val tlim = ((totalLimit - totalFlushed) min (pos + count)).toInt
    val buflen = BufferUtil.growBuffer(buf.length, tlim, 1)
    //println(s"$show.growBufferCopy($count): $buflen")
    if(buflen > buf.length) buf = Arrays.copyOf(buf, buflen)
    lim = tlim min buf.length
    unshare()
  }

  /** Insert a block directly before this block in the prefix list. */
  private[this] def insertPrefix(b: BufferedOutput): Unit = {
    b.prev = prev
    b.next = this
    prev.next = b
    prev = b
  }

  def flush(): Unit = {
    checkState()
    if(out != null && (prev eq root)) root.flushBlocks(true)
  }

  def close(): Unit = {
    checkState()
    closed = true
    lim = pos
    if(root ne this) {
      if((prev eq root) && out != null) root.flushBlocks(false)
    } else {
      var b = next
      while(b ne this) {
        b.closed = true
        b = b.next
      }
      if(out != null) flushBlocks(true)
    }
  }

  /** Get a cached or new exclusive block. Must only be called on the root block. */
  private def getExclusiveBlock(): BufferedOutput = {
    if(cachedExclusive == null) {
      val b = new BufferedOutput(new Array[Byte](initialBufferSize), bigEndian, 0, 0, 0, out, initialBufferSize, false, Long.MaxValue)
      b.root = this
      b
    } else {
      val b = cachedExclusive
      cachedExclusive = b.next
      b
    }
  }

  /** Get a cached or new shared block. Must only be called on the root block. */
  private def getSharedBlock(): BufferedOutput = {
    if(cachedShared == null) {
      val b = new BufferedOutput(null, bigEndian, 0, 0, 0, out, initialBufferSize, false, Long.MaxValue)
      b.root = this
      b.fixed = true
      b
    } else {
      val b = cachedShared
      cachedShared = b.next
      b
    }
  }

  private[this] def deferShort(length: Int): BufferedOutput = {
    val p = fwd(length)
    val len = p - start
    val b = root.getSharedBlock()
    b.reinit(buf, bigEndian, start, p, pos, SHARING_LEFT, Long.MaxValue, -len)
    insertPrefix(b)
    totalFlushed += (pos - start)
    start = pos
    if(sharing == SHARING_EXCLUSIVE) sharing = SHARING_RIGHT
    b
  }

  private[this] def deferLong(max: Long, deferred: Boolean): BufferedOutput = {
    val len = pos - start
    val b = root.getExclusiveBlock()
    var l = lim
    if(l - pos > max) l = (max + pos).toInt
    buf = b.reinit(buf, bigEndian, start, pos, l, SHARING_EXCLUSIVE, max-len, -len)
    //println(s"after reinit for max=$max, lim=$lim, l=$l: ${b.show}")
    insertPrefix(b)
    totalFlushed += len
    if(!deferred) totalFlushed += max
    pos = 0
    lim = buf.size
    b
  }

  /** Leave a fixed-size gap at the current position that can be filled later after continuing to write
   * to this BufferedOutput. This BufferdOutput's `totalBytesWritten` is immediately increased by the requested
   * size. Attempting to write more than the requested amount of data to the returned BufferedOutput or closing
   * it before writing all of the data throws an IOException. */
  def reserve(length: Long): BufferedOutput = {
    checkState()
    if(fixed) deferShort((length min available).toInt)
    else if(length < initialBufferSize) deferShort(length.toInt)
    else deferLong(length, false)
  }

  /** Create a hole at the current position that can be filled later after continuing to write
   * to this BufferedOutput. Its size is *not* counted as part of this object's size. */
  def defer(max: Long = Long.MaxValue): BufferedOutput = {
    checkState()
    deferLong(max, true)
  }
}

class FullyBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int, _initialBufferSize: Int)
  extends BufferedOutput(_buf, _bigEndian, _start, _pos, _lim, null, _initialBufferSize, false, Long.MaxValue) {

  //TODO support prefix blocks

  def writeBytes(out: Array[Byte]): Unit = {
    val b = getBuffer
    System.arraycopy(b, 0, out, 0, pos)
  }
  def getBuffer = buf
  def getSize = pos
}
