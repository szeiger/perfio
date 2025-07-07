package perfio;

import perfio.internal.MemoryAccessor;
import perfio.internal.StringInternals;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


/// A WritableBuffer contains a byte array, start/position/limit indices and an endian flag.
/// It provides all the methods for writing data to a single block. Operations that can span
/// multiple blocks (like creating views or flushing data) are part of [BufferedOutput].
/// 
/// @param <Self> The self-type that is returned by most methods for chaining operations.
public abstract class WritableBuffer<Self extends WritableBuffer<Self>> {
  protected byte[] buf;
  protected boolean bigEndian;
  protected int start, pos, lim;

  /// Return the byte order of this buffer.
  public final ByteOrder order() { return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN; }

  final int available() { return lim - pos; }

  /// Advance the position in the buffer by `count` and return the previous position.
  /// Throws an IOException if the buffer's size limit would be exceeded.
  protected final int fwd(int count) throws IOException {
    ensureAvailable(count);
    var p = pos;
    pos += count;
    return p;
  }

  /// Ensure `count` bytes are available in the buffer without advancing the position.
  protected final void ensureAvailable(int count) throws IOException {
    // The goal here is to allow HotSpot to elide range checks in some scenarios (e.g.
    // `BufferedOutputUncheckedBenchmark`). The condition should be `available() < count`,
    // i.e. `lim - pos < count` but this does not elide range checks. `pos + count > lim` does,
    // but it would produce an incorrect result due to signed arithmetic if `pos + count`
    // overflows. Using the `Integer.compareUnsigned` intrinsic does not elide the range checks.
    // The double condition does, with no performance impact in the benchmarks.
    if(pos+count > lim || pos+count < 0) {
      flushAndGrow(count);
      if(pos+count > lim || pos+count < 0) throw new EOFException();
    }
  }

  /// Try to advance the position by `count` and return the previous position. May advance by less
  /// (or even 0) if the buffer's size limit would be exceeded. The caller must check [#available()].
  protected final int tryFwd(int count) throws IOException {
    if(pos+count > lim || pos+count < 0) flushAndGrow(count);
    var p = pos;
    pos += Math.min(count, available());
    return p;
  }

  /// Write the contents of the given array region.
  public final Self write(byte[] a, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, a.length);
    var p = fwd(len);
    System.arraycopy(a, off, buf, p, len);
    return (Self)this;
  }

  /// Write the contents of the given array.
  public final Self write(byte[] a) throws IOException { return write(a, 0, a.length); }

  /// Write a signed 8-bit integer (`byte`).
  public final Self int8(byte b) throws IOException {
    var p = fwd(1);
    MemoryAccessor.INSTANCE.int8(buf, p, b);
    return (Self)this;
  }

  /// Write the lower 8 bits of the given `int` as an unsigned 8-bit integer.
  public final Self uint8(int b) throws IOException { return int8((byte)b); }

  /// Write a signed 16-bit integer (`short`) in the current byte [#order()].
  public final Self int16(short s) throws IOException {
    var p = fwd(2);
    MemoryAccessor.INSTANCE.int16(buf, p, s, bigEndian);
    return (Self)this;
  }
  /// Write a signed 16-bit integer (`short`) in the native byte order.
  public final Self int16n(short s) throws IOException {
    var p = fwd(2);
    MemoryAccessor.INSTANCE.int16n(buf, p, s);
    return (Self)this;
  }
  /// Write a signed 16-bit integer (`short`) in big endian byte order.
  public final Self int16b(short s) throws IOException {
    var p = fwd(2);
    MemoryAccessor.INSTANCE.int16b(buf, p, s);
    return (Self)this;
  }
  /// Write a signed 16-bit integer (`short`) in little endian byte order.
  public final Self int16l(short s) throws IOException {
    var p = fwd(2);
    MemoryAccessor.INSTANCE.int16l(buf, p, s);
    return (Self)this;
  }

  /// Write an unsigned 16-bit integer (`char`) in the current byte [#order()].
  public final Self uint16(char c) throws IOException {
    var p = fwd(2);
    MemoryAccessor.INSTANCE.uint16(buf, p, c, bigEndian);
    return (Self)this;
  }
  /// Write an unsigned 16-bit integer (`char`) in the native byte order.
  public final Self uint16n(char c) throws IOException {
    var p = fwd(2);
    MemoryAccessor.INSTANCE.uint16n(buf, p, c);
    return (Self)this;
  }
  /// Write an unsigned 16-bit integer (`char`) in big endian byte order.
  public final Self uint16b(char c) throws IOException {
    var p = fwd(2);
    MemoryAccessor.INSTANCE.uint16b(buf, p, c);
    return (Self)this;
  }
  /// Write an unsigned 16-bit integer (`char`) in little endian byte order.
  public final Self uint16l(char c) throws IOException {
    var p = fwd(2);
    MemoryAccessor.INSTANCE.uint16l(buf, p, c);
    return (Self)this;
  }

  /// Write a signed 32-bit integer (`int`) in the current byte [#order()].
  public final Self int32(int i) throws IOException {
    var p = fwd(4);
    MemoryAccessor.INSTANCE.int32(buf, p, i, bigEndian);
    return (Self)this;
  }
  /// Write a signed 32-bit integer (`int`) in the native byte order.
  public final Self int32n(int i) throws IOException {
    var p = fwd(4);
    MemoryAccessor.INSTANCE.int32n(buf, p, i);
    return (Self)this;
  }
  /// Write a signed 32-bit integer (`int`) in big endian byte order.
  public final Self int32b(int i) throws IOException {
    var p = fwd(4);
    MemoryAccessor.INSTANCE.int32b(buf, p, i);
    return (Self)this;
  }
  /// Write a signed 32-bit integer (`int`) in little endian byte order.
  public final Self int32l(int i) throws IOException {
    var p = fwd(4);
    MemoryAccessor.INSTANCE.int32l(buf, p, i);
    return (Self)this;
  }

  /// Write the lower 32 bits of the given `long` as an unsigned 32-bit integer in the current byte [#order()].
  public final Self uint32(long i) throws IOException { return int32((int)i); }
  /// Write the lower 32 bits of the given `long` as an unsigned 32-bit integer in the native byte order.
  public final Self uint32n(long i) throws IOException { return int32n((int)i); }
  /// Write the lower 32 bits of the given `long` as an unsigned 32-bit integer in big endian byte order.
  public final Self uint32b(long i) throws IOException { return int32b((int)i); }
  /// Write the lower 32 bits of the given `long` as an unsigned 32-bit integer in little endian byte order.
  public final Self uint32l(long i) throws IOException { return int32l((int)i); }

  /// Write a signed 64-bit integer (`long`) in the current byte [#order()].
  public final Self int64(long l) throws IOException {
    var p = fwd(8);
    MemoryAccessor.INSTANCE.int64(buf, p, l, bigEndian);
    return (Self)this;
  }
  /// Write a signed 64-bit integer (`long`) in the native byte order.
  public final Self int64n(long l) throws IOException {
    var p = fwd(8);
    MemoryAccessor.INSTANCE.int64n(buf, p, l);
    return (Self)this;
  }
  /// Write a signed 64-bit integer (`long`) in big endian byte order.
  public final Self int64b(long l) throws IOException {
    var p = fwd(8);
    MemoryAccessor.INSTANCE.int64b(buf, p, l);
    return (Self)this;
  }
  /// Write a signed 64-bit integer (`long`) in little endian byte order.
  public final Self int64l(long l) throws IOException {
    var p = fwd(8);
    MemoryAccessor.INSTANCE.int64l(buf, p, l);
    return (Self)this;
  }

  /// Write a 32-bit IEEE-754 floating point value (`float`) in the current byte [#order()].
  public final Self float32(float f) throws IOException {
    var p = fwd(4);
    MemoryAccessor.INSTANCE.float32(buf, p, f, bigEndian);
    return (Self)this;
  }
  /// Write a 32-bit IEEE-754 floating point value (`float`) in the native byte order.
  public final Self float32n(float f) throws IOException {
    var p = fwd(4);
    MemoryAccessor.INSTANCE.float32n(buf, p, f);
    return (Self)this;
  }
  /// Write a 32-bit IEEE-754 floating point value (`float`) in big endian byte order.
  public final Self float32b(float f) throws IOException {
    var p = fwd(4);
    MemoryAccessor.INSTANCE.float32b(buf, p, f);
    return (Self)this;
  }
  /// Write a 32-bit IEEE-754 floating point value (`float`) in little endian byte order.
  public final Self float32l(float f) throws IOException {
    var p = fwd(4);
    MemoryAccessor.INSTANCE.float32l(buf, p, f);
    return (Self)this;
  }

  /// Write a 64-bit IEEE-754 floating point value (`double`) in the current byte [#order()].
  public final Self float64(double d) throws IOException {
    var p = fwd(8);
    MemoryAccessor.INSTANCE.float64(buf, p, d, bigEndian);
    return (Self)this;
  }
  /// Write a 64-bit IEEE-754 floating point value (`double`) in the native byte order.
  public final Self float64n(double d) throws IOException {
    var p = fwd(8);
    MemoryAccessor.INSTANCE.float64n(buf, p, d);
    return (Self)this;
  }
  /// Write a 64-bit IEEE-754 floating point value (`double`) in big endian byte order.
  public final Self float64b(double d) throws IOException {
    var p = fwd(8);
    MemoryAccessor.INSTANCE.float64b(buf, p, d);
    return (Self)this;
  }
  /// Write a 64-bit IEEE-754 floating point value (`double`) in little endian byte order.
  public final Self float64l(double d) throws IOException {
    var p = fwd(8);
    MemoryAccessor.INSTANCE.float64l(buf, p, d);
    return (Self)this;
  }

  /// Write a [String] in the given [Charset].
  ///
  /// Encoding is performed as a standalone operation with the default replacement characters. It is not possible
  /// to write surrogate pairs that are split across 2 strings. Use [TextOutput] if you need this feature.
  ///
  /// The encoded data is written verbatim without a length prefix or terminator.
  public final int string(String s, Charset charset) throws IOException {
    if(charset == StandardCharsets.UTF_8) return stringUtf8(s, 0);
    else if(charset == StandardCharsets.ISO_8859_1) return stringLatin1(s, 0);
    return stringFallback(s, charset, 0);
  }

  /// Write a [String] in UTF-8 encoding.
  ///
  /// Note that this is the standard UTF-8 format. It is not compatible with [java.io.DataInput] /
  /// [java.io.DataOutput] which use a non-standard variant.
  ///
  /// @see #string(String, Charset)
  public final int string(String s) throws IOException { return stringUtf8(s, 0); }

  private int stringFallback(String s, Charset charset, int extra) throws IOException {
    var b = s.getBytes(charset);
    var p = fwd(b.length + extra);
    System.arraycopy(b, 0, buf, p, b.length);
    return b.length + extra;
  }

  private int stringLatin1(String s, int extra) throws IOException {
    var l = s.length();
    if(l == 0) return 0;
    else {
      if(StringInternals.isLatin1(s)) {
        var p = fwd(l + extra);
        var v = StringInternals.value(s);
        System.arraycopy(v, 0, buf, p, v.length);
        return l + extra;
      } else return stringFallback(s, StandardCharsets.ISO_8859_1, extra);
    }
  }

  private int stringUtf8(String s, int extra) throws IOException {
    var l = s.length();
    if(l == 0) return 0;
    else {
      if(StringInternals.isLatin1(s)) {
        var v = StringInternals.value(s);
        if(!StringInternals.hasNegatives(v, 0, v.length)) {
          var p = fwd(l + extra);
          System.arraycopy(v, 0, buf, p, v.length);
          return l + extra;
        } else return stringFallback(s, StandardCharsets.UTF_8, extra);
      } else return stringFallback(s, StandardCharsets.UTF_8, extra);
    }
  }

  /// Write a zero-terminated [String] in the given [Charset].
  /// @see #string(String, Charset)
  public final int zstring(String s, Charset charset) throws IOException {
    int l = (charset == StandardCharsets.UTF_8) ? stringUtf8(s, 1)
        : (charset == StandardCharsets.ISO_8859_1) ? stringLatin1(s, 1)
        : stringFallback(s, charset, 1);
    if(l == 0) {
      int8((byte)0);
      return 1;
    } else {
      buf[pos-1] = 0;
      return l;
    }
  }

  /// Write a zero-terminated [String] in UTF-8 encoding.
  /// @see #string(String, Charset)
  public final int zstring(String s) throws IOException { return zstring(s, StandardCharsets.UTF_8); }

  protected abstract void flushAndGrow(int count) throws IOException;
}
