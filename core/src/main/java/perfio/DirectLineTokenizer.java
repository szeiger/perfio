package perfio;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import perfio.internal.LineBuffer;
import perfio.internal.StringInternals;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static perfio.internal.VectorSupport.*;


abstract sealed class DirectLineTokenizer extends LineTokenizer implements CloseableView permits DirectScalarLineTokenizer, DirectVectorizedLineTokenizer {
  final BufferedInput bin;
  final MemorySegment ms;
  long start;
  final int initialPos;

  DirectLineTokenizer(BufferedInput bin, byte eolChar, byte preEolChar) throws IOException {
    super(eolChar, preEolChar);
    //System.out.println("Creating LineTokenizer from "+bin.show());
    this.bin = bin;
    this.ms = bin.ms;
    this.start = bin.bbStart + bin.pos;
    this.initialPos = bin.pos;
    bin.lock();
    bin.closeableView = this;
  }

  private final LineBuffer linebuf = new LineBuffer();

  @Override
  public void markClosed() {
    super.markClosed();
    bin.closeableView = null;
  }

  public LineTokenizer detach() { return this; }

  abstract String makeString(byte[] buf, int start, int len);

  String makeString(MemorySegment buf, long start, long llen) {
    return makeStringGeneric(buf, start, llen);
  }

  final String makeStringGeneric(MemorySegment buf, long start, long llen) {
    var len = (int)llen;
    var b = linebuf.get(len);
    MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, start, b, 0, len);
    return makeString(b, 0, len);
  }

  final String makeStringLatin1Internal(MemorySegment buf, long start, long llen) {
    var len = (int)llen;
    var a = new byte[len];
    MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, start, a, 0, len);
    return StringInternals.newString(a, StringInternals.LATIN1);
  }

  String emit(long start, long lfpos) {
    var end = lfpos > 0 && preEolChar != (byte)(-1) && ms.get(ValueLayout.JAVA_BYTE, lfpos-1) == preEolChar ? lfpos-1 : lfpos;
    return start == end ? "" : makeString(ms, start, end-start);
  }

  public void close(boolean closeUpstream) throws IOException {
    if(!closed) {
      bin.unlock();
      bin.pos = initialPos;
      if(closeUpstream) {
        bin.close();
      } else {
        var origStart = bin.bbStart + bin.pos;
        bin.skip(start-origStart);
      }
      markClosed();
    }
  }
}


abstract non-sealed class DirectScalarLineTokenizer extends DirectLineTokenizer {
  static DirectScalarLineTokenizer of(BufferedInput in, Charset charset, byte eol, byte preEol) throws IOException {
    if(charset == StandardCharsets.ISO_8859_1) {
      if(StringInternals.internalAccessEnabled) return new DirectScalarLineTokenizer(in, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
        @Override String makeString(MemorySegment buf, long start, long llen) { return makeStringLatin1Internal(buf, start, llen); }
      };
      else return new DirectScalarLineTokenizer(in, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
      };
    } else return new DirectScalarLineTokenizer(in, eol, preEol) {
      String makeString(byte[] buf, int start, int len) { return new String(buf, start, len, charset); }
    };
  }

  private long pos;
  private final long limit;

  DirectScalarLineTokenizer(BufferedInput bin, byte eolChar, byte preEolChar) throws IOException {
    super(bin, eolChar, preEolChar);
    this.pos = start;
    limit = bin.totalReadLimit;
    //System.out.println("  Created "+show());
  }

  String show() {
    return "start="+start+", pos="+pos+", limit="+limit;
  }

  @Override
  public void markClosed() {
    super.markClosed();
    pos = limit;
  }

  private String rest() throws IOException {
    checkState();
    if(start < pos) {
      var s = makeString(ms, start, pos-start);
      start = pos;
      return s;
    } else return null;
  }

  public final String readLine() throws IOException {
    var p = pos;
    while(p < limit) {
      var b = ms.get(ValueLayout.JAVA_BYTE, p);
      p += 1;
      if(b == eolChar) {
        var s = emit(start, p-1);
        start = p;
        pos = p;
        return s;
      }
    }
    pos = p;
    return rest();
  }
}


/// A vectorized implementation of [[DirectLineTokenizer]].
///
/// The parser uses explicit SIMD loops with up to 512 bits / 64 lanes per vector (depending on
/// hardware and JVM support).
abstract non-sealed class DirectVectorizedLineTokenizer extends DirectLineTokenizer {
  static DirectVectorizedLineTokenizer of(BufferedInput in, Charset charset, byte eol, byte preEol) throws IOException {
    if(charset == StandardCharsets.ISO_8859_1) {
      if(StringInternals.internalAccessEnabled) return new DirectVectorizedLineTokenizer(in, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
        @Override String makeString(MemorySegment buf, long start, long llen) { return makeStringLatin1Internal(buf, start, llen); }
      };
      else return new DirectVectorizedLineTokenizer(in, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
      };
    } else return new DirectVectorizedLineTokenizer(in, eol, preEol) {
      String makeString(byte[] buf, int start, int len) { return new String(buf, start, len, charset); }
    };
  }

  private long vpos;
  private final long limit;
  private long mask = 0L;
  private final ByteVector eolVector;

  DirectVectorizedLineTokenizer(BufferedInput bin, byte eolChar, byte preEolChar) throws IOException {
    super(bin, eolChar, preEolChar);
    this.vpos = start - VLEN;
    this.limit = ms.byteSize();
    this.eolVector = ByteVector.broadcast(SPECIES, eolChar);
  }

  @Override
  public void markClosed() {
    super.markClosed();
    mask = 0L;
    vpos = Long.MAX_VALUE - VLEN;
  }

  private String rest() throws IOException {
    String r;
    if(vpos != Long.MAX_VALUE) {
      var l = limit-start;
      mask = 0;
      if(l != 0) {
        var s = makeString(ms, start, l);
        start = limit;
        r = s;
      } else r = null;
    } else {
      checkState();
      r = null;
    }
    vpos = Long.MAX_VALUE - VLEN;
    return r;
  }

  public String readLine() throws IOException {
    while(true) {
      var f = Long.numberOfTrailingZeros(mask);
      if(f < VLEN) {
        var lfpos = vpos+f;
        var s = emit(start, lfpos);
        start = lfpos+1;
        mask &= ~(1L<<f);
        return s;
      }
      vpos += VLEN;
      if(vpos <= limit-VLEN) mask = computeSimpleMask();
      else if(vpos < limit) mask = computeMask();
      else return rest();
    }
  }

  private long computeSimpleMask() {
    return ByteVector.fromMemorySegment(SPECIES, ms, vpos, ByteOrder.BIG_ENDIAN).compare(VectorOperators.EQ, eolVector).toLong();
  }

  private long computeMask() {
    var excess = vpos+VLEN-limit;
    var v = excess <= 0
        ? ByteVector.fromMemorySegment(SPECIES, ms, vpos, ByteOrder.BIG_ENDIAN)
        : ByteVector.fromMemorySegment(SPECIES, ms, vpos, ByteOrder.BIG_ENDIAN, VectorMask.fromLong(SPECIES, FULL_MASK >>> excess));
    // Seems to be slightly faster than comparing against a scalar:
    return v.compare(VectorOperators.EQ, eolVector).toLong();
  }
}
