package perfio;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Accessors for private String methods for faster string encoding that can be used when running with
 * `--add-opens java.base/java.lang=ALL-UNNAMED`. Lookup and fallback are implemented via load-time initialization
 * of constant MethodHandles for optimal performance.
 */
class StringInternals {
  private static final MethodHandle isLatin1MH, valueMH, hasNegativesMH;

  /** Error encountered during initialization of internals call paths, or null if successful / manually disabled. */
  public static final Throwable internalAccessError;

  /** true if calls go to internal String methods, false for fallbacks. */
  public static final boolean internalAccessEnabled;

  public static boolean isLatin1(String s) throws Throwable { return (boolean)isLatin1MH.invokeExact(s); }
  public static byte[] value(String s) throws Throwable { return (byte[])valueMH.invokeExact(s); }
  public static boolean hasNegatives(byte[] ba, int off, int len) throws Throwable { return (boolean)hasNegativesMH.invokeExact(ba, off, len); }

  // Fallback implementations. These methods are used to special-case compact Latin-1 strings so we make
  // isLatin1() return false. Behavior of the other methods doesn't really matter because they won't be called.
  private static boolean dummy_isLatin1(String s) { return false; }
  private static byte[] dummy_value(String s) { return null; }
  private static boolean dummy_hasNegatives(byte[] ba, int off, int len) { return true; }

  static {
    MethodHandle isLatin1L, valueL, hasNegativesL;
    Throwable internalAccessErrorL = null;
    var internalAccessEnabledL = true;
    var disabled = "true".equals(System.getProperty("perfio.disableStringInternals"));
    try {
      if(disabled) throw new RuntimeException();

      var lookup = MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
      isLatin1L = lookup.findVirtual(String.class, "isLatin1", MethodType.methodType(Boolean.TYPE));
      valueL = lookup.findVirtual(String.class, "value", MethodType.methodType(Byte.TYPE.arrayType()));
      var stringCodingC = lookup.findClass("java.lang.StringCoding");
      hasNegativesL = lookup.findStatic(stringCodingC, "hasNegatives",
        MethodType.methodType(Boolean.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
      check(isLatin1L, valueL, hasNegativesL); // Perform some sanity checks
    } catch(Throwable t) {
      internalAccessEnabledL = false;
      if(!disabled) internalAccessErrorL = t;
      try {
        var lookup = MethodHandles.privateLookupIn(StringInternals.class, MethodHandles.lookup());
        isLatin1L = lookup.findStatic(StringInternals.class, "dummy_isLatin1", MethodType.methodType(Boolean.TYPE, String.class));
        valueL = lookup.findStatic(StringInternals.class, "dummy_value", MethodType.methodType(Byte.TYPE.arrayType(), String.class));
        hasNegativesL = lookup.findStatic(StringInternals.class, "dummy_hasNegatives", MethodType.methodType(Boolean.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
      } catch(Throwable t2) { throw new RuntimeException(t2); }
    }
    isLatin1MH = isLatin1L;
    valueMH = valueL;
    hasNegativesMH = hasNegativesL;
    internalAccessError = internalAccessErrorL;
    internalAccessEnabled = internalAccessEnabledL;
  }

  private static final void check(MethodHandle isLatin1L, MethodHandle valueL, MethodHandle hasNegativesL) throws Throwable {
    var s0 = "\u007f";
    var s1 = "\u00ff";
    if(!(boolean)isLatin1L.invokeExact(s0) || !(boolean)isLatin1L.invokeExact(s1))
      throw new RuntimeException("isLatin1() misbehaves - compact strings may be disabled");
    var v0 = (byte[])valueL.invokeExact(s0);
    var v1 = (byte[])valueL.invokeExact(s1);
    if(v0.length != 1 || v1.length != 1) throw new RuntimeException("value() misbehaves");
    if((boolean)hasNegativesL.invokeExact(v0, 0, v0.length) || !(boolean)hasNegativesL.invokeExact(v1, 0, v1.length))
      throw new RuntimeException("hasNegatives() misbehaves");
  }
}
