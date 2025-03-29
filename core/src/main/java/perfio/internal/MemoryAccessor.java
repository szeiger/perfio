package perfio.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static perfio.internal.BufferUtil.*;

public abstract class MemoryAccessor {
  public static final MemoryAccessor INSTANCE;
  public static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

  static {
    MemoryAccessor a = null;
    var unsafeEnabled = "true".equals(System.getProperty("perfio.enableUnsafe"));
    if(unsafeEnabled)
      try { a = UnsafeMemoryAccessor.INSTANCE; } catch (Throwable t) {}
    INSTANCE = a != null ? a : SafeMemoryAccessor.INSTANCE;
  }

  public abstract boolean isUnsafe();

  public abstract byte int8(byte[] a, int i);
  public abstract void int8(byte[] a, int i, byte v);

  public abstract short int16(byte[] a, int i, boolean big);
  public abstract short int16n(byte[] a, int i);
  public abstract short int16b(byte[] a, int i);
  public abstract short int16l(byte[] a, int i);
  public abstract void int16(byte[] a, int i, short v, boolean big);
  public abstract void int16n(byte[] a, int i, short v);
  public abstract void int16b(byte[] a, int i, short v);
  public abstract void int16l(byte[] a, int i, short v);

  public abstract char uint16(byte[] a, int i, boolean big);
  public abstract char uint16n(byte[] a, int i);
  public abstract char uint16b(byte[] a, int i);
  public abstract char uint16l(byte[] a, int i);
  public abstract void uint16(byte[] a, int i, char v, boolean big);
  public abstract void uint16n(byte[] a, int i, char v);
  public abstract void uint16b(byte[] a, int i, char v);
  public abstract void uint16l(byte[] a, int i, char v);

  public abstract int int32(byte[] a, int i, boolean big);
  public abstract int int32n(byte[] a, int i);
  public abstract int int32b(byte[] a, int i);
  public abstract int int32l(byte[] a, int i);
  public abstract void int32(byte[] a, int i, int v, boolean big);
  public abstract void int32n(byte[] a, int i, int v);
  public abstract void int32b(byte[] a, int i, int v);
  public abstract void int32l(byte[] a, int i, int v);

  public abstract long int64(byte[] a, int i, boolean big);
  public abstract long int64n(byte[] a, int i);
  public abstract long int64b(byte[] a, int i);
  public abstract long int64l(byte[] a, int i);
  public abstract void int64(byte[] a, int i, long v, boolean big);
  public abstract void int64n(byte[] a, int i, long v);
  public abstract void int64b(byte[] a, int i, long v);
  public abstract void int64l(byte[] a, int i, long v);

  public abstract float float32(byte[] a, int i, boolean big);
  public abstract float float32n(byte[] a, int i);
  public abstract float float32b(byte[] a, int i);
  public abstract float float32l(byte[] a, int i);
  public abstract void float32(byte[] a, int i, float v, boolean big);
  public abstract void float32n(byte[] a, int i, float v);
  public abstract void float32b(byte[] a, int i, float v);
  public abstract void float32l(byte[] a, int i, float v);

  public abstract double float64(byte[] a, int i, boolean big);
  public abstract double float64n(byte[] a, int i);
  public abstract double float64b(byte[] a, int i);
  public abstract double float64l(byte[] a, int i);
  public abstract void float64(byte[] a, int i, double v, boolean big);
  public abstract void float64n(byte[] a, int i, double v);
  public abstract void float64b(byte[] a, int i, double v);
  public abstract void float64l(byte[] a, int i, double v);

  // No unsafe implementations of direct access. They were slower in all benchmarks.

  public byte int8(ByteBuffer a, int i) { return a.get(i); }

  public short int16(ByteBuffer a, int i, boolean big) { return (short)(big ? BB_SHORT_BIG : BB_SHORT_LITTLE).get(a, i); }
  public short int16n(ByteBuffer a, int i) { return (short)(BIG_ENDIAN ? BB_SHORT_BIG : BB_SHORT_LITTLE).get(a, i); }
  public short int16b(ByteBuffer a, int i) { return (short)BB_SHORT_BIG.get(a, i); }
  public short int16l(ByteBuffer a, int i) { return (short)BB_SHORT_LITTLE.get(a, i); }

  public char uint16(ByteBuffer a, int i, boolean big) { return (char)(big ? BB_CHAR_BIG : BB_CHAR_LITTLE).get(a, i); }
  public char uint16n(ByteBuffer a, int i) { return (char)(BIG_ENDIAN ? BB_CHAR_BIG : BB_CHAR_LITTLE).get(a, i); }
  public char uint16b(ByteBuffer a, int i) { return (char)BB_CHAR_BIG.get(a, i); }
  public char uint16l(ByteBuffer a, int i) { return (char)BB_CHAR_LITTLE.get(a, i); }

  public int int32(ByteBuffer a, int i, boolean big) { return (int)(big ? BB_INT_BIG : BB_INT_LITTLE).get(a, i); }
  public int int32n(ByteBuffer a, int i) { return (int)(BIG_ENDIAN ? BB_INT_BIG : BB_INT_LITTLE).get(a, i); }
  public int int32b(ByteBuffer a, int i) { return (int)BB_INT_BIG.get(a, i); }
  public int int32l(ByteBuffer a, int i) { return (int)BB_INT_LITTLE.get(a, i); }

  public long int64(ByteBuffer a, int i, boolean big) { return (long)(big ? BB_LONG_BIG : BB_LONG_LITTLE).get(a, i); }
  public long int64n(ByteBuffer a, int i) { return (long)(BIG_ENDIAN ? BB_LONG_BIG : BB_LONG_LITTLE).get(a, i); }
  public long int64b(ByteBuffer a, int i) { return (long)BB_LONG_BIG.get(a, i); }
  public long int64l(ByteBuffer a, int i) { return (long)BB_LONG_LITTLE.get(a, i); }

  public float float32(ByteBuffer a, int i, boolean big) { return (float)(big ? BB_FLOAT_BIG : BB_FLOAT_LITTLE).get(a, i); }
  public float float32n(ByteBuffer a, int i) { return (float)(BIG_ENDIAN ? BB_FLOAT_BIG : BB_FLOAT_LITTLE).get(a, i); }
  public float float32b(ByteBuffer a, int i) { return (float)BB_FLOAT_BIG.get(a, i); }
  public float float32l(ByteBuffer a, int i) { return (float)BB_FLOAT_LITTLE.get(a, i); }

  public double float64(ByteBuffer a, int i, boolean big) { return (double)(big ? BB_DOUBLE_BIG : BB_DOUBLE_LITTLE).get(a, i); }
  public double float64n(ByteBuffer a, int i) { return (double)(BIG_ENDIAN ? BB_DOUBLE_BIG : BB_DOUBLE_LITTLE).get(a, i); }
  public double float64b(ByteBuffer a, int i) { return (double)BB_DOUBLE_BIG.get(a, i); }
  public double float64l(ByteBuffer a, int i) { return (double)BB_DOUBLE_LITTLE.get(a, i); }
}
