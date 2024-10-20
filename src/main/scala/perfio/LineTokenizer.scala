package perfio

import java.io.IOException
import java.lang.foreign.{MemorySegment, ValueLayout}

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
  protected[this] var closed: Boolean = false

  /** Returns the next line of text, or null if the end of the input has been reached. */
  def readLine(): String

  /** Close this LineTokenizer without closing the underlying BufferedInput. The BufferedInput continues reading
   * from the current position of this LineTokenizer (i.e. directly after the last end-of-line character that was
   * read). A subsequent call to [[close]] has no effect.
   *
   * @return The underlying BufferedInput.
   */
  def end(): BufferedInput

  protected[this] def checkState(): Unit =
    if(closed) throwClosed

  private[this] def throwClosed: Nothing = throw new IOException("LineTokenizer has already been closed")

  /** Prevent reuse of this view. This ensures that it stays closed when a new view or LineTokenizer is created from
   *  the parent BufferedInput. */
  def detach(): LineTokenizer

  private[perfio] def markClosed(): Unit = {
    closed = true
  }
}


private abstract class HeapLineTokenizer(
  private[perfio] val parentBin: HeapBufferedInput, _eolChar: Byte, _preEolChar: Byte) extends LineTokenizer(_eolChar, _preEolChar) with CloseableView {
  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String
  protected[this] val bin: HeapBufferedInput = parentBin.identicalView().asInstanceOf[HeapBufferedInput]
  bin.closeableView = this

  // The view is already positioned at the start of the line so we can simply return control to the parent
  def end(): BufferedInput = {
    if(!closed) {
      bin.close()
      markClosed()
    }
    parentBin
  }

  override private[perfio] def markClosed(): Unit = {
    super.markClosed()
    bin.closeableView = null
  }

  def detach(): LineTokenizer = {
    bin.detach()
    this
  }

  protected[this] def emit(start: Int, lfpos: Int): String = {
    val end = if(lfpos > 0 && preEolChar != (-1).toByte && bin.buf(lfpos-1) == preEolChar) lfpos-1 else lfpos
    if(start == end) "" else makeString(bin.buf, start, end-start)
  }

  def close(): Unit = {
    if(!closed) {
      parentBin.close()
      markClosed()
    }
  }
}


private abstract class DirectLineTokenizer(
  private[perfio] val bin: DirectBufferedInput, _eolChar: Byte, _preEolChar: Byte) extends LineTokenizer(_eolChar, _preEolChar) with CloseableView {
  private[this] var linebuf = new Array[Byte](256)
  protected[this] val ms = bin.ms
  protected[this] var start = bin.bbStart + bin.pos
  bin.lock()
  bin.closeableView = this

  def end(): BufferedInput = {
    if(!closed) {
      bin.reposition(start)
      bin.unlock()
      markClosed()
    }
    bin
  }

  override private[perfio] def markClosed(): Unit = {
    super.markClosed()
    bin.closeableView = null
  }

  def detach(): LineTokenizer = this

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
    if(!closed) {
      bin.unlock()
      bin.close()
      markClosed()
    }
  }
}
