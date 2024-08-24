package perfio

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Arrays
import scala.annotation.tailrec

object ScalarLineTokenizer {
  /** Create a ScalarLineTokenizer from an InputStream. See [[LineTokenizer.apply]] for details. */
  def apply(in: InputStream, charset: Charset = StandardCharsets.UTF_8, initialBufferSize: Int = 32768,
    minRead: Int = -1): ScalarLineTokenizer = {
    val buf = new Array[Byte](initialBufferSize)
    val inputBufferHeadroom = if(minRead > 0) minRead else buf.length/2
    create(buf, 0, 0, in, inputBufferHeadroom, charset)
  }

  /** Create a ScalarLineTokenizer from an array of bytes. See [[LineTokenizer.fromArray]] for details. */
  def fromArray(buf: Array[Byte], offset: Int = 0, length: Int = -1, charset: Charset = StandardCharsets.UTF_8): ScalarLineTokenizer =
    create(buf, offset, if(length == -1) buf.length-offset else length, null, 0, charset)

  private[this] def create(buf: Array[Byte], start: Int, limit: Int, in: InputStream, inputBufferHeadroom: Int, cs: Charset): ScalarLineTokenizer =
    if(cs eq StandardCharsets.ISO_8859_1)
      new ScalarLineTokenizer(buf, start, limit, in, inputBufferHeadroom) {
        // Use the slightly faster constructor for Latin-1
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, 0, start, len)
      }
    else
      new ScalarLineTokenizer(buf, start, limit, in, inputBufferHeadroom) {
        protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String = new String(buf, start, len, cs)
      }
}

abstract class ScalarLineTokenizer(
  private[this] var buf: Array[Byte],
  private[this] var start: Int,
  private[this] var limit: Int,
  in: InputStream, inputBufferHeadroom: Int
) extends LineTokenizer {
  private[this] var pos = start

  private[this] def buffer(): Int = {
    if(limit > buf.length-inputBufferHeadroom) {
      if(start > buf.length/2) {
        System.arraycopy(buf, start, buf, 0, pos-start)
        pos -= start
        limit -= start
        start = 0
      } else buf = Arrays.copyOf(buf, buf.length*2)
    }
    if(in != null) in.read(buf, pos, buf.length-pos) else 0
  }

  protected[this] def makeString(buf: Array[Byte], start: Int, len: Int): String

  private[this] def emit(start: Int, lfpos: Int): String = {
    val end = if(lfpos > 0 && buf(lfpos-1) == '\r'.toByte) lfpos-1 else lfpos
    if(start == end) "" else makeString(buf, start, end-start)
  }

  private[this] def rest(): String = {
    if(start < pos) {
      val s = makeString(buf, start, pos-start)
      start = pos
      s
    } else null
  }

  @tailrec final def readLine(): String = {
    var p = pos
    while(p < limit) {
      val b = buf(p)
      p += 1
      if(b == '\n'.toByte) {
        val s = emit(start, p-1)
        start = p
        pos = p
        return s
      }
    }
    pos = p
    val read = buffer()
    if(read <= 0) return rest()
    limit += read
    readLine()
  }

  def close(): Unit = if(in != null) in.close()
}
