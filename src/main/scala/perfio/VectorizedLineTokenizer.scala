package perfio

import jdk.incubator.vector.{ByteVector, VectorMask, VectorOperators}

import java.io.InputStream
import java.lang.{Long => JLong}
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Arrays
import scala.annotation.tailrec

/**
 * A vectorized implementation of [[LineTokenizer]].
 *
 * The parser uses explicit SIMD loops with up to 512 bits / 64 lanes per vector (depending on
 * hardware and JVM support).
 */
object VectorizedLineTokenizer {
  import VectorSupport._

  /** Create a VectorizedLineTokenizer from an InputStream. See [[LineTokenizer.apply]] for details. */
  def apply(in: InputStream, charset: Charset = StandardCharsets.UTF_8, initialBufferSize: Int = 32768,
    minRead: Int = -1, streaming: Boolean = true): VectorizedLineTokenizer = {
    val in2 = if(streaming) in else new BufferingAdapter(in)
    val buf = new Array[Byte](if(initialBufferSize % species.length() == 0) initialBufferSize else species.loopBound(initialBufferSize+vlen))
    val inputBufferHeadroom = if(minRead > 0) minRead else buf.length/2
    create(buf, 0, 0, in2, inputBufferHeadroom, charset)
  }

  /** Create a VectorizedLineTokenizer from an array of bytes. See [[LineTokenizer.fromArray]] for details. */
  def fromArray(buf: Array[Byte], offset: Int = 0, length: Int = -1, charset: Charset = StandardCharsets.UTF_8): VectorizedLineTokenizer =
    create(buf, offset, if(length == -1) buf.length-offset else length, null, 0, charset)

  private[this] def create(buf: Array[Byte], start: Int, limit: Int, in: InputStream, inputBufferHeadroom: Int, cs: Charset): VectorizedLineTokenizer =
    if(cs eq StandardCharsets.ISO_8859_1)
      new VectorizedLineTokenizer(buf, start, limit, in, inputBufferHeadroom) {
        // Use the slightly faster constructor for Latin-1
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      }
    else
      new VectorizedLineTokenizer(buf, start, limit, in, inputBufferHeadroom) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, cs)
      }

  private[this] final class BufferingAdapter(in: InputStream) extends InputStream {
    override def read(b: Array[Byte], off: Int, len: Int) = in.readNBytes(b, off, len)
    override def close(): Unit = in.close()
    def read(): Int = in.read()
  }
}

abstract class VectorizedLineTokenizer private (
  private[this] var buf: Array[Byte],
  private[this] var start: Int, // start of the current line in buf
  private[this] var limit: Int, // number of used bytes in buf
  in: InputStream, inputBufferHeadroom: Int
) extends LineTokenizer {
  import VectorSupport._

  private[this] var pos = start-vlen // start of the current vector in buf
  private[this] var mask = 0L // vector mask that marks the LFs

  private[this] def buffer(): Int = {
    if(pos == Int.MaxValue || in == null) return 0
    val offsetInVector = limit % vlen
    if(limit > buf.length-inputBufferHeadroom) {
      val shift = species.loopBound(start)
      if(shift > buf.length/2) {
        System.arraycopy(buf, start, buf, start-shift, pos-start)
        pos -= shift
        start -= shift
        limit -= shift
      } else buf = Arrays.copyOf(buf, buf.length*2)
    }
    val read = in.read(buf, limit, buf.length-limit)
    if(read > 0) {
      limit += read
      // old limit was not at vector boundary -> rewind and mask the already read lanes
      if(offsetInVector > 0) {
        pos -= vlen
        mask = computeMask() & (-1L >>> offsetInVector << offsetInVector)
      } else mask = computeMask()
      read
    } else 0
  }

  private[this] def rest(): String = {
    val r = if(pos != Int.MaxValue) {
      val l = limit-start
      mask = 0
      if(l != 0) makeString(buf, start, l) else null
    } else null
    pos = Int.MaxValue - vlen
    r
  }

  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String

  @inline private[this] def emit(start: Int, lfpos: Int): String = {
    val end = if(lfpos > 0 && buf(lfpos-1) == '\r'.toByte) lfpos-1 else lfpos
    if(start == end) "" else makeString(buf, start, end-start)
  }

  @tailrec final def readLine(): String = {
    val f = JLong.numberOfTrailingZeros(mask)
    if(f < vlen) {
      val lfpos = pos+f
      val s = emit(start, lfpos)
      start = lfpos+1
      mask &= ~(1L<<f)
      return s
    }
    pos += vlen
    if(pos < limit) mask = computeMask()
    else if(buffer() == 0) return rest()
    readLine()
  }

  private[this] final def computeMask(): Long = {
    val overshoot = pos+vlen-limit
    val v =
      if(overshoot <= 0) ByteVector.fromArray(species, buf, pos)
      else ByteVector.fromArray(species, buf, pos, VectorMask.fromLong(species, fullMask >>> overshoot))
    // Seems to be slightly faster than comparing against a scalar:
    v.compare(VectorOperators.EQ, lfs).toLong
  }

  def close(): Unit = if(in != null) in.close()
}
