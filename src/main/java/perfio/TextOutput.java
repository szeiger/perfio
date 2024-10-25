package perfio;

import java.io.Closeable;
import java.io.EOFException;
import java.io.Flushable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;


/** TextOutput prints formatted text to a BufferedOutput. It generally behaves like java.io.PrintWriter except that it
 * does not swallow IOExceptions. They are thrown immediately like in BufferedOutput/OutputStream/etc. Most methods
 * return `this` for convenient chaining of operations. Unlike PrintWriter/PrintStream it allows the end-of-line
 * sequence to be changed from the system default.
 *
 * This class is not thread-safe. Unless external synchronization is used, an instance may only be accessed from a
 * single thread (which must be the same thread that is used for accessing the underlying BufferedOutput).
 *
 * TextOutput does not perform any internal buffering, so you can mix calls to a TextOutput and the underlying
 * BufferedOutput instance. The only general exception to this is when a surrogate pair is split (i.e. the high and
 * low surrogate character are written in 2 separate `print(String)` or `print(Char)` calls), but more esoteric
 * Charset implementations may contain buffering of their own.
 */
public abstract class TextOutput implements Closeable, Flushable {

  /** Create a TextOutput for a given BufferedOutput.
   *
   * @param out The underlying BufferedOutput
   * @param cs The Charset for encoding text. ASCII-compatible standard Charsets (`StandardCharsets.ISO_8859_1`,
   *           `StandardCharsets.UTF_8`, `StandardCharsets.US_ASCII`) are specially optimized and much faster than
   *           the generic implementation for other Charsets (which is still significantly faster than
   *           `java.io.PrintWriter` because it can write directly to a BufferedOutput).
   * */
  public static TextOutput of(BufferedOutput out, Charset cs, String eol, boolean autoFlush) {
    if(cs == StandardCharsets.ISO_8859_1) return new Latin1TextOutput(out, eol.getBytes(cs), autoFlush);
    else if(cs == StandardCharsets.UTF_8) return new UTF8TextOutput(out, eol.getBytes(cs), autoFlush);
    else if(cs == StandardCharsets.US_ASCII) return new ASCIITextOutput(out, eol.getBytes(cs), autoFlush);
    else return new GenericTextOutput(out, cs, eol, autoFlush);
  }

  public static TextOutput of(BufferedOutput out, Charset cs) { return of(out, cs, System.lineSeparator(), false); }

  public static TextOutput of(BufferedOutput out) { return of(out, StandardCharsets.UTF_8, System.lineSeparator(), false); }


  // ======================================================= non-static parts:

  final BufferedOutput out;
  final Charset cs;
  final boolean autoFlush;

  TextOutput(BufferedOutput out, Charset cs, boolean autoFlush) {
    this.out = out;
    this.cs = cs;
    this.autoFlush = autoFlush;
  }

  final CharBuffer surrogateCharBuf = CharBuffer.allocate(2);
  boolean hasSurrogate = false;

  abstract void printRaw(String s) throws IOException;
  abstract void printlnRaw(String s) throws IOException;
  abstract void printlnRaw() throws IOException;
  abstract void printRawNull() throws IOException;
  abstract void printlnRawNull() throws IOException;

  final void maybeFlush() throws IOException { if(autoFlush) out.flush(); }

  public final TextOutput println() throws IOException {
    printlnRaw();
    maybeFlush();
    return this;
  }

  public final TextOutput print(String s) throws IOException {
    if(s == null) printRawNull();
    else printRaw(s);
    return this;
  }
  public final TextOutput println(String s) throws IOException {
    if(s == null) printlnRawNull();
    else printlnRaw(s);
    maybeFlush();
    return this;
  }

  public final TextOutput print(Object o) throws IOException {
    if(o == null) printRawNull();
    else printRaw(String.valueOf(o));
    return this;
  }

  public final TextOutput println(Object o) throws IOException {
    if(o == null) printlnRawNull();
    else printlnRaw(String.valueOf(o));
    maybeFlush();
    return this;
  }

  public TextOutput print(int i) throws IOException {
    printRaw(Integer.toString(i));
    return this;
  }
  public TextOutput println(int i) throws IOException {
    printlnRaw(Integer.toString(i));
    maybeFlush();
    return this;
  }

  public TextOutput print(boolean b) throws IOException {
    printRaw(String.valueOf(b));
    return this;
  }
  public TextOutput println(boolean b) throws IOException {
    printlnRaw(String.valueOf(b));
    maybeFlush();
    return this;
  }

  public TextOutput print(long l) throws IOException {
    printRaw(java.lang.Long.toString(l));
    return this;
  }
  public TextOutput println(long l) throws IOException {
    printlnRaw(java.lang.Long.toString(l));
    maybeFlush();
    return this;
  }

  public TextOutput print(char c) throws IOException {
    printRaw(String.valueOf(c));
    return this;
  }
  public TextOutput println(char c) throws IOException {
    printlnRaw(String.valueOf(c));
    maybeFlush();
    return this;
  }

  public TextOutput print(char[] s) throws IOException {
    printRaw(new String(s));
    return this;
  }
  public TextOutput println(char[] s) throws IOException {
    printlnRaw(new String(s));
    maybeFlush();
    return this;
  }

  public TextOutput print(float f) throws IOException {
    printRaw(String.valueOf(f));
    return this;
  }
  public TextOutput println(float f) throws IOException {
    printlnRaw(String.valueOf(f));
    maybeFlush();
    return this;
  }

  public TextOutput print(double d) throws IOException {
    printRaw(String.valueOf(d));
    return this;
  }
  public TextOutput println(double d) throws IOException {
    printlnRaw(String.valueOf(d));
    maybeFlush();
    return this;
  }

  public void flush() throws IOException { out.flush(); }

  public void close() throws IOException { out.close(); }
}


/** TextOutput implementation for ASCII-compatible charsets. All ASCII characters are encoded as a single ASCII byte. */
abstract class ASCIICompatibleTextOutput extends TextOutput {
  final byte[] eol;
  final int eolLen;
  private final byte eol0, eol1;
  private final int maxSafeChar;

  ASCIICompatibleTextOutput(BufferedOutput out, Charset cs, byte[] eol, boolean autoFlush) {
    super(out, cs, autoFlush);
    this.eol = eol;
    this.eolLen = eol.length;
    this.eol0 = eol[0];
    this.eol1 = eolLen > 1 ? eol[1] : (byte)0;
    this.maxSafeChar = cs == StandardCharsets.ISO_8859_1 ? 255 : 127;
  }

  void unsafeWriteEOL(int p) {
    switch(eolLen) {
      case 1 -> out.buf[p] = eol0;
      case 2 -> { out.buf[p] = eol0; out.buf[p+1] = eol1; }
      default -> System.arraycopy(eol, 0, out.buf, p, eolLen);
    }
  }

  private void flushSurrogate() throws IOException {
    if(hasSurrogate) {
      out.int8((byte)'?');
      hasSurrogate = false;
    }
  }

  void printRaw(String s) throws IOException {
    if(!s.isEmpty()) {
      if(hasSurrogate || Character.isHighSurrogate(s.charAt(s.length()-1))) printRawWithSurrogates(s);
      else out.write(s.getBytes(cs));
    }
  }

  private void printRawWithSurrogates(String s) throws IOException {
    var len = s.length();
    var startOff = Character.isLowSurrogate(s.charAt(0)) ? 1 : 0;
    var endOff = Character.isHighSurrogate(s.charAt(len-1)) ? 1 : 0;
    if(startOff != 0 && hasSurrogate) {
      surrogateCharBuf.put(1, s.charAt(0)).position(0);
      var bb = cs.encode(surrogateCharBuf);
      out.write(bb.array(), 0, bb.limit());
    } else if(startOff != 0 || hasSurrogate) {
      out.int8((byte)'?');
    }
    var v = s.substring(startOff, len-endOff);
    if(v.length() > 0) out.write(v.getBytes(cs));
    if(endOff != 0) {
      hasSurrogate = true;
      surrogateCharBuf.put(0, s.charAt(len-1));
    } else hasSurrogate = false;
  }

  void printlnRaw(String s) throws IOException {
    if(!s.isEmpty()) {
      if(hasSurrogate || Character.isHighSurrogate(s.charAt(s.length()-1))) printlnRawWithSurrogates(s);
      else {
        var b = s.getBytes(cs);
        var l = b.length;
        var p = out.fwd(l + eolLen);
        if(l > 0) System.arraycopy(b, 0, out.buf, p, l);
        unsafeWriteEOL(p + l);
      }
    } else printlnRaw();
  }

  private void printlnRawWithSurrogates(String s) throws IOException {
    printRawWithSurrogates(s);
    printlnRaw();
  }

  void printlnRaw() throws IOException {
    flushSurrogate();
    var p = out.fwd(eolLen);
    unsafeWriteEOL(p);
  }

  void printRawNull() throws IOException {
    flushSurrogate();
    var p = out.fwd(4);
    unsafeWriteNull(p);
  }

  void printlnRawNull() throws IOException {
    flushSurrogate();
    var p = out.fwd(4 + eolLen);
    unsafeWriteNull(p);
    unsafeWriteEOL(p + 4);
  }

  @Override
  public TextOutput print(int i) throws IOException {
    flushSurrogate();
    var len = TextOutputUtil.numChars(i);
    var p = out.fwd(len);
    unsafeWriteInt(i, p, len);
    return this;
  }

  @Override
  public TextOutput println(int i) throws IOException {
    flushSurrogate();
    var len = TextOutputUtil.numChars(i);
    var p = out.fwd(len + eolLen);
    unsafeWriteInt(i, p, len);
    unsafeWriteEOL(p + len);
    maybeFlush();
    return this;
  }

  @Override
  public TextOutput print(long l) throws IOException {
    flushSurrogate();
    var len = TextOutputUtil.numChars(l);
    var p = out.fwd(len);
    unsafeWriteLong(l, p, len);
    return this;
  }

  @Override
  public TextOutput println(long l) throws IOException {
    flushSurrogate();
    var len = TextOutputUtil.numChars(l);
    var p = out.fwd(len + eolLen);
    unsafeWriteLong(l, p, len);
    unsafeWriteEOL(p + len);
    maybeFlush();
    return this;
  }

  @Override
  public TextOutput print(boolean b) throws IOException {
    flushSurrogate();
    if(b) {
      var p = out.fwd(4);
      unsafeWriteTrue(p);
    } else {
      var p = out.fwd(5);
      unsafeWriteFalse(p);
    }
    return this;
  }

  @Override
  public TextOutput println(boolean b) throws IOException {
    flushSurrogate();
    if(b) {
      var p = out.fwd(4 + eolLen);
      unsafeWriteTrue(p);
      unsafeWriteEOL(p + 4);
    } else {
      var p = out.fwd(5 + eolLen);
      unsafeWriteFalse(p);
      unsafeWriteEOL(p + 5);
    }
    maybeFlush();
    return this;
  }

  private void unsafeWriteTrue(int p) {
    TextOutputUtil.INT_NATIVE.set(out.buf, p, TextOutputUtil.LIT_TRUE);
  }

  private void unsafeWriteFalse(int p) {
    TextOutputUtil.INT_NATIVE.set(out.buf, p, TextOutputUtil.LIT_FALS);
    out.buf[p + 4] = (byte)'e';
  }

  private void unsafeWriteNull(int p) {
    TextOutputUtil.INT_NATIVE.set(out.buf, p, TextOutputUtil.LIT_NULL);
  }

  private void unsafeWriteInt(int i, int p, int len) {
    if(i < 0) out.buf[p] = (byte)'-';
    else i = -i;
    var j = p + len - 2;
    while(i <= -100) {
      var n = i % 100;
      TextOutputUtil.SHORT_NATIVE.set(out.buf, j, TextOutputUtil.digitPairs[-n]);
      j -= 2;
      i /= 100;
    }
    if(i < -9) TextOutputUtil.SHORT_NATIVE.set(out.buf, j, TextOutputUtil.digitPairs[-i]);
    else out.buf[j+1] = (byte)(-i + '0');
  }

  private void unsafeWriteLong(long l, int p, int len) {
    if(l < 0) out.buf[p] = (byte)'-';
    else l = -l;
    var j = p + len - 2;
    while(l <= -100) {
      var n = (int)(l % 100);
      TextOutputUtil.SHORT_NATIVE.set(out.buf, j, TextOutputUtil.digitPairs[-n]);
      j -= 2;
      l /= 100;
    }
    if(l < -9) TextOutputUtil.SHORT_NATIVE.set(out.buf, j, TextOutputUtil.digitPairs[(int)-l]);
    else out.buf[j+1] = (byte)((int)-l + '0');
  }

  @Override
  public TextOutput print(char c) throws IOException {
    if(c <= maxSafeChar && !hasSurrogate) out.int8((byte)c);
    else printRaw(String.valueOf(c));
    return this;
  }

  @Override
  public TextOutput println(char c) throws IOException {
    if(c <= maxSafeChar && !hasSurrogate) {
      var p = out.fwd(1 + eolLen);
      out.buf[p] = (byte)c;
      unsafeWriteEOL(p + 1);
    } else printlnRaw(String.valueOf(c));
    maybeFlush();
    return this;
  }

  @Override
  public void close() throws IOException {
    flushSurrogate();
    super.close();
  }
}


class Latin1TextOutput extends ASCIICompatibleTextOutput {
  Latin1TextOutput(BufferedOutput out, byte[] eol, boolean autoFlush) {
    super(out, StandardCharsets.ISO_8859_1, eol, autoFlush);
  }

  @Override
  void printRaw(String s) throws IOException {
    var l = s.length();
    if(l > 0) {
      if(StringInternals.isLatin1(s)) {
        var p = out.fwd(l);
        var v = StringInternals.value(s);
        System.arraycopy(v, 0, out.buf, p, v.length);
      } else super.printRaw(s);
    }
  }

  @Override
  void printlnRaw(String s) throws IOException {
    var l = s.length();
    if(StringInternals.isLatin1(s)) {
      var p = out.fwd(l + eolLen);
      var v = StringInternals.value(s);
      if(l > 0) System.arraycopy(v, 0, out.buf, p, v.length);
      unsafeWriteEOL(p + l);
    } else super.printlnRaw(s);
  }
}


class UTF8TextOutput extends ASCIICompatibleTextOutput {
  UTF8TextOutput(BufferedOutput out, byte[] eol, boolean autoFlush) {
    super(out, StandardCharsets.UTF_8, eol, autoFlush);
  }

  @Override
  void printRaw(String s) throws IOException {
    var l = s.length();
    if(l > 0) {
      if(StringInternals.isLatin1(s)) {
        var v = StringInternals.value(s);
        if(!StringInternals.hasNegatives(v, 0, v.length)) {
          var p = out.fwd(l);
          System.arraycopy(v, 0, out.buf, p, v.length);
        } else super.printRaw(s);
      } else super.printRaw(s);
    }
  }

  @Override
  void printlnRaw(String s) throws IOException {
    var l = s.length();
    if(StringInternals.isLatin1(s)) {
      var v = StringInternals.value(s);
      if(!StringInternals.hasNegatives(v, 0, v.length)) {
        var p = out.fwd(l + eolLen);
        if(l > 0) System.arraycopy(v, 0, out.buf, p, v.length);
        unsafeWriteEOL(p + l);
      } else super.printlnRaw(s);
    } else super.printlnRaw(s);
  }
}


class ASCIITextOutput extends ASCIICompatibleTextOutput {
  ASCIITextOutput(BufferedOutput out, byte[] eol, boolean autoFlush) {
    super(out, StandardCharsets.US_ASCII, eol, autoFlush);
  }

  @Override
  void printRaw(String s) throws IOException {
    var l = s.length();
    if(l > 0) {
      if(StringInternals.isLatin1(s)) {
        var v = StringInternals.value(s);
        if(!StringInternals.hasNegatives(v, 0, v.length)) {
          var p = out.fwd(l);
          System.arraycopy(v, 0, out.buf, p, v.length);
        } else super.printRaw(s);
      } else super.printRaw(s);
    }
  }

  @Override
  void printlnRaw(String s) throws IOException {
    var l = s.length();
    if(StringInternals.isLatin1(s)) {
      var v = StringInternals.value(s);
      if(!StringInternals.hasNegatives(v, 0, v.length)) {
        var p = out.fwd(l + eolLen);
        if(l > 0) System.arraycopy(v, 0, out.buf, p, v.length);
        unsafeWriteEOL(p + l);
      } else super.printlnRaw(s);
    } else super.printlnRaw(s);
  }
}


/** Generic TextOutput implementation that should work for arbitrary Charsets (including support for BOMs and end markers). */
class GenericTextOutput extends TextOutput {
  private final String eol;
  private final CharsetEncoder enc;

  GenericTextOutput(BufferedOutput out, Charset cs, String eol, boolean autoFlush) {
    super(out, cs, autoFlush);
    this.eol = eol;
    this.enc = cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  private byte[] prevBuf = null;
  private ByteBuffer bb = null;
  private final CharBuffer singleCharBuf = CharBuffer.allocate(1);

  private void updateBB(int p) {
    if(prevBuf != out.buf) {
      bb = ByteBuffer.wrap(out.buf);
      prevBuf = out.buf;
    }
    bb.limit(out.pos).position(p);
  }

  private void printRawInner(CharBuffer cb, boolean endOfInput) throws IOException {
    var max = (int)(enc.maxBytesPerChar() * cb.length());
    var p = out.tryFwd(max);
    updateBB(p);
    bb.limit(out.pos).position(p);
    var res = enc.encode(cb, bb, endOfInput);
    out.pos = bb.position();
    if(res.isOverflow()) throw new EOFException();
    if(cb.remaining() == 1) {
      hasSurrogate = true;
      surrogateCharBuf.put(0, cb.get());
    }
  }

  private void printRaw(CharBuffer cb) throws IOException {
    if(cb.remaining() > 0) {
      if(hasSurrogate) {
        hasSurrogate = false;
        if(cb.remaining() > 0) {
          surrogateCharBuf.put(1, cb.get()).position(0);
          printRawInner(surrogateCharBuf, false);
        }
      }
      printRawInner(cb, false);
    }
  }

  void printRaw(String s) throws IOException { printRaw(CharBuffer.wrap(s)); }

  void printRawNull() throws IOException { printRaw("null"); }

  void printlnRawNull() throws IOException { printlnRaw("null"); }

  @Override
  public void close() throws IOException {
    if(hasSurrogate) {
      surrogateCharBuf.position(0).limit(1);
      printRawInner(surrogateCharBuf, true);
    } else {
      singleCharBuf.position(1);
      printRawInner(singleCharBuf, true);
    }
    bb.limit(out.lim).position(out.pos);
    if(enc.flush(bb).isOverflow() && out.pos + 64 > out.lim) {
      // There is no way to know how much more space we need; 64 bytes should be on the safe side
      var p = out.tryFwd(64);
      updateBB(p);
      if(enc.flush(bb).isOverflow()) throw new EOFException();
      out.pos = bb.position();
    }
    super.close();
  }

  void printlnRaw(String s) throws IOException {
    printRaw(s);
    printlnRaw();
  }

  void printlnRaw() throws IOException { printRaw(eol); }

  @Override
  public TextOutput print(char c) throws IOException {
    singleCharBuf.position(0).put(0, c);
    printRaw(singleCharBuf);
    return this;
  }

  @Override
  public TextOutput println(char c) throws IOException {
    singleCharBuf.position(0).put(0, c);
    printRaw(singleCharBuf);
    printlnRaw();
    maybeFlush();
    return this;
  }

  @Override
  public TextOutput print(char[] s) throws IOException {
    printRaw(CharBuffer.wrap(s));
    return this;
  }

  @Override
  public TextOutput println(char[] s) throws IOException {
    printRaw(CharBuffer.wrap(s));
    printlnRaw();
    maybeFlush();
    return this;
  }
}


class TextOutputUtil {
  private TextOutputUtil() {}

  private static final boolean bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

  static final VarHandle SHORT_NATIVE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.nativeOrder());
  static final VarHandle INT_NATIVE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());

  static final int LIT_TRUE = lit4("true");
  static final int LIT_FALS = lit4("fals");
  static final int LIT_NULL = lit4("null");

  private static int lit4(String s) {
    var d0 = (int)s.charAt(0);
    var d1 = (int)s.charAt(1);
    var d2 = (int)s.charAt(2);
    var d3 = (int)s.charAt(3);
    return bigEndian
        ? (d0 << 24) | (d1 << 16) | (d2 << 8) | d3
        : (d3 << 24) | (d2 << 16) | (d1 << 8) | d0;
  }

  /** ASCII decimal digits of numbers 0 to 99 encoded as byte pairs in native byte order. */
  static final short[] digitPairs = new short[100];

  static {
    for(var i=0; i<100; i++) {
      var d0 = (i % 10) + '0';
      var d1 = ((i/10) % 10) + '0';
      var c = bigEndian ? (d1 << 8) | d0 : (d0 << 8) | d1;
      digitPairs[i] = (short)c;
    }
  }

  /** Number of chars needed to represent the given Int value using the same algorithm as `Integer.stringSize` (which
   * is not public API). This tends to be fastest for small to medium-sized numbers. Manually unrolling
   * (`if(x >= -9) ... else if(x >= -99) ...`) is faster for very small numbers, a constant-time algorithm like
   * https://github.com/ramanawithu/fast_int_to_string/blob/7a2d82bb4aea91afab48b741e87460f810141c71/fast_int_to_string.hpp#L47
   * may be faster for completely random numbers. */
  static int numChars(int x) {
    int d = 1;
    if(x >= 0) { x = -x; d = 0; }
    var p = -10;
    for(var i=1; i<10; i++) {
      if(x > p) return d + i;
      p *= 10;
    }
    return d + 10;
  }

  static int numChars(long x) {
    int d = 1;
    if(x >= 0) { x = -x; d = 0; }
    var p = -10L;
    for(var i=1; i<19; i++) {
      if(x > p) return d + i;
      p *= 10;
    }
    return d + 19;
  }
}
