package perfio

import java.io.{EOFException, IOException, OutputStream}
import java.lang.invoke.{MethodHandles, VarHandle}
import java.nio.{ByteBuffer, ByteOrder}
import java.util.Arrays
import scala.reflect.ClassTag

import BufferedOutput.{SHARING_EXCLUSIVE, SHARING_LEFT, SHARING_RIGHT}

object BufferedOutput {
  def apply(out: OutputStream, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    new FlushingBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, initialBufferSize, false, Long.MaxValue, out)
  }

  def growing(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): FullyBufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    new FullyBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, initialBufferSize, false)
  }

  //TODO make this work with block merging or remove the method?
  def fixed(buf: Array[Byte], start: Int = 0, len: Int = -1, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedOutput = {
    new FullyBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, start, start, if(len == -1) buf.length else len+start, initialBufferSize, true)
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

  private[perfio] val SHARING_EXCLUSIVE = 0.toByte // buffer is not shared
  private[perfio] val SHARING_LEFT = 1.toByte      // buffer is shared with next block
  private[perfio] val SHARING_RIGHT = 2.toByte     // buffer is shared with previous blocks only
}

abstract class BufferedOutput(
  private[perfio] var buf: Array[Byte],
  protected var bigEndian: Boolean,
  private[perfio] var start: Int, // first used byte in buf
  private[perfio] var pos: Int,
  private[perfio] var lim: Int,
  private[perfio] var fixed: Boolean, // buffer cannot be reallocated or grown beyond lim
  private[this] var totalLimit: Long,
) extends AutoCloseable {

  private[perfio] var totalFlushed = 0L
  private[perfio] var next, prev: BufferedOutput = this // prefix list as a double-linked ring
  private[perfio] var closed = false
  private[perfio] var truncate = true
  private[perfio] var root: RootBufferedOutput = null
  private[perfio] var sharing: Byte = SHARING_EXCLUSIVE

  /** Re-initialize this block and return its old buffer. */
  private def reinit(buf: Array[Byte], bigEndian: Boolean, start: Int, pos: Int, lim: Int, sharing: Byte, totalLimit: Long, totalFlushed: Long, truncate: Boolean): Array[Byte] = {
    val b = this.buf
    this.buf = buf
    this.bigEndian = bigEndian
    this.start = start
    this.pos = pos
    this.lim = lim
    this.sharing = sharing
    this.totalLimit = totalLimit
    this.totalFlushed = totalFlushed
    this.truncate = truncate
    closed = false
    b
  }

  /** Change the byte order of this BufferedOutput. */
  final def order(order: ByteOrder): this.type = {
    bigEndian = order == ByteOrder.BIG_ENDIAN
    this
  }

  private[this] def throwClosed(): Nothing = throw new IOException("BufferedOutput has already been closed")

  private[this] def throwUnderflow(len: Long, exp: Long): Nothing =
    throw new IOException(s"Can't close BufferedOutput created by reserve($exp): Only $len bytes written")

  private[this] def checkState(): Unit = {
    if(closed) throwClosed()
  }

  @inline private[perfio] final def available: Int = lim - pos

  final def totalBytesWritten: Long = totalFlushed + (pos - start)

  private[this] def fwd(count: Int): Int = {
    if(available < count) {
      flushAndGrow(count)
      if(available < count) throw new EOFException()
    }
    val p = pos
    pos += count
    p
  }

  final def int8(b: Byte): this.type = {
    val p = fwd(1)
    buf(p) = b
    this
  }

  final def uint8(b: Int): this.type = int8(b.toByte)

  final def int16(s: Short): this.type = {
    val p = fwd(2)
    (if(bigEndian) BufferedOutput.SHORT_BIG else BufferedOutput.SHORT_LITTLE).set(buf, p, s)
    this
  }

  final def uint16(c: Char): this.type = {
    val p = fwd(2)
    (if(bigEndian) BufferedOutput.CHAR_BIG else BufferedOutput.CHAR_LITTLE).set(buf, p, c)
    this
  }

  final def int32(i: Int): this.type = {
    val p = fwd(4)
    (if(bigEndian) BufferedOutput.INT_BIG else BufferedOutput.INT_LITTLE).set(buf, p, i)
    this
  }

  final def uint32(i: Long): this.type = int32(i.toInt)

  final def int64(l: Long): this.type = {
    val p = fwd(8)
    (if(bigEndian) BufferedOutput.LONG_BIG else BufferedOutput.LONG_LITTLE).set(buf, p, l)
    this
  }

  final def float32(f: Float): this.type = {
    val p = fwd(4)
    (if(bigEndian) BufferedOutput.FLOAT_BIG else BufferedOutput.FLOAT_LITTLE).set(buf, p, f)
    this
  }

  final def float64(d: Double): this.type = {
    val p = fwd(8)
    (if(bigEndian) BufferedOutput.DOUBLE_BIG else BufferedOutput.DOUBLE_LITTLE).set(buf, p, d)
    this
  }

  private[this] def flushAndGrow(count: Int): Unit = {
    checkState()
    if(!fixed) {
      //println(s"$show.flushAndGrow($count)")
      if(prev eq root) {
        root.flushBlocks(true)
        if(lim < pos + count) {
          if(pos == start) growBufferClear(count)
          else growBufferCopy(count)
        }
      } else growBufferCopy(count)
    }
  }

  private def checkUnderflow(): Unit = {
    if(fixed) {
      if(pos != lim) throwUnderflow(pos-start, lim-start)
    } else {
      if(totalBytesWritten != totalLimit) throwUnderflow(totalLimit, totalBytesWritten)
    }
  }

  /** Merge the contents of this block into the next one using this block's buffer. */
  private[perfio] final def mergeToRight(): Unit = {
    val n = next
    val nlen = n.pos - n.start
    System.arraycopy(n.buf, n.start, buf, pos, nlen)
    val len = pos - start
    n.totalFlushed -= len
    val tmpbuf = buf
    buf = n.buf
    n.buf = tmpbuf
    n.start = start
    n.lim = tmpbuf.length
    n.pos = pos + nlen
  }

  private[perfio] def show: String =
    s"[buf=$buf, buf.length=${buf.length}, start=$start, pos=$pos, lim=$lim, closed=$closed, sharing=$sharing, fixed=$fixed, totalFlushed=$totalFlushed, totalLimit=$totalLimit, available=$available, totalBytesWritten=$totalBytesWritten]"

  /** Switch a potentially shared block to exclusive after re-allocating its buffer */
  private[perfio] def unshare(): Unit = {
    if(sharing == SHARING_RIGHT) {
      sharing = SHARING_EXCLUSIVE
      if(prev.sharing == SHARING_LEFT) prev.sharing = SHARING_RIGHT
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

  private def insertBefore(b: BufferedOutput): Unit = {
    prev = b.prev
    next = b
    b.prev.next = this
    b.prev = this
  }

  final def flush(): Unit = {
    checkState()
    if(prev eq root) {
      root.flushBlocks(true)
      root.flushUpstream()
    }
  }

  final def close(): Unit = {
    checkState()
    if(!truncate) checkUnderflow()
    closed = true
    lim = pos
    if(root ne this) {
      if(prev eq root) root.flushBlocks(false)
    } else {
      var b = next
      while(b ne this) {
        if(!b.closed) {
          if(!b.truncate) b.checkUnderflow()
          b.closed = true
        }
        b = b.next
      }
      root.flushBlocks(true)
      root.closeUpstream()
    }
  }

  private[this] def deferShort(length: Int): BufferedOutput = {
    val p = fwd(length)
    val len = p - start
    val b = root.getSharedBlock()
    b.reinit(buf, bigEndian, start, p, pos, SHARING_LEFT, Long.MaxValue, -len, false)
    b.insertBefore(this)
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
    if(sharing == SHARING_LEFT) {
      ??? //TODO split shared buffers
    } else {
      buf = b.reinit(buf, bigEndian, start, pos, l, sharing, max-len, -len, deferred)
      //println(s"after reinit for max=$max, lim=$lim, l=$l: ${b.show}")
      b.insertBefore(this)
      totalFlushed += len
      sharing = SHARING_EXCLUSIVE
      if(!deferred) totalFlushed += max
      pos = 0
      lim = buf.size
    }
    b
  }

  /** Leave a fixed-size gap at the current position that can be filled later after continuing to write
   * to this BufferedOutput. This BufferdOutput's `totalBytesWritten` is immediately increased by the requested
   * size. Attempting to write more than the requested amount of data to the returned BufferedOutput or closing
   * it before writing all of the data throws an IOException. */
  final def reserve(length: Long): BufferedOutput = {
    checkState()
    if(fixed) deferShort((length min available).toInt)
    else if(length < root.initialBufferSize) deferShort(length.toInt)
    else deferLong(length, false)
  }

  /** Create a hole at the current position that can be filled later after continuing to write
   * to this BufferedOutput. Its size is *not* counted as part of this object's size. */
  final def defer(max: Long = Long.MaxValue): BufferedOutput = {
    checkState()
    deferLong(max, true)
  }
}


final class NestedBufferedOutput(_buf: Array[Byte], _fixed: Boolean, _root: RootBufferedOutput)
  extends BufferedOutput(_buf, false, 0, 0, 0, _fixed, Long.MaxValue) {

  this.root = _root
}


abstract class RootBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int,
  private[perfio] val initialBufferSize: Int,
  _fixed: Boolean, _totalLimit: Long)
  extends BufferedOutput(_buf, _bigEndian, _start, _pos, _lim, _fixed, _totalLimit) {

  this.root = this

  private[this] var cachedExclusive, cachedShared: BufferedOutput = null // single-linked lists (via `next`) of blocks cached for reuse

  /** Flush and unlink all closed blocks and optionally flush the root block. */
  private[perfio] def flushBlocks(forceFlush: Boolean): Unit

  private[perfio] def flushUpstream(): Unit

  private[perfio] def closeUpstream(): Unit

  /** Unlink a block and return it to the cache. */
  private[perfio] def unlink(b: BufferedOutput): Unit = {
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

  /** Get a cached or new exclusive block. Must only be called on the root block. */
  private[perfio] def getExclusiveBlock(): BufferedOutput = {
    if(cachedExclusive == null)
      new NestedBufferedOutput(new Array[Byte](root.initialBufferSize), false, root)
    else {
      val b = cachedExclusive
      cachedExclusive = b.next
      b
    }
  }

  /** Get a cached or new shared block. Must only be called on the root block. */
  private[perfio] def getSharedBlock(): BufferedOutput = {
    if(cachedShared == null)
      new NestedBufferedOutput(null, true, root)
    else {
      val b = cachedShared
      cachedShared = b.next
      b
    }
  }
}


class FullyBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int, _initialBufferSize: Int, _fixed: Boolean)
  extends RootBufferedOutput(_buf, _bigEndian, _start, _pos, _lim, _initialBufferSize, _fixed, Long.MaxValue) {

  private[perfio] def flushBlocks(forceFlush: Boolean): Unit = {
    while(next ne this) {
      val b = next
      if(!b.closed) return
      if(b.sharing == SHARING_LEFT) {
        val blen = b.pos - b.start
        if(blen > 0) {
          val n = b.next
          n.start = b.start
          n.totalFlushed -= blen
        }
      } else mergeBuffers(b)
      unlink(b)
    }
  }

//  private[this] def mergeShared(b: BufferedOutput): Boolean = {
//    if(b.sharing == SHARING_LEFT) {
//      val blen = b.pos - b.start
//      if(blen > 0) {
//        val n = b.next
//        if(n.buf eq b.buf)
//        val ns = b.nextShared
//        ns.start = b.start
//        ns.totalFlushed -= blen
//      }
//      unlink(b)
//    } else true
//  }

  private[this] def mergeBuffers(b: BufferedOutput): Unit = {
    //println(s"mergeBuffers ${b.show}, ${b.next.show}")
    assert(b.next.sharing != SHARING_LEFT) //TODO support this
    assert(!b.fixed)
    assert(!b.next.fixed)
    val n = b.next
    val blen = b.pos - b.start
    val ntotal = n.lim - n.start
    if(b.buf.length - b.pos < ntotal) {
      if(b.pos == b.start) {
        val buflen = BufferUtil.growBuffer(b.buf.length, ntotal, 1)
        b.buf = new Array[Byte](buflen)
        b.lim -= b.start
        b.pos = 0
        b.start = 0
      } else {
        val buflen = BufferUtil.growBuffer(b.buf.length, b.pos + ntotal, 1)
        b.buf = Arrays.copyOf(b.buf, buflen)
      }
    }

    //println(s"n before: ${n.show}")
    System.arraycopy(n.buf, n.start, b.buf, b.pos, ntotal)
    val noff = b.pos - n.start
    val tmpbuf = n.buf
    n.buf = b.buf
    n.pos += noff
    n.lim += noff
    n.start = b.start
    n.totalFlushed -= blen
    b.buf = tmpbuf
    //println(s"n after: ${n.show}")

    //b.mergeToRight()
    b.unshare()
  }

  //TODO support prefix blocks

  private[perfio] def flushUpstream(): Unit = ()
  private[perfio] def closeUpstream(): Unit = ()

  def toByteArray: Array[Byte] = {
    val b = getBuffer
    Arrays.copyOf(b, pos)
  }

  def writeBytes(out: Array[Byte]): Unit = {
    val b = getBuffer
    System.arraycopy(b, 0, out, 0, pos)
  }
  def getBuffer = buf
  def getSize = pos
}


class FlushingBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int,
  _initialBufferSize: Int, _fixed: Boolean, _totalLimit: Long, out: OutputStream)
  extends RootBufferedOutput(_buf, _bigEndian, _start, _pos, _lim, _initialBufferSize, _fixed, _totalLimit) {

  private[perfio] def flushUpstream(): Unit = out.flush()

  private[perfio] def closeUpstream(): Unit = out.close()

  private[perfio] def flushBlocks(forceFlush: Boolean): Unit = {
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
        return
      }
      if(b.sharing == SHARING_LEFT) {
        if(blen < initialBufferSize) {
          val n = b.next
          n.start = b.start
          n.totalFlushed -= blen
        } else if(blen > 0) writeToOutput(b.buf, b.start, blen)
      } else {
        if(blen < initialBufferSize/2 && maybeMergeToRight(b)) ()
        else if(blen > 0) writeToOutput(b.buf, b.start, blen)
      }
      unlink(b)
    }
    val len = pos-start
    if((forceFlush && len > 0) || len > initialBufferSize/2) {
      writeToOutput(buf, start, len)
      totalFlushed += len
      pos = start
    }
  }

  private[this] def maybeMergeToRight(b: BufferedOutput): Boolean = {
    val n = b.next
    if(!n.fixed && (b.pos + (n.pos - n.start) < initialBufferSize)) {
      b.mergeToRight()
      true
    } else false
  }

  /** Write the buffer to the output. Must only be called on the root block. */
  private[this] def writeToOutput(buf: Array[Byte], off: Int, len: Int): Unit = {
    //println(s"Writing $len bytes")
    out.write(buf, off, len)
  }
}
