package perfio

import java.io.{EOFException, IOException, OutputStream}
import java.lang.invoke.{MethodHandles, VarHandle}
import java.nio.{ByteBuffer, ByteOrder}
import java.util.Arrays
import scala.reflect.ClassTag

object BufferedOutput {
  def apply(out: OutputStream, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    new BufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, out, initialBufferSize, false)
  }

  def growing(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): FullyBufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    new FullyBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, initialBufferSize)
  }

  //TODO make this work with block merging or remove the method?
  def fixed(buf: Array[Byte], start: Int = 0, len: Int = -1, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedOutput = {
    new BufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, start, start, if(len == -1) buf.length else len+start, null, initialBufferSize, true)
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
  private var fixed: Boolean
) extends AutoCloseable {
  import BufferedOutput.{SHARING_EXCLUSIVE, SHARING_LEFT, SHARING_RIGHT}

  private[this] var totalFlushed = 0L
  private var next, prev: BufferedOutput = this // prefix list as a double-linked ring
  private[this] var cachedExclusive, cachedShared: BufferedOutput = null // single-linked lists (via `next`) of blocks cached for reuse
  private var closed = false
  private var root: BufferedOutput = this
  private var sharing: Byte = SHARING_EXCLUSIVE

  /** Re-initialize this block and return its old buffer. */
  private def reinit(buf: Array[Byte], bigEndian: Boolean, start: Int, pos: Int, lim: Int, sharing: Byte): Array[Byte] = {
    val b = this.buf
    this.buf = buf
    this.bigEndian = bigEndian
    this.start = start
    this.pos = pos
    this.lim = lim
    this.sharing = sharing
    totalFlushed = 0
    closed = false
    b
  }

  /** Change the byte order of this BufferedInput. */
  def order(order: ByteOrder): this.type = {
    bigEndian = order == ByteOrder.BIG_ENDIAN
    this
  }

  @inline private[this] def throwClosed: Nothing = throw new IOException("BufferedOutput has already been closed")

  protected[this] def checkState(): Unit = {
    if(closed) throwClosed
  }

  @inline private[this] def available: Int = lim - pos

  def totalBytesWritten: Long = {
    //TODO compute this correctly (also for prefix lists and deferred blocks)
    totalFlushed + (pos - start)
  }

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
    if(out != null && (prev eq root)) {
      if(root.flushBlocks(true) && lim < start+count) growBufferClear(count)
      else growBufferCopy(count)
    } else growBufferCopy(count)
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
  private def flushBlocks(forceFlushRoot: Boolean): Boolean = {
    while(next ne this) {
      val b = next
      if(!b.closed) return false
      val n = b.next
      if(b.sharing == SHARING_LEFT && (b.pos - b.start) < initialBufferSize) {
        assert(b.pos == b.lim)
        n.start = b.start
      } else {
        val blen = b.pos - b.start
        if(blen < initialBufferSize/2 && b.sharing != SHARING_LEFT && n.sharing != SHARING_LEFT && !n.fixed && b.mergeToRight) ()
        else if(blen > 0) writeToOutput(b.buf, b.start, b.pos-b.start)
      }
      unlink(b)
    }
    val length = pos-start
    if((forceFlushRoot && length > 0) || length > initialBufferSize/2) {
      writeToOutput(buf, start, length)
      pos = start
    }
    true
  }

  /** Merge the contents of this buffer into the next one. Returns false if there was not enough space to merge. */
  private def mergeToRight(): Boolean = {
    val n = next
    val nlen = n.pos - n.start
    //println(s"Trying to merge ${this.show} into ${n.show}")
    if(pos + nlen < initialBufferSize) {
      //println(s"Merging ${this.show} into ${n.show}")
      System.arraycopy(n.buf, n.start, buf, pos, nlen)
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

  private def show: String = {
    s"[buf=$buf, start=$start, pos=$pos, lim=$lim, closed=$closed, sharing=$sharing, fixed=$fixed]"
  }

  /** Switch a potentially shared block to exclusive after re-allocating its buffer */
  private[this] def unshare(): Unit = {
    assert(sharing != SHARING_LEFT)
    if(sharing == SHARING_RIGHT) {
      sharing = SHARING_EXCLUSIVE
      prev.sharing =
        if(prev.sharing == SHARING_LEFT) SHARING_RIGHT else SHARING_EXCLUSIVE
    }
  }

  private[this] def growBufferClear(count: Int): Unit = {
    if(!fixed) {
      val buflen = BufferUtil.growBuffer(buf.length, count, 1)
      //println(s"$show.growBufferClear($count): $buflen")
      buf = new Array[Byte](buflen)
      lim = buflen
      unshare()
    }
  }

  private[this] def growBufferCopy(count: Int): Unit = {
    if(!fixed) {
      val buflen = BufferUtil.growBuffer(buf.length, pos + count, 1)
      //println(s"$show.growBufferCopy($count): $buflen")
      buf = Arrays.copyOf(buf, buflen)
      lim = buflen
      unshare()
    }
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
      val b = new BufferedOutput(new Array[Byte](initialBufferSize), bigEndian, 0, 0, 0, out, initialBufferSize, false)
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
      val b = new BufferedOutput(null, bigEndian, 0, 0, 0, out, initialBufferSize, false)
      b.root = this
      b.fixed = true
      b
    } else {
      val b = cachedShared
      cachedShared = b.next
      b
    }
  }

  private[this] def deferShort(max: Int, truncate: Boolean): BufferedOutput = {
    val p = fwd(max)
    val b = root.getSharedBlock()
    b.reinit(buf, bigEndian, start, p, pos, SHARING_LEFT)
    insertPrefix(b)
    start = pos
    if(sharing == SHARING_EXCLUSIVE) sharing = SHARING_RIGHT
    b
  }

  private[this] def deferLong(truncate: Boolean): BufferedOutput = {
    val b = root.getExclusiveBlock()
    insertPrefix(b)
    buf = b.reinit(buf, bigEndian, start, pos, lim, SHARING_EXCLUSIVE)
    pos = 0
    lim = buf.size
    b
  }

  def defer(max: Long = Long.MaxValue, truncate: Boolean = true): BufferedOutput = {
    //TODO support `truncate`
    //TODO enforce limit in deferLong
    checkState()
    if(fixed) deferShort((max min available).toInt, truncate)
    else if(max < initialBufferSize) deferShort(max.toInt, truncate)
    else deferLong(truncate)
  }
}

class FullyBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int, _initialBufferSize: Int)
  extends BufferedOutput(_buf, _bigEndian, _start, _pos, _lim, null, _initialBufferSize, false) {

  //TODO support prefix blocks

  def writeBytes(out: Array[Byte]): Unit = {
    val b = getBuffer
    System.arraycopy(b, 0, out, 0, pos)
  }
  def getBuffer = buf
  def getSize = pos
}
