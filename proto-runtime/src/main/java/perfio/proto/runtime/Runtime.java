package perfio.proto.runtime;

import perfio.BufferedInput;
import perfio.BufferedOutput;
import perfio.internal.StringInternals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Runtime {
  public static final int VARINT = 0; // int32, int64, uint32, uint64, sint32, sint64, bool, enum
  public static final int I64    = 1; // fixed64, sfixed64, double
  public static final int LEN    = 2; // string, bytes, embedded messages, packed repeated fields
  public static final int SGROUP = 3; // group start (deprecated)
  public static final int EGROUP = 4; // group end (deprecated)
  public static final int I32    = 5; // fixed32, sfixed32, float

  static int varintToSInt32(long v) {
    var i = (int)v;
    return (i >>> 1) ^ -(i & 1);
  }

  static long varintToSInt64(long v) {
    return (v >>> 1) ^ -(v & 1);
  }

  static long sint32ToVarint(int i) {
    var l = i & 0xffffffffL;
    return (l << 1) ^ (l >> 31);
  }

  static long sint64ToVarint(long l) {
    return (l << 1) ^ (l >> 63);
  }

  public static long parseVarint(BufferedInput in) throws IOException {
    long res = 0L;
    for(int shift = 0; shift < 64; shift += 7) {
      byte b = in.int8();
      res |= (b & 0x7FL) << shift;
      if((b & 0x80) == 0) return res;
    }
    throw new IOException("Invalid varint");
  }

  public static void writeVarint(BufferedOutput out, long v) throws IOException {
    while(true) {
      long b = v;
      v >>>= 7;
      if(v == 0) {
        out.int8((byte)b);
        return;
      }
      out.int8((byte)(b | 0x80));
    }
  }

  public static IOException invalidWireType(int wt, int field) {
    return new IOException("Invalid wire type "+wireTypeName(wt)+" for field "+field);
  }

  private static String wireTypeName(int wireType) {
    return switch(wireType) {
      case VARINT -> "VARINT";
      case I64    -> "I64";
      case LEN    -> "LEN";
      case SGROUP -> "SGROUP";
      case EGROUP -> "EGROUP";
      case I32    -> "I32";
      default     -> String.valueOf(wireType);
    };
  }

  public static int parseInt32(BufferedInput in) throws IOException { return (int)parseVarint(in); }
  public static long parseInt64(BufferedInput in) throws IOException { return parseVarint(in); }
  public static int parseFixed32(BufferedInput in) throws IOException { return in.int32(); }
  public static long parseFixed64(BufferedInput in) throws IOException { return in.int64(); }
  public static boolean parseBoolean(BufferedInput in) throws IOException { return parseVarint(in) != 0; }
  public static float parseFloat(BufferedInput in) throws IOException { return in.float32(); }
  public static double parseDouble(BufferedInput in) throws IOException { return in.float64(); }
  public static int parseSInt32(BufferedInput in) throws IOException { return varintToSInt32(parseVarint(in)); }
  public static long parseSInt64(BufferedInput in) throws IOException { return varintToSInt64(parseVarint(in)); }
  public static byte[] parseBytes(BufferedInput in) throws IOException { return in.bytes((int)parseLen(in)); }
  public static String parseString(BufferedInput in) throws IOException { return in.string((int)parseLen(in)); }
  public static long parseLen(BufferedInput in) throws IOException { return parseVarint(in); }

  public static void skip(BufferedInput in, int wireType) throws IOException {
    switch(wireType) {
      case VARINT -> parseVarint(in);
      case I32 -> in.skip(4);
      case I64 -> in.skip(8);
      case LEN -> in.skip(parseVarint(in));
      case SGROUP, EGROUP -> throw new IOException("TODO "+wireType); //TODO
      default -> throw new IOException("Invalid wire type "+wireType);
    }
  }

  public static void writeInt32(BufferedOutput out, int i) throws IOException { writeVarint(out, i & 0xffffffffL); }
  public static void writeInt64(BufferedOutput out, long l) throws IOException { writeVarint(out, l); }
  public static void writeFixed32(BufferedOutput out, int i) throws IOException { out.int32(i); }
  public static void writeFixed64(BufferedOutput out, long l) throws IOException { out.int64(l); }
  public static void writeBoolean(BufferedOutput out, boolean b) throws IOException { writeVarint(out, b ? 1L : 0L); }
  public static void writeFloat(BufferedOutput out, float f) throws IOException { out.float32(f); }
  public static void writeDouble(BufferedOutput out, double d) throws IOException { out.float64(d); }
  public static void writeSInt32(BufferedOutput out, int i) throws IOException { writeVarint(out, sint32ToVarint(i)); }
  public static void writeSInt64(BufferedOutput out, long l) throws IOException { writeVarint(out, sint64ToVarint(l)); }
  public static void writeLen(BufferedOutput out, long l) throws IOException { writeVarint(out, l); }

  public static void writeBytes(BufferedOutput out, byte[] bytes) throws IOException {
    writeLen(out, bytes.length);
    out.write(bytes);
  }

  public static void writeString(BufferedOutput out, String s) throws IOException {
    var l = s.length();
    if(l == 0) out.int8((byte)0);
    else {
      if(StringInternals.isLatin1(s)) {
        var v = StringInternals.value(s);
        if(!StringInternals.hasNegatives(v, 0, v.length)) {
          writeLen(out, v.length);
          out.write(v);
        } else writeBytes(out, s.getBytes(StandardCharsets.UTF_8));
      } else writeBytes(out, s.getBytes(StandardCharsets.UTF_8));
    }
  }

  public static void writePackedInt32(BufferedOutput out, IntList l) throws IOException {
    int lenMin = varintSize(l.len), lenMax = varintSize(l.len*10);
    if(lenMin == lenMax) {
      var b = out.reserve(lenMin);
      var l0 = out.totalBytesWritten();
      for(int i=0; i<l.len; i++) writeInt32(out, l.data[i]);
      var l1 = out.totalBytesWritten();
      writeLen(b, l1-l0);
      b.close();
    } else {
      var b = out.defer();
      for(int i=0; i<l.len; i++) writeInt32(b, l.data[i]);
      writeLen(out, b.totalBytesWritten());
      b.close();
    }
  }

  private static int varintSize(long v) {
    return ((Long.SIZE * 9 + (1 << 6)) - (Long.numberOfLeadingZeros(v) * 9)) >>> 6;
  }
}
