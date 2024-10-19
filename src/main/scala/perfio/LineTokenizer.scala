package perfio

import java.lang.foreign.{MemorySegment, ValueLayout}
import java.nio.charset.{Charset, StandardCharsets}

/**
 * Read text from a [[BufferedInput]] and split it into lines. If the input ends with a newline, no
 * additional empty line is returned (same as [[java.io.BufferedReader]]).
 *
 * Parsing is performed on raw byte data so it only works for charsets that are compatible with
 * ASCII line splitting. These include UTF-8, ASCII, the ISO-8859 family and other 8-bit
 * ASCII-based charsets. This avoids the inefficiency of more general parsers (like
 * [[java.io.BufferedInputStream]]) of first decoding the entire input to 16-bit chars and then
 * attempting to compress it back to bytes when creating the Strings.
 *
 * With the default settings for `eol` and `preEol` a LineTokenizer will recognize both LF (Unix) and CRLF (Windows)
 * line endings. Automatic recognition of pure CR line endings (classic MacOS) at the same time is not supported but
 * can be configured manually with `eol = '\r', preEol = -1`.
 */
sealed abstract class LineTokenizer(protected[this] val eolChar: Byte, protected[this] val preEolChar: Byte) extends AutoCloseable {
  /** Returns the next line of text, or null if the end of the input has been reached. */
  def readLine(): String
}

object LineTokenizer {
  /**
   * Create a [[VectorizedLineTokenizer]] or [[ScalarLineTokenizer]] depending on JVM and hardware support.
   *
   * @param in        BufferedInput to parse starting at its current position
   * @param charset   Charset for decoding strings. Decoding is applied to individual lines after splitting.
   *                  Default: UTF-8
   * @param eol       End-of-line character. Default: LF (\n)
   * @param preEol    Optional character that is removed if it occurs before an EOL. Default: CR (\r). Set to -1 to
   *                  disable this feature.
   */
  def apply(in: BufferedInput, charset: Charset = StandardCharsets.UTF_8, eol: Byte = '\n'.toByte, preEol: Byte = '\r'.toByte): LineTokenizer =
    if(VectorSupport.isEnabled) VectorizedLineTokenizer(in, charset, eol, preEol)
    else ScalarLineTokenizer(in, charset, eol, preEol)
}

private[perfio] abstract class HeapLineTokenizer(
  private[perfio] val parentBin: HeapBufferedInput, _eolChar: Byte, _preEolChar: Byte) extends LineTokenizer(_eolChar, _preEolChar) {
  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String
  protected[this] val bin: HeapBufferedInput = parentBin.identicalView().asInstanceOf[HeapBufferedInput]

  protected[this] def emit(start: Int, lfpos: Int): String = {
    val end = if(lfpos > 0 && preEolChar != (-1).toByte && bin.buf(lfpos-1) == preEolChar) lfpos-1 else lfpos
    if(start == end) "" else makeString(bin.buf, start, end-start)
  }

  def close(): Unit = parentBin.close()
}

private[perfio] abstract class DirectLineTokenizer(
  private[perfio] val bin: DirectBufferedInput, _eolChar: Byte, _preEolChar: Byte) extends LineTokenizer(_eolChar, _preEolChar) {
  private[this] var linebuf = new Array[Byte](256)
  protected[this] val ms = bin.ms
  protected[this] var start = bin.bbStart + bin.pos
  bin.lock()

  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String

  private[this] def extendBuffer(len: Int): Array[Byte] = {
    var buflen = linebuf.length
    while(buflen < len) buflen *= 2
    new Array[Byte](buflen)
  }

  protected[this] def makeString(buf: MemorySegment, start: Long, llen: Long): String = {
    val len = llen.toInt
    if(linebuf.length < len) linebuf = extendBuffer(len)
    MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, start, linebuf, 0, len)
    makeString(linebuf, 0, len)
  }

  protected[this] def emit(start: Long, lfpos: Long): String = {
    val end = if(lfpos > 0 && preEolChar != (-1).toByte && ms.get(ValueLayout.JAVA_BYTE, lfpos-1) == preEolChar) lfpos-1 else lfpos
    if(start == end) "" else makeString(ms, start, end-start)
  }

  def close(): Unit = {
    bin.unlock()
    bin.close()
  }
}
