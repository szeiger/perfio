package perfio

import java.lang.foreign.{MemorySegment, ValueLayout}
import java.nio.ByteBuffer
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






//object ForeignScalarLineTokenizer2 {
//  /** Create a ForeignScalarLineTokenizer2 from a BufferedInput. See [[LineTokenizer.apply]] for details. */
//  def apply(in: BufferedInput, charset: Charset = StandardCharsets.UTF_8): ForeignScalarLineTokenizer2 = {
//    if(charset eq StandardCharsets.ISO_8859_1)
//      new ForeignScalarLineTokenizer2(in.asInstanceOf[DirectBufferedInput]) {
//        // Use the slightly faster constructor for Latin-1
//        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
//      }
//    else
//      new ForeignScalarLineTokenizer2(in.asInstanceOf[DirectBufferedInput]) {
//        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, charset)
//      }
//  }
//}
//
//abstract class ForeignScalarLineTokenizer2(bin: DirectBufferedInput) extends LineTokenizer {
//  private[this] var linebuf = new Array[Byte](1024)
//
//  private[this] def extendBuffer(len: Int): Array[Byte] = {
//    var buflen = linebuf.length
//    while(buflen < len) buflen *= 2
//    new Array[Byte](buflen)
//  }
//
//  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String
//
//  protected[this] def makeString(start: Int, len: Int): String = {
//    if(linebuf.length < len) linebuf = extendBuffer(len)
//    //MemorySegment.copy(bin.bbSegment, ValueLayout.JAVA_BYTE, start, linebuf, 0, len)
//    bin.bb.get(start, linebuf, 0, len)
//    makeString(linebuf, 0, len)
//  }
//
//  @inline private[this] def bufGet(i: Int): Byte = bin.bbSegment.get(ValueLayout.JAVA_BYTE, i.toLong)
//
//  private[this] def emit(start: Int, p: Int): String = {
//    val lfpos = p-1
//    bin.pos = p
//    val end = if(lfpos > 0 && bufGet(lfpos-1) == '\r'.toByte) lfpos-1 else lfpos
//    if(start == end) "" else makeString(start, end-start)
//  }
//
//  private[this] def rest(p: Int): String =
//    if(bin.pos < p) {
//      val s = makeString(bin.pos, p-bin.pos)
//      bin.pos = p
//      s
//    } else null
//
//  final def readLine(): String = {
//    var bp, p = bin.pos
//    while(true) {
//      while(p < bin.lim) {
//        val b = bufGet(p)
//        p += 1
//        if(b == '\n'.toByte) return emit(bp, p)
//      }
//      val done = rebuffer()
//      p -= bp - bin.pos
//      if(done) return rest(p)
//      bp = bin.pos
//    }
//    null // unreachable
//  }
//
//  private[this] def rebuffer(): Boolean = {
//    val oldav = bin.available
//    bin.prepareAndFillBuffer(1)
//    oldav == bin.available
//  }
//
//  def close(): Unit = bin.close()
//}





object ForeignScalarLineTokenizer2 {
  /** Create a ForeignScalarLineTokenizer2 from a BufferedInput. See [[LineTokenizer.apply]] for details. */
  def apply(in: BufferedInput, charset: Charset = StandardCharsets.UTF_8): ForeignScalarLineTokenizer2 = {
    if(charset eq StandardCharsets.ISO_8859_1)
      new ForeignScalarLineTokenizer2(in.asInstanceOf[DirectBufferedInput]) {
        // Use the slightly faster constructor for Latin-1
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      }
    else
      new ForeignScalarLineTokenizer2(in.asInstanceOf[DirectBufferedInput]) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, charset)
      }
  }
}

abstract class ForeignScalarLineTokenizer2(in: DirectBufferedInput) extends LineTokenizer {
  private[this] var pos, start = in.bbStart + in.pos
  private[this] val limit = in.totalReadLimit
  private[this] var linebuf = new Array[Byte](1024)
  private[this] val ms = in.ms

  @inline private[this] def bufGet(l: Long): Byte = ms.get(ValueLayout.JAVA_BYTE, l)

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
    if(start == end) "" else makeString(ms, start, end-start)
  }

  private[this] def rest(): String = {
    if(start < pos) {
      val s = makeString(ms, start, pos-start)
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

  def close(): Unit = in.close()
}
