package perfio

import jdk.incubator.vector.ByteVector

import java.io.{Closeable, InputStream}
import java.lang.foreign.MemorySegment
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Path
import scala.sys.BooleanProp

/** Return lines of text from an input. */
abstract class LineTokenizer extends Closeable {
  /** Returns the next line of text, or null if the end of the input has been reached. */
  def readLine(): String
}

object LineTokenizer {
  /**
   * Read text from an [[InputStream]] and split it into lines. If the input ends with a newline, no
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
   *
   * @param in                InputStream from which to read data.
   * @param charset           Charset for decoding the strings. Must be compatible with ASCII line splitting.
   *                          The behavior for incompatible charsets is undefined.
   * @param initialBufferSize Initial size of the internal buffer. It is automatically extended if it is too short for
   *                          a single line.
   * @param minRead           Approximate minimum block size to read from the InputStream. The default is half the
   *                          initialBufferSize.
   * @param streaming         Do not block until the input can be fully buffered. This is necessary when dealing with
   *                          streaming data (e.g. reading from stdin), but reading very small blocks of data that are
   *                          not aligned with vector boundaries is inefficient. Disable it for better performance when
   *                          reading from an InputStream that cannot read large blocks if streaming is not required.
   *                          This is only relevant for the SIMD implementation. The scalar version always supports
   *                          streaming and ignores this flag.
   */
  def apply(in: InputStream, charset: Charset = StandardCharsets.UTF_8, initialBufferSize: Int = 32768,
    minRead: Int = -1, streaming: Boolean = true): LineTokenizer =
    if(useVectorized) VectorizedLineTokenizer(in, charset, initialBufferSize, minRead, streaming)
    else ScalarLineTokenizer(in, charset, initialBufferSize, minRead)

  /** Read text from (part of) an array of bytes and split it into lines. See [[apply]] for details.
   *
   * @param buf               Array from which to read data.
   * @param offset            Starting point within the array.
   * @param length            Length of the data within the array, or -1 to read until the end of the array.
   * @param charset           Charset for decoding the strings. Must be compatible with ASCII line splitting.
   *                          The behavior for incompatible charsets is undefined.
   */
  def fromArray(buf: Array[Byte], offset: Int = 0, length: Int = -1, charset: Charset = StandardCharsets.UTF_8): LineTokenizer =
    if(useVectorized) VectorizedLineTokenizer.fromArray(buf, offset, length, charset)
    else ScalarLineTokenizer.fromArray(buf, offset, length, charset)

  /** Read text from a MemorySegment and split it into lines. See [[apply]] for details.
   *
   * @param buf               MemorySegment from which to read data.
   * @param charset           Charset for decoding the strings. Must be compatible with ASCII line splitting.
   *                          The behavior for incompatible charsets is undefined.
   */
  def fromMemorySegment(buf: MemorySegment, charset: Charset = StandardCharsets.UTF_8): LineTokenizer =
    if(useVectorized) VectorizedForeignLineTokenizer.fromMemorySegment(buf, charset)
    else ScalarForeignLineTokenizer.fromMemorySegment(buf, charset)

  /** Read text from a memory-mapped file and split it into lines. See [[apply]] for details.
   *
   * @param file              File from which to read data.
   * @param charset           Charset for decoding the strings. Must be compatible with ASCII line splitting.
   *                          The behavior for incompatible charsets is undefined.
   */
  def fromMappedFile(file: Path, charset: Charset = StandardCharsets.UTF_8): LineTokenizer =
    if(useVectorized) VectorizedForeignLineTokenizer.fromMappedFile(file, charset)
    else ScalarForeignLineTokenizer.fromMappedFile(file, charset)

  private[this] var useVectorized: Boolean = {
    // Just a sanity check. We accept any reasonable size. Even 64-bit vectors (SWAR) are faster than scalar.
    // Hopefully this will guarantee that the preferred species is actually vectorized (which is not the case
    // with the experimental preview API at the moment).
    try ByteVector.SPECIES_PREFERRED.length() >= 8 && !BooleanProp.valueIsTrue("perfio.disableVectorized")
    catch { case _: NoClassDefFoundError => false }
  }
}
