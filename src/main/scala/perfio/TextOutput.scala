package perfio

import java.io.{EOFException, Flushable}
import java.lang.invoke.MethodHandles
import java.nio.{ByteBuffer, ByteOrder, CharBuffer}
import java.nio.charset.{Charset, CodingErrorAction, StandardCharsets}

object TextOutput {
  /** Create a TextOutput for a given BufferedOutput.
   *
   * @param out The underlying BufferedOutput
   * @param cs The Charset for encoding text. ASCII-compatible standard Charsets (`StandardCharsets.ISO_8859_1`,
   *           `StandardCharsets.UTF_8`, `StandardCharsets.US_ASCII`) are specially optimized and much faster than
   *           the generic implementation for other Charsets (which is still significantly faster than
   *           `java.io.PrintWriter` because it can write directly to a BufferedOutput).
   * */
  def apply(out: BufferedOutput, cs: Charset, eol: String = System.lineSeparator, autoFlush: Boolean = false): TextOutput =
    if(cs eq StandardCharsets.ISO_8859_1) new Latin1TextOutput(out, eol.getBytes(cs), autoFlush)
    else if(cs eq StandardCharsets.UTF_8) new UTF8TextOutput(out, eol.getBytes(cs), autoFlush)
    else if(cs eq StandardCharsets.US_ASCII) new ASCIITextOutput(out, eol.getBytes(cs), autoFlush)
    else new GenericTextOutput(out, cs, eol, autoFlush)
}

/** TextOutput prints formatted text to a BufferedOutput. It generally behaves like java.io.PrintWriter except that it
 * does not swallow IOExceptions. They are thrown immediately like in BufferedOutput/OutputStream/etc. Most methods
 * return `this` for convenient chaining of operations. Unlike PrintWriter/PrintStream it allows the end-of-line
 * sequence to be changed from the system default.
 *
 * This class is not thread-safe. Unless external synchronization is used, an instance may only be accessed from a
 * single thread (which must be the same thread that is used for accessing the underlying BufferedOutput).
 *
 * TextOutput does not perform any internal buffering so you can mix calls to a TextOutput and the underlying
 * BufferedOutput instance. The only general exception to this is when a surrogate pair is split (i.e. the high and
 * low surrogate character are written in 2 separate `print(String)` or `print(Char)` calls), but more esoteric
 * Charset implementations may contain buffering of their own.
 */
abstract class TextOutput(protected val out: BufferedOutput, protected val cs: Charset, protected val autoFlush: Boolean) extends AutoCloseable with Flushable {
  protected[this] val surrogateCharBuf = CharBuffer.allocate(2)
  protected[this] var hasSurrogate = false

  protected def printRaw(s: String): Unit
  protected def printlnRaw(s: String): Unit
  protected def printlnRaw(): Unit
  protected def printRawNull(): Unit
  protected def printlnRawNull(): Unit

  protected final def maybeFlush(): Unit = if(autoFlush) out.flush()

  final def println(): this.type = {
    printlnRaw()
    maybeFlush()
    this
  }

  final def print(s: String): this.type = {
    if(s == null) printRawNull()
    else printRaw(s)
    this
  }
  final def println(s: String): this.type = {
    if(s == null) printlnRawNull()
    else printlnRaw(s)
    maybeFlush()
    this
  }

  final def print(o: Any): this.type = {
    if(o == null) printRawNull()
    else printRaw(String.valueOf(o))
    this
  }

  final def println(o: Any): this.type = {
    if(o == null) printlnRawNull()
    else printlnRaw(String.valueOf(o))
    maybeFlush()
    this
  }

  def print(i: Int): this.type = {
    printRaw(Integer.toString(i))
    this
  }
  def println(i: Int): this.type = {
    printlnRaw(Integer.toString(i))
    maybeFlush()
    this
  }

  def print(b: Boolean): this.type = {
    printRaw(String.valueOf(b))
    this
  }
  def println(b: Boolean): this.type = {
    printlnRaw(String.valueOf(b))
    maybeFlush()
    this
  }

  def print(l: Long): this.type = {
    printRaw(java.lang.Long.toString(l))
    this
  }
  def println(l: Long): this.type = {
    printlnRaw(java.lang.Long.toString(l))
    maybeFlush()
    this
  }

  def print(c: Char): this.type = {
    printRaw(String.valueOf(c))
    this
  }
  def println(c: Char): this.type = {
    printlnRaw(String.valueOf(c))
    maybeFlush()
    this
  }

  def print(s: Array[Char]): this.type = {
    printRaw(new String(s))
    this
  }
  def println(s: Array[Char]): this.type = {
    printlnRaw(new String(s))
    maybeFlush()
    this
  }

  def print(f: Float): this.type = {
    printRaw(String.valueOf(f))
    this
  }
  def println(f: Float): this.type = {
    printlnRaw(String.valueOf(f))
    maybeFlush()
    this
  }

  def print(d: Double): this.type = {
    printRaw(String.valueOf(d))
    this
  }
  def println(d: Double): this.type = {
    printlnRaw(String.valueOf(d))
    maybeFlush()
    this
  }

  def flush(): Unit = out.flush()

  def close(): Unit = out.close()
}


/** TextOutput implementation for ASCII-compatible charsets. All ASCII characters are encoded as a single ASCII byte. */
private class ASCIICompatibleTextOutput(_out: BufferedOutput, _cs: Charset, protected val eol: Array[Byte], _autoFlush: Boolean) extends TextOutput(_out, _cs, _autoFlush) {
  protected[this] val eolLen = eol.length
  private[this] val eol0: Byte = eol(0)
  private[this] val eol1: Byte = if(eolLen > 1) eol(1) else 0.toByte
  private[this] val maxSafeChar = if(cs eq StandardCharsets.ISO_8859_1) 255 else 127

  protected def unsafeWriteEOL(p: Int): Unit = {
    eolLen match {
      case 1 => out.buf(p) = eol0
      case 2 => out.buf(p) = eol0; out.buf(p+1) = eol1
      case n => System.arraycopy(eol, 0, out.buf, p, n)
    }
  }

  private[this] def flushSurrogate(): Unit =
    if(hasSurrogate) {
      out.int8('?'.toByte)
      hasSurrogate = false
    }

  protected def printRaw(s: String): Unit = {
    if(!s.isEmpty) {
      if(hasSurrogate || Character.isHighSurrogate(s.charAt(s.length-1))) printRawWithSurrogates(s)
      else out.write(s.getBytes(cs))
    }
  }

  private[this] def printRawWithSurrogates(s: String): Unit = {
    val len = s.length
    val startOff = if(Character.isLowSurrogate(s.charAt(0))) 1 else 0
    val endOff = if(Character.isHighSurrogate(s.charAt(len-1))) 1 else 0
    if(startOff != 0 && hasSurrogate) {
      surrogateCharBuf.put(1, s.charAt(0)).position(0)
      val bb = cs.encode(surrogateCharBuf)
      out.write(bb.array(), 0, bb.limit())
    } else if(startOff != 0 || hasSurrogate) {
      out.int8('?'.toByte)
    }
    val v = s.substring(startOff, len-endOff)
    if(v.length > 0) out.write(v.getBytes(cs))
    if(endOff != 0) {
      hasSurrogate = true
      surrogateCharBuf.put(0, s.charAt(len-1))
    } else hasSurrogate = false
  }

  protected def printlnRaw(s: String): Unit = {
    if(!s.isEmpty) {
      if(hasSurrogate || Character.isHighSurrogate(s.charAt(s.length-1))) printlnRawWithSurrogates(s)
      else {
        val b = s.getBytes(cs)
        val l = b.length
        val p = out.fwd(l + eolLen)
        if(l > 0) System.arraycopy(b, 0, out.buf, p, l)
        unsafeWriteEOL(p + l)
      }
    } else printlnRaw()
  }

  private[this] def printlnRawWithSurrogates(s: String): Unit = {
    printRawWithSurrogates(s)
    printlnRaw()
  }

  protected def printlnRaw(): Unit = {
    flushSurrogate()
    val p = out.fwd(eolLen)
    unsafeWriteEOL(p)
  }

  protected def printRawNull(): Unit = {
    flushSurrogate()
    val p = out.fwd(4)
    unsafeWriteNull(p)
  }

  protected def printlnRawNull(): Unit = {
    flushSurrogate()
    val p = out.fwd(4 + eolLen)
    unsafeWriteNull(p)
    unsafeWriteEOL(p + 4)
  }

  override def print(i: Int): this.type = {
    flushSurrogate()
    val len = TextOutputUtil.numChars(i)
    val p = out.fwd(len)
    unsafeWriteInt(i, p, len)
    this
  }

  override def println(i: Int): this.type = {
    flushSurrogate()
    val len = TextOutputUtil.numChars(i)
    val p = out.fwd(len + eolLen)
    unsafeWriteInt(i, p, len)
    unsafeWriteEOL(p + len)
    maybeFlush()
    this
  }

  override def print(l: Long): this.type = {
    flushSurrogate()
    val len = TextOutputUtil.numChars(l)
    val p = out.fwd(len)
    unsafeWriteLong(l, p, len)
    this
  }

  override def println(l: Long): this.type = {
    flushSurrogate()
    val len = TextOutputUtil.numChars(l)
    val p = out.fwd(len + eolLen)
    unsafeWriteLong(l, p, len)
    unsafeWriteEOL(p + len)
    maybeFlush()
    this
  }

  override def print(b: Boolean): this.type = {
    flushSurrogate()
    if(b) {
      val p = out.fwd(4)
      unsafeWriteTrue(p)
    } else {
      val p = out.fwd(5)
      unsafeWriteFalse(p)
    }
    this
  }
  override def println(b: Boolean): this.type = {
    flushSurrogate()
    if(b) {
      val p = out.fwd(4 + eolLen)
      unsafeWriteTrue(p)
      unsafeWriteEOL(p + 4)
    } else {
      val p = out.fwd(5 + eolLen)
      unsafeWriteFalse(p)
      unsafeWriteEOL(p + 5)
    }
    maybeFlush()
    this
  }

  private[this] def unsafeWriteTrue(p: Int): Unit =
    TextOutputUtil.INT_NATIVE.set(out.buf, p, TextOutputUtil.LIT_TRUE)

  private[this] def unsafeWriteFalse(p: Int): Unit = {
    TextOutputUtil.INT_NATIVE.set(out.buf, p, TextOutputUtil.LIT_FALS)
    out.buf(p + 4) = 'e'.toByte
  }

  private[this] def unsafeWriteNull(p: Int): Unit =
    TextOutputUtil.INT_NATIVE.set(out.buf, p, TextOutputUtil.LIT_NULL)

  private[this] def unsafeWriteInt(_i: Int, p: Int, len: Int): Unit = {
    var i = _i
    if(i < 0) out.buf(p) = '-'.toByte
    else i = -i
    var j = p + len - 2
    while(i <= -100) {
      val n = i % 100
      TextOutputUtil.SHORT_NATIVE.set(out.buf, j, TextOutputUtil.digitPairs(-n))
      j -= 2
      i /= 100
    }
    if(i < -9) TextOutputUtil.SHORT_NATIVE.set(out.buf, j, TextOutputUtil.digitPairs(-i))
    else out.buf(j+1) = (-i + '0').toByte
  }

  private[this] def unsafeWriteLong(_l: Long, p: Int, len: Int): Unit = {
    var l = _l
    if(l < 0) out.buf(p) = '-'.toByte
    else l = -l
    var j = p + len - 2
    while(l <= -100) {
      val n = (l % 100).toInt
      TextOutputUtil.SHORT_NATIVE.set(out.buf, j, TextOutputUtil.digitPairs(-n))
      j -= 2
      l /= 100
    }
    if(l < -9) TextOutputUtil.SHORT_NATIVE.set(out.buf, j, TextOutputUtil.digitPairs(-l.toInt))
    else out.buf(j+1) = (-l.toInt + '0').toByte
  }

  override def print(c: Char): this.type = {
    if(c <= maxSafeChar && !hasSurrogate) out.int8(c.toByte)
    else printRaw(String.valueOf(c))
    this
  }

  override def println(c: Char): this.type = {
    if(c <= maxSafeChar && !hasSurrogate) {
      val p = out.fwd(1 + eolLen)
      out.buf(p) = c.toByte
      unsafeWriteEOL(p + 1)
    } else printlnRaw(String.valueOf(c))
    maybeFlush()
    this
  }

  override def close(): Unit = {
    flushSurrogate()
    super.close()
  }
}


private class Latin1TextOutput(_out: BufferedOutput, _eol: Array[Byte], _autoFlush: Boolean) extends ASCIICompatibleTextOutput(_out, StandardCharsets.ISO_8859_1, _eol, _autoFlush) {
  override protected def printRaw(s: String): Unit = {
    val l = s.length
    if(l > 0) {
      if(StringInternals.isLatin1(s)) {
        val p = out.fwd(l)
        val v = StringInternals.value(s)
        System.arraycopy(v, 0, out.buf, p, v.length)
      } else super.printRaw(s)
    }
  }

  override protected def printlnRaw(s: String): Unit = {
    val l = s.length
    if(StringInternals.isLatin1(s)) {
      val p = out.fwd(l + eolLen)
      val v = StringInternals.value(s)
      if(l > 0) System.arraycopy(v, 0, out.buf, p, v.length)
      unsafeWriteEOL(p + l)
    } else super.printlnRaw(s)
  }
}


private class UTF8TextOutput(_out: BufferedOutput, _eol: Array[Byte], _autoFlush: Boolean) extends ASCIICompatibleTextOutput(_out, StandardCharsets.UTF_8, _eol, _autoFlush) {
  override protected def printRaw(s: String): Unit = {
    val l = s.length
    if(l > 0) {
      if(StringInternals.isLatin1(s)) {
        val v = StringInternals.value(s)
        if(!StringInternals.hasNegatives(v, 0, v.length)) {
          val p = out.fwd(l)
          System.arraycopy(v, 0, out.buf, p, v.length)
        } else super.printRaw(s)
      } else super.printRaw(s)
    }
  }

  override protected def printlnRaw(s: String): Unit = {
    val l = s.length
    if(StringInternals.isLatin1(s)) {
      val v = StringInternals.value(s)
      if(!StringInternals.hasNegatives(v, 0, v.length)) {
        val p = out.fwd(l + eolLen)
        if(l > 0) System.arraycopy(v, 0, out.buf, p, v.length)
        unsafeWriteEOL(p + l)
      } else super.printlnRaw(s)
    } else super.printlnRaw(s)
  }
}


private class ASCIITextOutput(_out: BufferedOutput, _eol: Array[Byte], _autoFlush: Boolean) extends ASCIICompatibleTextOutput(_out, StandardCharsets.US_ASCII, _eol, _autoFlush) {
  override protected def printRaw(s: String): Unit = {
    val l = s.length
    if(l > 0) {
      if(StringInternals.isLatin1(s)) {
        val v = StringInternals.value(s)
        if(!StringInternals.hasNegatives(v, 0, v.length)) {
          val p = out.fwd(l)
          System.arraycopy(v, 0, out.buf, p, v.length)
        } else super.printRaw(s)
      } else super.printRaw(s)
    }
  }

  override protected def printlnRaw(s: String): Unit = {
    val l = s.length
    if(StringInternals.isLatin1(s)) {
      val v = StringInternals.value(s)
      if(!StringInternals.hasNegatives(v, 0, v.length)) {
        val p = out.fwd(l + eolLen)
        if(l > 0) System.arraycopy(v, 0, out.buf, p, v.length)
        unsafeWriteEOL(p + l)
      } else super.printlnRaw(s)
    } else super.printlnRaw(s)
  }
}


/** Generic TextOutput implementation that should work for arbitrary Charsets (including support for BOMs and end markers). */
private class GenericTextOutput(_out: BufferedOutput, _cs: Charset, eol: String, _autoFlush: Boolean) extends TextOutput(_out, _cs, _autoFlush) {
  private[this] val enc = cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE)
  private[this] var prevBuf: Array[Byte] = null
  private[this] var bb: ByteBuffer = null
  private[this] val singleCharBuf = CharBuffer.allocate(1)

  private[this] def updateBB(p: Int): Unit = {
    if(prevBuf ne out.buf) {
      bb = ByteBuffer.wrap(out.buf)
      prevBuf = out.buf
    }
    bb.limit(out.pos).position(p)
  }

  private[this] def printRawInner(cb: CharBuffer, endOfInput: Boolean): Unit = {
    val max = (enc.maxBytesPerChar() * cb.length).toInt
    val p = out.tryFwd(max)
    updateBB(p)
    bb.limit(out.pos).position(p)
    val res = enc.encode(cb, bb, endOfInput)
    out.pos = bb.position()
    if(res.isOverflow) throw new EOFException()
    if(cb.remaining() == 1) {
      hasSurrogate = true
      surrogateCharBuf.put(0, cb.get())
    }
  }

  private[this] def printRaw(cb: CharBuffer, endOfInput: Boolean): Unit = {
    if(cb.remaining() > 0) {
      if(hasSurrogate) {
        hasSurrogate = false
        if(cb.remaining() > 0) {
          surrogateCharBuf.put(1, cb.get()).position(0)
          printRawInner(surrogateCharBuf, false)
        }
      }
      printRawInner(cb, endOfInput)
    }
  }

  protected def printRaw(s: String): Unit = printRaw(CharBuffer.wrap(s), false)

  protected def printRawNull(): Unit = printRaw("null")

  protected def printlnRawNull(): Unit = printlnRaw("null")

  override def close(): Unit = {
    if(hasSurrogate) {
      surrogateCharBuf.position(0).limit(1)
      printRawInner(surrogateCharBuf, true)
    } else {
      singleCharBuf.position(1)
      printRawInner(singleCharBuf, true)
    }
    bb.limit(out.lim).position(out.pos)
    if(enc.flush(bb).isOverflow && out.pos + 64 > out.lim) {
      // There is no way to know how much more space we need; 64 bytes should be on the safe side
      val p = out.tryFwd(64)
      updateBB(p)
      if(enc.flush(bb).isOverflow) throw new EOFException()
      out.pos = bb.position()
    }
    super.close()
  }

  protected def printlnRaw(s: String): Unit = {
    printRaw(s)
    printlnRaw()
  }

  protected def printlnRaw(): Unit = printRaw(eol)

  override def print(c: Char): this.type = {
    singleCharBuf.position(0).put(0, c)
    printRaw(singleCharBuf, false)
    this
  }

  override def println(c: Char): this.type = {
    singleCharBuf.position(0).put(0, c)
    printRaw(singleCharBuf, false)
    printlnRaw()
    maybeFlush()
    this
  }

  override def print(s: Array[Char]): this.type = {
    printRaw(CharBuffer.wrap(s), false)
    this
  }

  override def println(s: Array[Char]): this.type = {
    printRaw(CharBuffer.wrap(s), false)
    printlnRaw()
    maybeFlush()
    this
  }
}


private object TextOutputUtil {
  private[this] val bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN

  val SHORT_NATIVE = MethodHandles.byteArrayViewVarHandle(classOf[Short].arrayType(), ByteOrder.nativeOrder())
  val INT_NATIVE = MethodHandles.byteArrayViewVarHandle(classOf[Int].arrayType(), ByteOrder.nativeOrder())

  val LIT_TRUE = lit4("true")
  val LIT_FALS = lit4("fals")
  val LIT_NULL = lit4("null")

  private[this] def lit4(s: String): Int = {
    val d0 = s.charAt(0).toInt
    val d1 = s.charAt(1).toInt
    val d2 = s.charAt(2).toInt
    val d3 = s.charAt(3).toInt
    if(bigEndian) (d0 << 24) | (d1 << 16) | (d2 << 8) | d3
    else (d3 << 24) | (d2 << 16) | (d1 << 8) | d0
  }

  /** ASCII decimal digits of numbers 0 to 99 encoded as byte pairs in native byte order. */
  val digitPairs: Array[Short] = {
    val a = new Array[Short](100)
    var i = 0
    while(i < 100) {
      val d0 = (i % 10) + '0'
      val d1 = ((i/10) % 10) + '0'
      val c = if(bigEndian) (d1 << 8) | d0 else (d0 << 8) | d1
      a(i) = c.toShort
      i += 1
    }
    a
  }

  /** Number of chars needed to represent the given Int value using the same algorithm as `Integer.stringSize` (which
   * is not public API). This tends to be fastest for small to medium-sized numbers. Manually unrolling
   * (`if(x >= -9) ... else if(x >= -99) ...`) is faster for very small numbers, a constant-time algorithm like
   * https://github.com/ramanawithu/fast_int_to_string/blob/7a2d82bb4aea91afab48b741e87460f810141c71/fast_int_to_string.hpp#L47
   * may be faster for completely random numbers. */
  def numChars(_x: Int): Int = {
    val (x, d) = if(_x < 0) (_x, 1) else (-_x, 0)
    var p = -10
    var i = 1
    while(i < 10) {
      if(x > p) return d + i
      p *= 10
      i += 1
    }
    d + 10
  }

  def numChars(_x: Long): Int = {
    val (x, d) = if(_x < 0) (_x, 1) else (-_x, 0)
    var p = -10L
    var i = 1
    while(i < 19) {
      if(x > p) return d + i
      p *= 10
      i += 1
    }
    d + 19
  }
}
