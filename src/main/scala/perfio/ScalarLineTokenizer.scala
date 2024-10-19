package perfio

import java.lang.foreign.ValueLayout
import java.nio.charset.{Charset, StandardCharsets}

object ScalarLineTokenizer {
  def apply(in: BufferedInput, charset: Charset = StandardCharsets.UTF_8, eol: Byte = '\n'.toByte, preEol: Byte = '\r'.toByte): LineTokenizer = in match {
    case in: HeapBufferedInput =>
      if(charset eq StandardCharsets.ISO_8859_1) new HeapScalarLineTokenizer(in, eol, preEol) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      } else new HeapScalarLineTokenizer(in, eol, preEol) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, charset)
      }
    case in: DirectBufferedInput =>
      if(charset eq StandardCharsets.ISO_8859_1) new DirectScalarLineTokenizer(in, eol, preEol) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      } else new DirectScalarLineTokenizer(in, eol, preEol) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, charset)
      }
  }
}

private sealed abstract class HeapScalarLineTokenizer(_bin: HeapBufferedInput, _eolChar: Byte, _preEolChar: Byte)
  extends HeapLineTokenizer(_bin, _eolChar, _preEolChar) {

  private[this] def rest(p: Int): String =
    if(bin.pos < p) {
      val s = makeString(bin.buf, bin.pos, p-bin.pos)
      bin.pos = p
      s
    } else null

  final def readLine(): String = {
    var bp, p = bin.pos
    while(true) {
      while(p < bin.lim) {
        val b = bin.buf(p)
        p += 1
        if(b == eolChar) {
          bin.pos = p
          return emit(bp, p-1)
        }
      }
      val done = rebuffer()
      p -= bp - bin.pos
      if(done) return rest(p)
      bp = bin.pos
    }
    null // unreachable
  }

  private[this] def rebuffer(): Boolean = {
    val oldav = bin.available
    bin.prepareAndFillBuffer(1)
    oldav == bin.available
  }
}

private sealed abstract class DirectScalarLineTokenizer(_bin: DirectBufferedInput, _eolChar: Byte, _preEolChar: Byte)
  extends DirectLineTokenizer(_bin, _eolChar, _preEolChar) {
  private[this] var pos, start = bin.bbStart + bin.pos
  private[this] val limit = bin.totalReadLimit

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
      val b = ms.get(ValueLayout.JAVA_BYTE, p)
      p += 1
      if(b == eolChar) {
        val s = emit(start, p-1)
        start = p
        pos = p
        return s
      }
    }
    pos = p
    rest()
  }
}
