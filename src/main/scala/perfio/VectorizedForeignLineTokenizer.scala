package perfio

import jdk.incubator.vector.{ByteVector, VectorMask, VectorOperators}

import java.lang.foreign.{MemorySegment, ValueLayout}
import java.lang.{Long => JLong}
import java.nio.ByteOrder
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Path
import scala.annotation.tailrec

/** MemorySegment-based version of VectorizedLineTokenizer. */
object VectorizedForeignLineTokenizer {
  /** Create a VectorizedForeignLineTokenizer. See [[LineTokenizer.fromMemorySegment]] for details. */
  def fromMemorySegment(buf: MemorySegment, charset: Charset = StandardCharsets.UTF_8, closeable: AutoCloseable = null): VectorizedForeignLineTokenizer =
    create(buf, closeable, charset)

  /** Create a VectorizedForeignLineTokenizer. See [[LineTokenizer.fromMappedFile]] for details. */
  def fromMappedFile(file: Path, charset: Charset = StandardCharsets.UTF_8): VectorizedForeignLineTokenizer =
    create(ForeignSupport.mapRO(file), null, charset)

  private[this] def create(buf: MemorySegment, closeable: AutoCloseable, cs: Charset): VectorizedForeignLineTokenizer =
    if(cs eq StandardCharsets.ISO_8859_1)
      new VectorizedForeignLineTokenizer(buf, closeable) {
        // Use the slightly faster constructor for Latin-1
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      }
    else
      new VectorizedForeignLineTokenizer(buf, closeable) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, cs)
      }
}

abstract class VectorizedForeignLineTokenizer private (buf: MemorySegment, closeable: AutoCloseable) extends LineTokenizer {
  import VectorSupport._

  private[this] var start = 0L
  private[this] var pos = start-vlen
  private[this] val limit = buf.byteSize()
  private[this] var linebuf = new Array[Byte](1024)
  private[this] var mask = 0L

  @inline private[this] def bufGet(l: Long): Byte = buf.get(ValueLayout.JAVA_BYTE, l)

  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String

  private[this] def extendBuffer(len: Int): Array[Byte] = {
    var buflen = linebuf.length
    while(buflen < len) buflen *= 2
    new Array[Byte](buflen)
  }

  private[this] def makeString(buf: MemorySegment, start: Long, llen: Long): String = {
    val len = llen.toInt
    if(linebuf.length < len) linebuf = extendBuffer(len)
    MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, start, linebuf, 0, len)
    makeString(linebuf, 0, len)
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

  @inline private[this] def emit(start: Long, lfpos: Long): String = {
    val end = if(lfpos > 0 && bufGet(lfpos-1) == '\r'.toByte) lfpos-1 else lfpos
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
    else return rest()
    readLine()
  }

  private[this] final def computeMask(): Long = {
    val overshoot = pos+vlen-limit
    val v =
      if(overshoot <= 0) ByteVector.fromMemorySegment(species, buf, pos, ByteOrder.BIG_ENDIAN)
      else ByteVector.fromMemorySegment(species, buf, pos, ByteOrder.BIG_ENDIAN, VectorMask.fromLong(species, fullMask >>> overshoot))
    // Seems to be slightly faster than comparing against a scalar:
    v.compare(VectorOperators.EQ, lfs).toLong
  }

  def close(): Unit = if(closeable != null) closeable.close()
}
