package perfio

import java.lang.foreign.{MemorySegment, ValueLayout}
import java.nio.charset.{Charset, StandardCharsets}

/**
 * Read text from a [[BufferedInput]] and split it into lines. If the input ends with a newline, no
 * additional empty line is returned (same as [[java.io.BufferedReader]]).
 *
 * This method creates a [[VectorizedLineTokenizer]] or [[ScalarLineTokenizer]] depending on JVM and hardware support.
 *
 * Parsing is performed on raw byte data so it only works for charsets that are compatible with
 * ASCII line splitting. These include UTF-8, ASCII, the ISO-8859 family and other 8-bit
 * ASCII-based charsets. This avoids the inefficiency of more general parsers (like
 * [[java.io.BufferedInputStream]]) of first decoding the entire input to 16-bit chars and then
 * attempting to compress it back to bytes when creating the Strings.
 *
 * Pure CR line endings (classic MacOS) are not supported, only LF (Unix) and CRLF (Windows).
 */
sealed abstract class LineTokenizer extends AutoCloseable {
  /** Returns the next line of text, or null if the end of the input has been reached. */
  def readLine(): String
}

object LineTokenizer {
  /**
   * Create a [[VectorizedLineTokenizer]] or [[ScalarLineTokenizer]] depending on JVM and hardware support.
   */
  def apply(in: BufferedInput, charset: Charset = StandardCharsets.UTF_8): LineTokenizer =
    if(VectorSupport.isEnabled) VectorizedLineTokenizer(in, charset) else ScalarLineTokenizer(in, charset)
}

private[perfio] abstract class HeapLineTokenizer(private[perfio] val bin: HeapBufferedInput) extends LineTokenizer {
  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String

  protected[this] def emit(start: Int, lfpos: Int): String = {
    val end = if(lfpos > 0 && bin.buf(lfpos-1) == '\r'.toByte) lfpos-1 else lfpos
    if(start == end) "" else makeString(bin.buf, start, end-start)
  }

  def close(): Unit = bin.close()
}

private[perfio] abstract class DirectLineTokenizer(private[perfio] val bin: DirectBufferedInput) extends LineTokenizer {
  private[this] var linebuf = new Array[Byte](1024)
  protected[this] val ms = bin.ms

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
    val end = if(lfpos > 0 && ms.get(ValueLayout.JAVA_BYTE, lfpos-1) == '\r'.toByte) lfpos-1 else lfpos
    if(start == end) "" else makeString(ms, start, end-start)
  }

  def close(): Unit = bin.close()
}
