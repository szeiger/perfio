package perfio

import java.io.{EOFException, Flushable, IOException, OutputStream}
import java.lang.invoke.MethodHandles
import java.nio.ByteOrder
import java.util.Arrays
import scala.reflect.ClassTag
import BufferedOutput.{SHARING_EXCLUSIVE, SHARING_LEFT, SHARING_RIGHT}

import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, OpenOption, Path}

object BufferedOutput {
  def apply(out: OutputStream, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    new FlushingBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, initialBufferSize, false, Long.MaxValue, out)
  }

  def growing(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): FullyBufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    new FullyBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, initialBufferSize, false)
  }

  def fixed(buf: Array[Byte], start: Int = 0, len: Int = -1, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): FullyBufferedOutput =
    new FullyBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, start, start, if(len == -1) buf.length else len+start, initialBufferSize, true)

  def ofFile(path: Path, byteOrder: ByteOrder, initialBufferSize: Int, option: OpenOption*): BufferedOutput = {
    val out = Files.newOutputStream(path, option: _*)
    apply(out, byteOrder, initialBufferSize)
  }

  def ofFile(path: Path, option: OpenOption*): BufferedOutput = {
    val out = Files.newOutputStream(path, option: _*)
    apply(out)
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
  private[perfio] val fixed: Boolean, // buffer cannot be reallocated or grown beyond lim
  private[perfio] var totalLimit: Long,
) extends AutoCloseable with Flushable {

  private[perfio] var totalFlushed = 0L
  private[perfio] var next, prev: BufferedOutput = this // prefix list as a double-linked ring
  private[perfio] var closed = false
  private[perfio] var truncate = true
  private[perfio] var root: BufferedOutput = this
  private[perfio] var cacheRoot: CacheRootBufferedOutput = null
  private[perfio] var sharing: Byte = SHARING_EXCLUSIVE

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

  private[perfio] def fwd(count: Int): Int = {
    if(available < count) {
      flushAndGrow(count)
      if(available < count) throw new EOFException()
    }
    val p = pos
    pos += count
    p
  }

  private[perfio] def tryFwd(count: Int): Int = {
    if(available < count) flushAndGrow(count)
    val p = pos
    pos += (count min available)
    p
  }

  def write(a: Array[Byte], off: Int, len: Int): this.type = {
    val p = fwd(len)
    System.arraycopy(a, off, buf, p, len)
    this
  }

  def write(a: Array[Byte]): this.type = write(a, 0, a.length)

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

  def bytes(b: Array[Byte], off: Int, len: Int): this.type = {
    val p = fwd(len)
    System.arraycopy(b, off, buf, p, len)
    this
  }

  def bytes(b: Array[Byte]): this.type = bytes(b, 0, b.length)

  def string(s: String, charset: Charset = StandardCharsets.UTF_8): Int = {
    val b = s.getBytes(charset)
    bytes(b)
    b.length
  }

  def zstring(s: String, charset: Charset = StandardCharsets.UTF_8): Int = {
    val b = s.getBytes(charset)
    val len = b.length
    val p = fwd(len + 1)
    System.arraycopy(b, 0, buf, p, b.length)
    buf(p + len) = 0
    len + 1
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

  private[perfio] def checkUnderflow(): Unit = {
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
    val tlim = ((totalLimit - totalBytesWritten) min count).toInt
    val buflen = BufferUtil.growBuffer(buf.length, tlim, 1)
    //println(s"$show.growBufferClear($count): ${buf.length} -> $buflen")
    if(buflen > buf.length) buf = new Array[Byte](buflen)
    lim = buf.length
    pos = 0
    start = 0
    unshare()
  }

  private[this] def growBufferCopy(count: Int): Unit = {
    val tlim = ((totalLimit - totalBytesWritten) min (pos + count)).toInt
    val buflen = BufferUtil.growBuffer(buf.length, tlim, 1)
    //println(s"$show.growBufferCopy($count): ${buf.length} -> $buflen")
    if(buflen > buf.length) buf = Arrays.copyOf(buf, buflen)
    lim = buf.length
    unshare()
  }

  /** Insert this block before the given block. */
  private[perfio] def insertBefore(b: BufferedOutput): Unit = {
    prev = b.prev
    next = b
    b.prev.next = this
    b.prev = this
  }

  /** Insert this block and its prefix list before the given block. Must only be called on a root block. */
  private[perfio] def insertAllBefore(b: BufferedOutput): Unit = {
    val n = next
    n.prev = b.prev
    b.prev.next = n
    next = b
    b.prev = this
  }

  /** Unlink this block and return it to the cache. */
  private[perfio] def unlinkAndReturn(): Unit = {
    prev.next = next
    next.prev = prev
    cacheRoot.returnToCache(this)
  }

  /** Unlink this block. */
  private[perfio] def unlinkOnly(): Unit = {
    prev.next = next
    next.prev = prev
  }

  /** Flush and unlink all closed blocks and optionally flush the root block. Must only be called on the root block. */
  private[perfio] def flushBlocks(forceFlush: Boolean): Unit

  /** Force flush to the upstream after flushBlocks. Must only be called on the root block. */
  private[perfio] def flushUpstream(): Unit

  /** Called at the end of the first close(). */
  protected def closeUpstream(): Unit = ()

  final def flush(): Unit = {
    checkState()
    if(prev eq root) {
      root.flushBlocks(true)
      root.flushUpstream()
    }
  }

  final def close(): Unit = {
    if(!closed) {
      if(!truncate) checkUnderflow()
      closed = true
      lim = pos
      if(root eq this) {
        var b = next
        while(b ne this) {
          if(!b.closed) {
            if(!b.truncate) b.checkUnderflow()
            b.closed = true
          }
          b = b.next
        }
        flushBlocks(true)
      } else {
        if(prev eq root) root.flushBlocks(false)
      }
      closeUpstream()
    }
  }

  /** Reserve a short block (fully allocated) by splitting this block into two shared blocks. */
  private[this] def reserveShort(length: Int): BufferedOutput = {
    val p = fwd(length)
    val len = p - start
    val b = cacheRoot.getSharedBlock()
    b.reinit(buf, bigEndian, start, p, pos, SHARING_LEFT, length, -len, false, root, null)
    b.insertBefore(this)
    totalFlushed += (pos - start)
    start = pos
    if(sharing == SHARING_EXCLUSIVE) sharing = SHARING_RIGHT
    b
  }

  /** Reserve a long block (flushed on demand) by allocating a new block. Must not be called on a shared block. */
  private[this] def reserveLong(max: Long): BufferedOutput = {
    val len = pos - start
    val b = cacheRoot.getExclusiveBlock()
    var l = lim
    if(l - pos > max) l = (max + pos).toInt
    buf = b.reinit(buf, bigEndian, start, pos, l, sharing, max-len, -len, false, root, null)
    //println(s"after reinit for max=$max, lim=$lim, l=$l: ${b.show}")
    b.insertBefore(this)
    totalFlushed = totalFlushed + len + max
    sharing = SHARING_EXCLUSIVE
    pos = 0
    lim = buf.size
    b
  }

  /** Leave a fixed-size gap at the current position that can be filled later after continuing to write
   * to this BufferedOutput. This BufferedOutput's `totalBytesWritten` is immediately increased by the requested
   * size. Attempting to write more than the requested amount of data to the returned BufferedOutput or closing
   * it before writing all of the data throws an IOException. */
  final def reserve(length: Long): BufferedOutput = {
    checkState()
    if(fixed) reserveShort((length min available).toInt)
    else if(length < cacheRoot.initialBufferSize) reserveShort(length.toInt)
    else reserveLong(length)
  }

  /** Create a new BufferedOutput `b` that gets appended to `this` when closed. If `this` has a limited
   * size and appending `b` would exceed the limit, an EOFException is thrown when attempting to close `b`.
   * Attempting to write more than the requested maximum length to `b` results in an IOException. */
  def defer(max: Long = Long.MaxValue): BufferedOutput = {
    val b = cacheRoot.getExclusiveBlock()
    b.reinit(b.buf, bigEndian, 0, 0, b.buf.length, SHARING_EXCLUSIVE, max, 0L, true, b, this)
    b.next = b
    b.prev = b
    b
  }

  /** Append a nested root block */
  private[perfio] def appendNested(b: BufferedOutput): Unit = {
    val btot = b.totalBytesWritten
    val rem = totalLimit - totalBytesWritten
    if(btot > rem) throw new EOFException()
    if(fixed) appendNestedToFixed(b)
    else {
      val tmpbuf = buf
      val tmpstart = start
      val tmppos = pos
      val tmpsharing = sharing
      val len = tmppos - tmpstart
      val blen = b.pos - b.start
      totalFlushed += b.totalFlushed + len
      b.totalFlushed += blen - len
      buf = b.buf
      start = b.start
      pos = b.pos
      lim = buf.length
      sharing = b.sharing
      b.buf = tmpbuf
      b.start = tmpstart
      b.pos = tmppos
      b.lim = tmppos
      b.closed = true
      b.sharing = tmpsharing
      b.insertAllBefore(this)
      if(b.prev eq this) flushBlocks(false)
    }
  }

  private[this] def appendNestedToFixed(r: BufferedOutput): Unit = {
    var b = r.next
    while(true) {
      val blen = b.pos - b.start
      if(blen > 0) {
        val p = fwd(blen)
        System.arraycopy(b.buf, b.start, buf, p, blen)
      }
      cacheRoot.returnToCache(b)
      if(b eq r) return
      b = b.next
    }
  }
}


final class NestedBufferedOutput(_buf: Array[Byte], _fixed: Boolean, _cacheRoot: CacheRootBufferedOutput)
  extends BufferedOutput(_buf, false, 0, 0, 0, _fixed, Long.MaxValue) {

  private[this] var parent: BufferedOutput = null

  this.cacheRoot = _cacheRoot

  /** Re-initialize this block and return its old buffer. */
  private[perfio] def reinit(buf: Array[Byte], bigEndian: Boolean, start: Int, pos: Int, lim: Int, sharing: Byte,
    totalLimit: Long, totalFlushed: Long, truncate: Boolean, root: BufferedOutput, parent: BufferedOutput): Array[Byte] = {
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
    this.root = root
    this.parent = parent
    closed = false
    b
  }

  private[perfio] def flushBlocks(forceFlush: Boolean): Unit = ()

  private[perfio] def flushUpstream(): Unit = ()

  protected override def closeUpstream(): Unit = {
    if(parent != null) parent.appendNested(this)
  }
}


abstract class CacheRootBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int,
  private[perfio] val initialBufferSize: Int,
  _fixed: Boolean, _totalLimit: Long)
  extends BufferedOutput(_buf, _bigEndian, _start, _pos, _lim, _fixed, _totalLimit) {

  this.cacheRoot = this

  private[this] var cachedExclusive, cachedShared: BufferedOutput = null // single-linked lists (via `next`) of blocks cached for reuse

  private[perfio] def returnToCache(b: BufferedOutput): Unit = {
    if(b.sharing == SHARING_LEFT) {
      b.buf = null
      b.next = cachedShared
      cachedShared = b
    } else {
      b.next = cachedExclusive
      cachedExclusive = b
    }
  }

  /** Get a cached or new exclusive block. */
  private[perfio] def getExclusiveBlock(): NestedBufferedOutput = {
    if(cachedExclusive == null)
      new NestedBufferedOutput(new Array[Byte](cacheRoot.initialBufferSize), false, cacheRoot)
    else {
      val b = cachedExclusive
      cachedExclusive = b.next
      b.asInstanceOf[NestedBufferedOutput]
    }
  }

  /** Get a cached or new shared block. */
  private[perfio] def getSharedBlock(): NestedBufferedOutput = {
    if(cachedShared == null)
      new NestedBufferedOutput(null, true, cacheRoot)
    else {
      val b = cachedShared
      cachedShared = b.next
      b.asInstanceOf[NestedBufferedOutput]
    }
  }
}


class FullyBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int, _initialBufferSize: Int, _fixed: Boolean)
  extends CacheRootBufferedOutput(_buf, _bigEndian, _start, _pos, _lim, _initialBufferSize, _fixed, Long.MaxValue) {

  private var outbuf: Array[Byte] = null
  private var outstart, outpos: Int = 0

  private[perfio] def flushBlocks(forceFlush: Boolean): Unit = {
    while(next ne this) {
      val b = next
      if(!b.closed) return
      val blen = b.pos - b.start
      if(blen > 0) {
        if(b.sharing == SHARING_LEFT) {
          val n = b.next
          n.start = b.start
          n.totalFlushed -= blen
          b.unlinkAndReturn()
        } else flushSingle(b, true)
      } else b.unlinkAndReturn()
    }
  }

  private[this] def flushSingle(b: BufferedOutput, unlink: Boolean): Unit = {
    val blen = b.pos - b.start
    if(outbuf == null) {
      outbuf = b.buf
      outstart = b.start
      outpos = b.pos
      if(unlink) b.unlinkOnly()
    } else {
      val tot = totalBytesWritten
      if((outbuf.length - outpos) < tot) {
        if(tot + outpos > Int.MaxValue) throw new IOException("Buffer exceeds maximum array length")
        growOutBuffer((tot + outpos).toInt)
      }
      System.arraycopy(b.buf, b.start, outbuf, outpos, blen)
      outpos += blen
      if(unlink) b.unlinkAndReturn()
    }
  }

  private[this] def growOutBuffer(target: Int): Unit = {
    val buflen = BufferUtil.growBuffer(outbuf.length, target, 1)
    //println(s"Growing outbuf from ${outbuf.length} to $buflen based on $target")
    if(buflen > outbuf.length) outbuf = Arrays.copyOf(outbuf, buflen)
  }

  protected override def closeUpstream(): Unit = {
    flushSingle(this, false)
  }

  private[perfio] def flushUpstream(): Unit = ()

  private[this] def checkClosed(): Unit =
    if(!closed) throw new IOException("Cannot access buffer before closing")

  def copyToByteArray: Array[Byte] = Arrays.copyOf(getBuffer, getLength)

  def getBuffer: Array[Byte] = {
    checkClosed()
    outbuf
  }

  def getLength = {
    checkClosed()
    outpos
  }
}


class FlushingBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int,
  _initialBufferSize: Int, _fixed: Boolean, _totalLimit: Long, out: OutputStream)
  extends CacheRootBufferedOutput(_buf, _bigEndian, _start, _pos, _lim, _initialBufferSize, _fixed, _totalLimit) {

  private[perfio] def flushUpstream(): Unit = out.flush()

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
      b.unlinkAndReturn()
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
