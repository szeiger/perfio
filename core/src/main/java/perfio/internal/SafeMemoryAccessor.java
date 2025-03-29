package perfio.internal;

import static perfio.internal.BufferUtil.*;

final class SafeMemoryAccessor extends MemoryAccessor {
  private SafeMemoryAccessor() {}

  public static final SafeMemoryAccessor INSTANCE = new SafeMemoryAccessor();

  public boolean isUnsafe() { return false; }

  public byte int8(byte[] a, int i) { return a[i]; }
  public void int8(byte[] a, int i, byte v) { a[i] = v; }

  public short int16(byte[] a, int i, boolean big) { return (short)(big ? BA_SHORT_BIG : BA_SHORT_LITTLE).get(a, i); }
  public short int16n(byte[] a, int i) { return (short)(BIG_ENDIAN ? BA_SHORT_BIG : BA_SHORT_LITTLE).get(a, i); }
  public short int16b(byte[] a, int i) { return (short)BA_SHORT_BIG.get(a, i); }
  public short int16l(byte[] a, int i) { return (short)BA_SHORT_LITTLE.get(a, i); }
  public void int16(byte[] a, int i, short v, boolean big) { (big ? BA_SHORT_BIG : BA_SHORT_LITTLE).set(a, i, v); }
  public void int16n(byte[] a, int i, short v) { (BIG_ENDIAN ? BA_SHORT_BIG : BA_SHORT_LITTLE).set(a, i, v); }
  public void int16b(byte[] a, int i, short v) { BA_SHORT_BIG.set(a, i, v); }
  public void int16l(byte[] a, int i, short v) { BA_SHORT_LITTLE.set(a, i, v); }

  public char uint16(byte[] a, int i, boolean big) { return (char)(big ? BA_CHAR_BIG : BA_CHAR_LITTLE).get(a, i); }
  public char uint16n(byte[] a, int i) { return (char)(BIG_ENDIAN ? BA_CHAR_BIG : BA_CHAR_LITTLE).get(a, i); }
  public char uint16b(byte[] a, int i) { return (char)BA_CHAR_BIG.get(a, i); }
  public char uint16l(byte[] a, int i) { return (char)BA_CHAR_LITTLE.get(a, i); }
  public void uint16(byte[] a, int i, char v, boolean big) { (big ? BA_CHAR_BIG : BA_CHAR_LITTLE).set(a, i, v); }
  public void uint16n(byte[] a, int i, char v) { (BIG_ENDIAN ? BA_CHAR_BIG : BA_CHAR_LITTLE).set(a, i, v); }
  public void uint16b(byte[] a, int i, char v) { BA_CHAR_BIG.set(a, i, v); }
  public void uint16l(byte[] a, int i, char v) { BA_CHAR_LITTLE.set(a, i, v); }

  public int int32(byte[] a, int i, boolean big) { return (int)(big ? BA_INT_BIG : BA_INT_LITTLE).get(a, i); }
  public int int32n(byte[] a, int i) { return (int)(BIG_ENDIAN ? BA_INT_BIG : BA_INT_LITTLE).get(a, i); }
  public int int32b(byte[] a, int i) { return (int)BA_INT_BIG.get(a, i); }
  public int int32l(byte[] a, int i) { return (int)BA_INT_LITTLE.get(a, i); }
  public void int32(byte[] a, int i, int v, boolean big) { (big ? BA_INT_BIG : BA_INT_LITTLE).set(a, i, v); }
  public void int32n(byte[] a, int i, int v) { (BIG_ENDIAN ? BA_INT_BIG : BA_INT_LITTLE).set(a, i, v); }
  public void int32b(byte[] a, int i, int v) { BA_INT_BIG.set(a, i, v); }
  public void int32l(byte[] a, int i, int v) { BA_INT_LITTLE.set(a, i, v); }

  public long int64(byte[] a, int i, boolean big) { return (long)(big ? BA_LONG_BIG : BA_LONG_LITTLE).get(a, i); }
  public long int64n(byte[] a, int i) { return (long)(BIG_ENDIAN ? BA_LONG_BIG : BA_LONG_LITTLE).get(a, i); }
  public long int64b(byte[] a, int i) { return (long)BA_LONG_BIG.get(a, i); }
  public long int64l(byte[] a, int i) { return (long)BA_LONG_LITTLE.get(a, i); }
  public void int64(byte[] a, int i, long v, boolean big) { (big ? BA_LONG_BIG : BA_LONG_LITTLE).set(a, i, v); }
  public void int64n(byte[] a, int i, long v) { (BIG_ENDIAN ? BA_LONG_BIG : BA_LONG_LITTLE).set(a, i, v); }
  public void int64b(byte[] a, int i, long v) { BA_LONG_BIG.set(a, i, v); }
  public void int64l(byte[] a, int i, long v) { BA_LONG_LITTLE.set(a, i, v); }

  public float float32(byte[] a, int i, boolean big) { return (float)(big ? BA_FLOAT_BIG : BA_FLOAT_LITTLE).get(a, i); }
  public float float32n(byte[] a, int i) { return (float)(BIG_ENDIAN ? BA_FLOAT_BIG : BA_FLOAT_LITTLE).get(a, i); }
  public float float32b(byte[] a, int i) { return (float)BA_FLOAT_BIG.get(a, i); }
  public float float32l(byte[] a, int i) { return (float)BA_FLOAT_LITTLE.get(a, i); }
  public void float32(byte[] a, int i, float v, boolean big) { (big ? BA_FLOAT_BIG : BA_FLOAT_LITTLE).set(a, i, v); }
  public void float32n(byte[] a, int i, float v) { (BIG_ENDIAN ? BA_FLOAT_BIG : BA_FLOAT_LITTLE).set(a, i, v); }
  public void float32b(byte[] a, int i, float v) { BA_FLOAT_BIG.set(a, i, v); }
  public void float32l(byte[] a, int i, float v) { BA_FLOAT_LITTLE.set(a, i, v); }

  public double float64(byte[] a, int i, boolean big) { return (double)(big ? BA_DOUBLE_BIG : BA_DOUBLE_LITTLE).get(a, i); }
  public double float64n(byte[] a, int i) { return (double)(BIG_ENDIAN ? BA_DOUBLE_BIG : BA_DOUBLE_LITTLE).get(a, i); }
  public double float64b(byte[] a, int i) { return (double)BA_DOUBLE_BIG.get(a, i); }
  public double float64l(byte[] a, int i) { return (double)BA_DOUBLE_LITTLE.get(a, i); }
  public void float64(byte[] a, int i, double v, boolean big) { (big ? BA_DOUBLE_BIG : BA_DOUBLE_LITTLE).set(a, i, v); }
  public void float64n(byte[] a, int i, double v) { (BIG_ENDIAN ? BA_DOUBLE_BIG : BA_DOUBLE_LITTLE).set(a, i, v); }
  public void float64b(byte[] a, int i, double v) { BA_DOUBLE_BIG.set(a, i, v); }
  public void float64l(byte[] a, int i, double v) { BA_DOUBLE_LITTLE.set(a, i, v); }
}
