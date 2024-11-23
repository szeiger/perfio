package perfio.internal;

import sun.misc.Unsafe;

import java.nio.ByteOrder;

public abstract class UnsafeMemoryAccessor extends MemoryAccessor {
  protected UnsafeMemoryAccessor() {}

  public boolean isUnsafe() { return true; }

  public byte int8(byte[] a, int i) { return UNSAFE.getByte(a, (long)BA_OFFSET + i); }
  public void int8(byte[] a, int i, byte v) { UNSAFE.putByte(a, (long)BA_OFFSET + i, v); }

  public short int16n(byte[] a, int i) { return UNSAFE.getShort(a, (long)BA_OFFSET + i); }
  public void int16n(byte[] a, int i, short v) { UNSAFE.putShort(a, (long)BA_OFFSET + i, v); }

  public char uint16n(byte[] a, int i) { return UNSAFE.getChar(a, (long)BA_OFFSET + i); }
  public void uint16n(byte[] a, int i, char v) { UNSAFE.putChar(a, (long)BA_OFFSET + i, v); }

  public int int32n(byte[] a, int i) { return UNSAFE.getInt(a, (long)BA_OFFSET + i); }
  public void int32n(byte[] a, int i, int v) { UNSAFE.putInt(a, (long)BA_OFFSET + i, v); }

  public long int64n(byte[] a, int i) { return UNSAFE.getLong(a, (long)BA_OFFSET + i); }
  public void int64n(byte[] a, int i, long v) { UNSAFE.putLong(a, (long)BA_OFFSET + i, v); }

  public float float32n(byte[] a, int i) { return UNSAFE.getFloat(a, (long)BA_OFFSET + i); }
  public void float32n(byte[] a, int i, float v) { UNSAFE.putFloat(a, (long)BA_OFFSET + i, v); }
  public float float32(byte[] a, int i, boolean big) { return Float.intBitsToFloat(int32(a, i, big)); }
  public void float32(byte[] a, int i, float v, boolean big) { int32(a, i, Float.floatToRawIntBits(v), big); }

  public double float64n(byte[] a, int i) { return UNSAFE.getDouble(a, (long)BA_OFFSET + i); }
  public void float64n(byte[] a, int i, double v) { UNSAFE.putDouble(a, (long)BA_OFFSET + i, v); }
  public double float64(byte[] a, int i, boolean big) { return Double.longBitsToDouble(int64(a, i, big)); }
  public void float64(byte[] a, int i, double v, boolean big) { int64(a, i, Double.doubleToRawLongBits(v), big); }

  /// Error encountered during initialization of Unsafe, or null if successful.
  public static final Throwable initializationError;

  /// MemoryAccessor instance if unsafe access is available, otherwise null.
  public static final UnsafeMemoryAccessor INSTANCE;

  static final Unsafe UNSAFE;
  static final int BA_OFFSET;

  static {
    UnsafeMemoryAccessor instanceL = null;
    Unsafe unsafeL = null;
    int baOffsetL = 0;
    Throwable initializationErrorL = null;
    var ok = false;
    try {
      var f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      unsafeL = (Unsafe)f.get(null);
      baOffsetL = unsafeL.arrayBaseOffset(byte[].class);
      instanceL = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? new UnsafeBigMemoryAccessor() : new UnsafeLittleMemoryAccessor();
      ok = true;
    } catch (Throwable t) {
      initializationErrorL = t;
    }
    if(!ok) instanceL = null;
    UNSAFE = unsafeL;
    INSTANCE = instanceL;
    BA_OFFSET = baOffsetL;
    initializationError = initializationErrorL;
  }
}


final class UnsafeBigMemoryAccessor extends UnsafeMemoryAccessor {
  public short int16(byte[] a, int i, boolean big) { return big ? int16n(a, i) : Short.reverseBytes(int16n(a, i)); }
  public short int16b(byte[] a, int i) { return int16n(a, i); }
  public short int16l(byte[] a, int i) { return Short.reverseBytes(int16n(a, i)); }
  public void int16(byte[] a, int i, short v, boolean big) { int16n(a, i, big ? v : Short.reverseBytes(v)); }
  public void int16b(byte[] a, int i, short v) { int16n(a, i, v); }
  public void int16l(byte[] a, int i, short v) { int16n(a, i, Short.reverseBytes(v)); }

  public char uint16(byte[] a, int i, boolean big) { return big ? uint16n(a, i) : Character.reverseBytes(uint16n(a, i)); }
  public char uint16b(byte[] a, int i) { return uint16n(a, i); }
  public char uint16l(byte[] a, int i) { return Character.reverseBytes(uint16n(a, i)); }
  public void uint16(byte[] a, int i, char v, boolean big) { uint16n(a, i, big ? v : Character.reverseBytes(v)); }
  public void uint16b(byte[] a, int i, char v) { uint16n(a, i, v); }
  public void uint16l(byte[] a, int i, char v) { uint16n(a, i, Character.reverseBytes(v)); }

  public int int32(byte[] a, int i, boolean big) { return big ? int32n(a, i) : Integer.reverseBytes(int32n(a, i)); }
  public int int32b(byte[] a, int i) { return int32n(a, i); }
  public int int32l(byte[] a, int i) { return Integer.reverseBytes(int32n(a, i)); }
  public void int32(byte[] a, int i, int v, boolean big) { int32n(a, i, big ? v : Integer.reverseBytes(v)); }
  public void int32b(byte[] a, int i, int v) { int32n(a, i, v); }
  public void int32l(byte[] a, int i, int v) { int32n(a, i, Integer.reverseBytes(v)); }

  public long int64(byte[] a, int i, boolean big) { return big ? int64n(a, i) : Long.reverseBytes(int64n(a, i)); }
  public long int64b(byte[] a, int i) { return int64n(a, i); }
  public long int64l(byte[] a, int i) { return Long.reverseBytes(int64n(a, i)); }
  public void int64(byte[] a, int i, long v, boolean big) { int64n(a, i, big ? v : Long.reverseBytes(v)); }
  public void int64b(byte[] a, int i, long v) { int64n(a, i, v); }
  public void int64l(byte[] a, int i, long v) { int64n(a, i, Long.reverseBytes(v)); }

  public float float32b(byte[] a, int i) { return float32n(a, i); }
  public float float32l(byte[] a, int i) { return Float.intBitsToFloat(int32l(a, i)); }
  public void float32b(byte[] a, int i, float v) { float32n(a, i, v); }
  public void float32l(byte[] a, int i, float v) { int32l(a, i, Float.floatToRawIntBits(v)); }

  public double float64b(byte[] a, int i) { return float64n(a, i); }
  public double float64l(byte[] a, int i) { return Double.longBitsToDouble(int64l(a, i)); }
  public void float64b(byte[] a, int i, double v) { float64n(a, i, v); }
  public void float64l(byte[] a, int i, double v) { int64l(a, i, Double.doubleToRawLongBits(v)); }
}


final class UnsafeLittleMemoryAccessor extends UnsafeMemoryAccessor {
  public short int16(byte[] a, int i, boolean big) { return big ? Short.reverseBytes(int16n(a, i)) : int16n(a, i); }
  public short int16l(byte[] a, int i) { return int16n(a, i); }
  public short int16b(byte[] a, int i) { return Short.reverseBytes(int16n(a, i)); }
  public void int16(byte[] a, int i, short v, boolean big) { int16n(a, i, big ? Short.reverseBytes(v) : v); }
  public void int16l(byte[] a, int i, short v) { int16n(a, i, v); }
  public void int16b(byte[] a, int i, short v) { int16n(a, i, Short.reverseBytes(v)); }

  public char uint16(byte[] a, int i, boolean big) { return big ? Character.reverseBytes(uint16n(a, i)) : uint16n(a, i); }
  public char uint16l(byte[] a, int i) { return uint16n(a, i); }
  public char uint16b(byte[] a, int i) { return Character.reverseBytes(uint16n(a, i)); }
  public void uint16(byte[] a, int i, char v, boolean big) { uint16n(a, i, big ? Character.reverseBytes(v) : v); }
  public void uint16l(byte[] a, int i, char v) { uint16n(a, i, v); }
  public void uint16b(byte[] a, int i, char v) { uint16n(a, i, Character.reverseBytes(v)); }

  public int int32(byte[] a, int i, boolean big) { return big ? Integer.reverseBytes(int32n(a, i)) : int32n(a, i); }
  public int int32l(byte[] a, int i) { return int32n(a, i); }
  public int int32b(byte[] a, int i) { return Integer.reverseBytes(int32n(a, i)); }
  public void int32(byte[] a, int i, int v, boolean big) { int32n(a, i, big ? Integer.reverseBytes(v) : v); }
  public void int32l(byte[] a, int i, int v) { int32n(a, i, v); }
  public void int32b(byte[] a, int i, int v) { int32n(a, i, Integer.reverseBytes(v)); }

  public long int64(byte[] a, int i, boolean big) { return big ? Long.reverseBytes(int64n(a, i)) : int64n(a, i); }
  public long int64l(byte[] a, int i) { return int64n(a, i); }
  public long int64b(byte[] a, int i) { return Long.reverseBytes(int64n(a, i)); }
  public void int64(byte[] a, int i, long v, boolean big) { int64n(a, i, big ? Long.reverseBytes(v) : v); }
  public void int64l(byte[] a, int i, long v) { int64n(a, i, v); }
  public void int64b(byte[] a, int i, long v) { int64n(a, i, Long.reverseBytes(v)); }

  public float float32l(byte[] a, int i) { return float32n(a, i); }
  public float float32b(byte[] a, int i) { return Float.intBitsToFloat(int32b(a, i)); }
  public void float32l(byte[] a, int i, float v) { float32n(a, i, v); }
  public void float32b(byte[] a, int i, float v) { int32b(a, i, Float.floatToRawIntBits(v)); }

  public double float64l(byte[] a, int i) { return float64n(a, i); }
  public double float64b(byte[] a, int i) { return Double.longBitsToDouble(int64b(a, i)); }
  public void float64l(byte[] a, int i, double v) { float64n(a, i, v); }
  public void float64b(byte[] a, int i, double v) { int64b(a, i, Double.doubleToRawLongBits(v)); }
}
