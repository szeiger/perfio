package perfio

import java.io.{EOFException, OutputStream}
import java.lang.invoke.{MethodHandles, VarHandle}
import java.nio.{ByteBuffer, ByteOrder}
import java.util.Arrays
import scala.reflect.ClassTag

object BufferedOutput {
  def apply(out: OutputStream, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): BufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    //val bb = ForeignSupport.createByteBuffer(buf, byteOrder)
    new BufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length, out)
  }

  def growing(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, initialBufferSize: Int = 32768): FullyBufferedOutput = {
    val buf: Array[Byte] = new Array(initialBufferSize max MinBufferSize)
    //val bb = ForeignSupport.createByteBuffer(buf, byteOrder)
    new FullyBufferedOutput(buf, byteOrder == ByteOrder.BIG_ENDIAN, 0, 0, buf.length)
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
}

class BufferedOutput(
  protected[this] var buf: Array[Byte],
  private[this] var bigEndian: Boolean,
  protected[this] var start: Int, // first used byte in buf
  protected[this] var pos: Int,
  protected[this] var lim: Int,
  out: OutputStream,
) extends AutoCloseable {

  private[this] var totalFlushed = 0L

  /** Change the byte order of this BufferedInput. */
  def order(order: ByteOrder): this.type = {
    bigEndian = order == ByteOrder.BIG_ENDIAN
    this
  }

  @inline private[this] def available: Int = lim - pos

  def totalBytesWritten: Long = {
    totalFlushed + (pos - start)
  }

  private[this] def fwd(count: Int): Int = {
    if(available < count) {
      flushBuffer(count)
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

  protected[this] def flushBuffer(count: Int): Unit = {
    // TODO support shared buffers and non-0 start
    if(out != null) {
      if(pos > 0) {
        out.write(buf, 0, pos)
        pos = 0
      }
      if(lim < count) {
        val buflen = BufferUtil.growBuffer(buf.length, count, 1)
        buf = new Array[Byte](buflen)
        lim = buflen
      }
    } else {
      val target = pos + count
      var buflen = buf.length
      while(buflen < target) buflen *= 2
      buf = Arrays.copyOf(buf, buflen)
      lim = buflen
    }
  }

  def flush(): Unit = if(pos != 0 && out != null) flushBuffer(0)

  def close(): Unit = if(out != null) flush()

  def defer(max: Long = Long.MaxValue, truncate: Boolean = true): BufferedOutput = {
    ???
  }
}

class FullyBufferedOutput(_buf: Array[Byte], _bigEndian: Boolean, _start: Int, _pos: Int, _lim: Int)
  extends BufferedOutput(_buf, _bigEndian, _start, _pos, _lim, null) {

  def writeBytes(out: Array[Byte]): Unit = System.arraycopy(buf, 0, out, 0, pos)
  def getBuffer = buf
  def getSize = pos
}
