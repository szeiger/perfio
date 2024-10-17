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

  private[this] var vpos = start-vlen // start of the current vector in buf
  private[this] var mask = 0L // vector mask that marks the LFs

  private[this] def buffer(): Int = {
    if(vpos == Int.MaxValue || in == null) return 0
    val scanned = limit - start

    if(limit > buf.length-inputBufferHeadroom) {
      val shift = if(start % 32 == 0) start else start - (start % 32) // species.loopBound(start)
      if(shift > buf.length/2) {
        System.arraycopy(buf, start, buf, start-shift, (vpos min limit)-start)
        start -= shift
        limit -= shift
      } else buf = Arrays.copyOf(buf, buf.length*2)
    }
    val read = in.read(buf, limit, buf.length-limit)
    if(read > 0) { limit += read }

    if(limit-start == scanned) return 0

    vpos = start + scanned

    val offsetInVector = vpos % vlen
    //println(offsetInVector)
    // old limit was not at vector boundary -> rewind and mask the already read lanes
    if(offsetInVector != 0) {
      vpos -= offsetInVector
      val rem = offsetInVector
      //println(s"${vpos % vlen}, $rem, $offsetInVector")
      mask = computeMask() & (-1L >>> rem << rem)
    } else mask = computeMask()
    1
  }

  private[this] def rest(): String = {
    val r = if(vpos != Int.MaxValue) {
      val l = limit-start
      mask = 0
      if(l != 0) makeString(buf, start, l) else null
    } else null
    vpos = Int.MaxValue - vlen
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
      val lfpos = vpos+f
      val s = emit(start, lfpos)
      start = lfpos+1
      mask &= ~(1L<<f)
      return s
    }
    vpos += vlen
    if(vpos < limit) mask = computeMask()
    else if(buffer() == 0) return rest()
    readLine()
  }

  private[this] final def computeMask(): Long = {
    val overshoot = vpos+vlen-limit
    //println(pos % vlen)
    val v =
      if(overshoot <= 0) ByteVector.fromArray(species, buf, vpos)
      else ByteVector.fromArray(species, buf, vpos, VectorMask.fromLong(species, fullMask >>> overshoot))
    // Seems to be slightly faster than comparing against a scalar:
    v.compare(VectorOperators.EQ, lfs).toLong
  }

  def close(): Unit = if(in != null) in.close()
}











object VectorizedLineTokenizer2 {

  /** Create a VectorizedLineTokenizer2 from a BufferedInput. See [[LineTokenizer.apply]] for details. */
  def apply(in: BufferedInput, charset: Charset = StandardCharsets.UTF_8): VectorizedLineTokenizer2 = {
    if(charset eq StandardCharsets.ISO_8859_1)
      new VectorizedLineTokenizer2(in.asInstanceOf[HeapBufferedInput]) {
        // Use the slightly faster constructor for Latin-1
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      }
    else
      new VectorizedLineTokenizer2(in.asInstanceOf[HeapBufferedInput]) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, charset)
      }
  }
}

abstract class VectorizedLineTokenizer2(bin: HeapBufferedInput) extends LineTokenizer {
  import VectorSupport._

  private[this] var vpos = bin.pos-vlen // start of the current vector in buf
  private[this] var mask = 0L // vector mask that marks the LFs

  if(bin.pos < bin.lim) {
    // Make sure the position is aligned if we have buffered data.
    // Otherwise we leave the initial buffering to the main loop in readLine().
    vpos += vlen
    alignPosAndComputeMask()
  }

  private[this] def buffer(): Int = {
    //println(s"  buffer(): pos=${bin.pos}, lim=${bin.lim}, vpos=$vpos, buflen=${bin.buf.length}")
    if(vpos == Int.MaxValue) return 0
    val scanned = bin.lim - bin.pos
    val oldav = bin.available
    bin.prepareAndFillBuffer(oldav + vlen)
    //println(s"    $oldav -> ${bin.available}")
    if(oldav == bin.available) return 0
    vpos = bin.pos + scanned
    alignPosAndComputeMask()
    1
  }

  private[this] def alignPosAndComputeMask(): Unit = {
    val offsetInVector = vpos % vlen
    vpos -= offsetInVector
    mask = computeMask() & (-1L >>> offsetInVector << offsetInVector)
  }

  private[this] def rest(): String = {
    //println(s"rest: pos=$pos")
    val r = if(vpos != Int.MaxValue) {
      val l = bin.available
      mask = 0
      if(l != 0) makeString(bin.buf, bin.pos, l) else null
    } else null
    vpos = Int.MaxValue - vlen
    r
  }

  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String

  @inline private[this] def emit(start: Int, lfpos: Int): String = {
    val end = if(lfpos > 0 && bin.buf(lfpos-1) == '\r'.toByte) lfpos-1 else lfpos
    if(start == end) "" else makeString(bin.buf, start, end-start)
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
    if(vpos < bin.lim) mask = computeMask()
    else if(buffer() == 0) return rest()
    readLine()
  }

  private[this] final def computeMask(): Long = {
    val excess = vpos+vlen-bin.lim
    val v =
      if(excess <= 0) ByteVector.fromArray(species, bin.buf, vpos)
      else ByteVector.fromArray(species, bin.buf, vpos, VectorMask.fromLong(species, fullMask >>> excess))
    // Seems to be slightly faster than comparing against a scalar:
    v.compare(VectorOperators.EQ, lfs).toLong
  }

  def close(): Unit = bin.close()
}
