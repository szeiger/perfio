package perfio

import jdk.incubator.vector.{ByteVector, VectorMask, VectorOperators}

import java.lang.{Long => JLong}
import java.nio.ByteOrder
import java.nio.charset.{Charset, StandardCharsets}
import scala.annotation.tailrec

/**
 * A vectorized implementation of [[LineTokenizer]].
 *
 * The parser uses explicit SIMD loops with up to 512 bits / 64 lanes per vector (depending on
 * hardware and JVM support).
 */
object VectorizedLineTokenizer {

  /** Create a VectorizedLineTokenizer2 from a BufferedInput. See [[LineTokenizer.apply]] for details. */
  def apply(in: BufferedInput, charset: Charset = StandardCharsets.UTF_8, eol: Byte = '\n'.toByte, preEol: Byte = '\r'.toByte): LineTokenizer = in match {
    case in: HeapBufferedInput =>
      if(charset eq StandardCharsets.ISO_8859_1) new HeapVectorizedLineTokenizer(in, eol, preEol) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      } else new HeapVectorizedLineTokenizer(in, eol, preEol) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, charset)
      }
    case in: DirectBufferedInput =>
      if(charset eq StandardCharsets.ISO_8859_1) new DirectVectorizedLineTokenizer(in, eol, preEol) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      } else new DirectVectorizedLineTokenizer(in, eol, preEol) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, charset)
      }
  }
}

private sealed abstract class HeapVectorizedLineTokenizer(_bin: HeapBufferedInput, _eolChar: Byte, _preEolChar: Byte) extends
  HeapLineTokenizer(_bin, _eolChar, _preEolChar) {
  import VectorSupport._

  private[this] var vpos = bin.pos-vlen // start of the current vector in buf
  private[this] var mask = 0L // vector mask that marks the LFs
  private[this] val eolVector = ByteVector.broadcast(species, eolChar)

  if(bin.pos < bin.lim) {
    // Make sure the position is aligned if we have buffered data.
    // Otherwise we leave the initial buffering to the main loop in readLine().
    vpos += vlen
    alignPosAndComputeMask()
  }

  private[this] def buffer(): Int = {
    //println(s"buffer: $closed, $mask, $vpos")
    //println(s"  buffer(): pos=${bin.pos}, lim=${bin.lim}, vpos=$vpos, buflen=${bin.buf.length}")
    if(vpos == Int.MaxValue) {
      vpos = Int.MaxValue - vlen
      return 0
    }
    val scanned = bin.lim - bin.pos
    val oldav = bin.available
    bin.prepareAndFillBuffer(oldav + vlen)
    //println(s"    $oldav -> ${bin.available}")
    if(oldav == bin.available) return 0
    vpos = bin.pos + scanned
    alignPosAndComputeMask()
    1
  }

  override private[perfio] def markClosed(): Unit = {
    super.markClosed()
    mask = 0L
    vpos = Int.MaxValue - vlen
  }

  private[this] def alignPosAndComputeMask(): Unit = {
    val offsetInVector = vpos % vlen
    vpos -= offsetInVector
    mask = computeMask() & (-1L >>> offsetInVector << offsetInVector)
  }

  private[this] def rest(): String = {
    checkState()
    //println(s"rest: pos=$pos")
    val r = if(vpos != Int.MaxValue) {
      val l = bin.available
      mask = 0
      if(l != 0) {
        val s = makeString(bin.buf, bin.pos, l)
        bin.pos = bin.lim
        s
      } else null
    } else null
    vpos = Int.MaxValue - vlen
    r
  }

  @tailrec final def readLine(): String = {
    val f = JLong.numberOfTrailingZeros(mask)
    //println(s"readLine(): ${bin.pos}, ${bin.lim}, $vpos, $mask -> $f")
    if(f < vlen) {
      val lfpos = vpos+f
      val s = emit(bin.pos, lfpos)
      bin.pos = lfpos+1
      mask &= ~(1L<<f)
      return s
    }
    vpos += vlen
    if(vpos <= bin.lim-vlen) mask = computeSimpleMask()
    else if(vpos < bin.lim) mask = computeMask()
    else if(buffer() == 0) return rest()
    readLine()
  }

  private[this] final def computeSimpleMask(): Long =
    ByteVector.fromArray(species, bin.buf, vpos).compare(VectorOperators.EQ, eolVector).toLong

  private[this] final def computeMask(): Long = {
    val excess = vpos+vlen-bin.lim
    val v =
      if(excess <= 0) ByteVector.fromArray(species, bin.buf, vpos)
      else ByteVector.fromArray(species, bin.buf, vpos, VectorMask.fromLong(species, fullMask >>> excess))
    // Seems to be slightly faster than comparing against a scalar:
    v.compare(VectorOperators.EQ, eolVector).toLong
  }
}

private sealed abstract class DirectVectorizedLineTokenizer(_bin: DirectBufferedInput, _eolChar: Byte, _preEolChar: Byte)
  extends DirectLineTokenizer(_bin, _eolChar, _preEolChar) {
  import VectorSupport._

  private[this] var vpos = start-vlen
  private[this] val limit = ms.byteSize()
  private[this] var mask = 0L
  private[this] val eolVector = ByteVector.broadcast(species, eolChar)

  override private[perfio] def markClosed(): Unit = {
    super.markClosed()
    mask = 0L
    vpos = Long.MaxValue - vlen
  }

  private[this] def rest(): String = {
    val r = if(vpos != Long.MaxValue) {
      val l = limit-start
      mask = 0
      if(l != 0) {
        val s = makeString(ms, start, l)
        start = limit
        s
      } else null
    } else {
      checkState()
      null
    }
    vpos = Long.MaxValue - vlen
    r
  }

  @tailrec final def readLine(): String = {
    val f = JLong.numberOfTrailingZeros(mask)
    if(f < vlen) {
      val lfpos = vpos+f
      val s = emit(start, lfpos)
      start = lfpos+1
      mask &= ~(1L<<f)
      return s
    }
    vpos += vlen
    if(vpos <= limit-vlen) mask = computeSimpleMask()
    else if(vpos < limit) mask = computeMask()
    else return rest()
    readLine()
  }

  @inline private[this] final def computeSimpleMask(): Long =
    ByteVector.fromMemorySegment(species, ms, vpos, ByteOrder.BIG_ENDIAN).compare(VectorOperators.EQ, eolVector).toLong

  private[this] final def computeMask(): Long = {
    val excess = vpos+vlen-limit
    val v =
      if(excess <= 0) ByteVector.fromMemorySegment(species, ms, vpos, ByteOrder.BIG_ENDIAN)
      else ByteVector.fromMemorySegment(species, ms, vpos, ByteOrder.BIG_ENDIAN, VectorMask.fromLong(species, fullMask >>> excess))
    // Seems to be slightly faster than comparing against a scalar:
    v.compare(VectorOperators.EQ, eolVector).toLong
  }
}
