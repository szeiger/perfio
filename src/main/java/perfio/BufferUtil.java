package perfio;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class BufferUtil {
  private BufferUtil() {}

  /// Compute a new buffer size for the given size alignment (and assuming the current size respects
  /// this alignment, clamped at the maximum aligned value <= [Integer#MAX_VALUE].
  static int growBuffer(int current, int target, int align) {
    int l = current;
    while(l < target) {
      l *= 2;
      if(l < 0) return Integer.MAX_VALUE - align + 1;
    }
    return l;
  }

  static final VarHandle BA_LONG_BIG = bavh(Long.TYPE, true);
  static final VarHandle BA_INT_BIG = bavh(Integer.TYPE, true);
  static final VarHandle BA_SHORT_BIG = bavh(Short.TYPE, true);
  static final VarHandle BA_CHAR_BIG = bavh(Character.TYPE, true);
  static final VarHandle BA_DOUBLE_BIG = bavh(Double.TYPE, true);
  static final VarHandle BA_FLOAT_BIG = bavh(Float.TYPE, true);

  static final VarHandle BA_LONG_LITTLE = bavh(Long.TYPE, false);
  static final VarHandle BA_INT_LITTLE = bavh(Integer.TYPE, false);
  static final VarHandle BA_SHORT_LITTLE = bavh(Short.TYPE, false);
  static final VarHandle BA_CHAR_LITTLE = bavh(Character.TYPE, false);
  static final VarHandle BA_DOUBLE_LITTLE = bavh(Double.TYPE, false);
  static final VarHandle BA_FLOAT_LITTLE = bavh(Float.TYPE, false);

  private static VarHandle bavh(Class<?> cls, boolean be) {
    return MethodHandles.byteArrayViewVarHandle(cls.arrayType(), be ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
  }

  static final VarHandle BB_LONG_BIG = bbvh(Long.TYPE, true);
  static final VarHandle BB_INT_BIG = bbvh(Integer.TYPE, true);
  static final VarHandle BB_SHORT_BIG = bbvh(Short.TYPE, true);
  static final VarHandle BB_CHAR_BIG = bbvh(Character.TYPE, true);
  static final VarHandle BB_DOUBLE_BIG = bbvh(Double.TYPE, true);
  static final VarHandle BB_FLOAT_BIG = bbvh(Float.TYPE, true);

  static final VarHandle BB_LONG_LITTLE = bbvh(Long.TYPE, false);
  static final VarHandle BB_INT_LITTLE = bbvh(Integer.TYPE, false);
  static final VarHandle BB_SHORT_LITTLE = bbvh(Short.TYPE, false);
  static final VarHandle BB_CHAR_LITTLE = bbvh(Character.TYPE, false);
  static final VarHandle BB_DOUBLE_LITTLE = bbvh(Double.TYPE, false);
  static final VarHandle BB_FLOAT_LITTLE = bbvh(Float.TYPE, false);

  private static VarHandle bbvh(Class<?> cls, boolean be) {
    return MethodHandles.byteBufferViewVarHandle(cls.arrayType(), be ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
  }

  static MemorySegment mapReadOnlyFile(Path file) throws IOException {
    var a = Arena.ofAuto();
    try (var ch = FileChannel.open(file, StandardOpenOption.READ)) {
      return ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size(), a);
    }
  }

  static final boolean VECTOR_ENABLED;
  static final int VECTOR_LENGTH;
  static {
    var e = false;
    var vlen = 8; // minimum for computing a usable minimum buffer length
    try {
      // Just a sanity check. We accept any reasonable size. Even 64-bit vectors (SWAR) are faster than scalar.
      // Hopefully this will guarantee that the preferred species is actually vectorized (which is not the case
      // with the experimental preview API at the moment).
      e = VectorSupport.SPECIES.length() >= 8 && !"true".equals(System.getProperty("perfio.disableVectorized"));
      if(e) vlen = VectorSupport.SPECIES.length();
    } catch(NoClassDefFoundError t) {}
    VECTOR_ENABLED = e;
    VECTOR_LENGTH = vlen;
  }
}
