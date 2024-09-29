package perfio

import java.lang.foreign.{MemorySegment, ValueLayout}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Path

/** MemorySegment-based version of ScalarLineTokenizer. */
object ForeignScalarLineTokenizer {
  /** Create a ForeignScalarLineTokenizer. See [[LineTokenizer.fromMemorySegment]] for details. */
  def fromMemorySegment(buf: MemorySegment, charset: Charset = StandardCharsets.UTF_8, closeable: AutoCloseable = null): ForeignScalarLineTokenizer =
    create(buf, closeable, charset)

  /** Create a ForeignScalarLineTokenizer. See [[LineTokenizer.fromMappedFile]] for details. */
  def fromMappedFile(file: Path, charset: Charset = StandardCharsets.UTF_8): ForeignScalarLineTokenizer =
    create(ForeignSupport.mapRO(file), null, charset)

  private[this] def create(buf: MemorySegment, closeable: AutoCloseable, cs: Charset): ForeignScalarLineTokenizer =
    if(cs eq StandardCharsets.ISO_8859_1)
      new ForeignScalarLineTokenizer(buf, closeable) {
        // Use the slightly faster constructor for Latin-1
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      }
    else
      new ForeignScalarLineTokenizer(buf, closeable) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, cs)
      }
}

abstract class ForeignScalarLineTokenizer(buf: MemorySegment, closeable: AutoCloseable) extends LineTokenizer {
  private[this] var pos, start = 0L
  private[this] val limit = buf.byteSize()
  private[this] var linebuf = new Array[Byte](1024)

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

  private[this] def emit(start: Long, lfpos: Long): String = {
    val end = if(lfpos > 0 && bufGet(lfpos-1) == '\r'.toByte) lfpos-1 else lfpos
    if(start == end) "" else makeString(buf, start, end-start)
  }

  private[this] def rest(): String = {
    if(start < pos) {
      val s = makeString(buf, start, pos-start)
      start = pos
      s
    } else null
  }

  final def readLine(): String = {
    var p = pos
    while(p < limit) {
      val b = bufGet(p)
      p += 1
      if(b == '\n'.toByte) {
        val s = emit(start, p-1)
        start = p
        pos = p
        return s
      }
    }
    pos = p
    rest()
  }

  def close(): Unit = if(closeable != null) closeable.close()
}