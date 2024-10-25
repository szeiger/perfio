package perfio;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static perfio.VectorSupport.*;

/// A vectorized implementation of [[LineTokenizer]].
///
/// The parser uses explicit SIMD loops with up to 512 bits / 64 lanes per vector (depending on
/// hardware and JVM support).
public class VectorizedLineTokenizer {
  private VectorizedLineTokenizer() {}

  static LineTokenizer of(BufferedInput in, Charset charset, byte eol, byte preEol) throws IOException {
    if(in instanceof HeapBufferedInput h) {
      if(charset == StandardCharsets.ISO_8859_1) return new HeapVectorizedLineTokenizer(h, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
      };
      else return new HeapVectorizedLineTokenizer(h, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, start, len, charset); }
      };
    } else {
      var i = (DirectBufferedInput)in;
      if(charset == StandardCharsets.ISO_8859_1) {
        if(StringInternals.internalAccessEnabled) return new DirectVectorizedLineTokenizer(i, eol, preEol) {
          String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
          @Override String makeString(MemorySegment buf, long start, long llen) { return makeStringLatin1Internal(buf, start, llen); }
        };
        else return new DirectVectorizedLineTokenizer(i, eol, preEol) {
          String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
        };
      } else return new DirectVectorizedLineTokenizer(i, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, start, len, charset); }
      };
    }
  }

  static LineTokenizer of(BufferedInput in, Charset charset) throws IOException { return of(in, charset, (byte)'\n', (byte)'\r'); }
}


abstract non-sealed class HeapVectorizedLineTokenizer extends HeapLineTokenizer {
  private int vpos; // start of the current vector in buf
  private long mask = 0L; // vector mask that marks the LFs
  private final ByteVector eolVector;

  HeapVectorizedLineTokenizer(HeapBufferedInput parentBin, byte eolChar, byte preEolChar) throws IOException {
    super(parentBin, eolChar, preEolChar);
    this.vpos = bin.pos - VLEN;
    this.mask = 0L;
    this.eolVector = ByteVector.broadcast(SPECIES, eolChar);
    if(bin.pos < bin.lim) {
      // Make sure the position is aligned if we have buffered data.
      // Otherwise we leave the initial buffering to the main loop in readLine().
      vpos += VLEN;
      alignPosAndComputeMask();
    }
  }

  private int buffer() throws IOException {
    if(vpos == Integer.MAX_VALUE) {
      vpos = Integer.MAX_VALUE - VLEN;
      return 0;
    }
    var scanned = bin.lim - bin.pos;
    var oldav = bin.available();
    bin.prepareAndFillBuffer(oldav + VLEN);
    if(oldav == bin.available()) return 0;
    vpos = bin.pos + scanned;
    alignPosAndComputeMask();
    return 1;
  }

  @Override
  public void markClosed() {
    super.markClosed();
    mask = 0L;
    vpos = Integer.MAX_VALUE - VLEN;
  }

  private void alignPosAndComputeMask() {
    var offsetInVector = vpos % VLEN;
    vpos -= offsetInVector;
    mask = computeMask() & (-1L >>> offsetInVector << offsetInVector);
  }

  private String rest() throws IOException {
    checkState();
    String r;
    if(vpos != Integer.MAX_VALUE) {
      var l = bin.available();
      mask = 0;
      if(l != 0) {
        var s = makeString(bin.buf, bin.pos, l);
        bin.pos = bin.lim;
        r = s;
      } else r = null;
    } else r = null;
    vpos = Integer.MAX_VALUE - VLEN;
    return r;
  }

  public final String readLine() throws IOException {
    while(true) {
      var f = Long.numberOfTrailingZeros(mask);
      //println(s"readLine(): ${bin.pos}, ${bin.lim}, $vpos, $mask -> $f")
      if(f < VLEN) {
        var lfpos = vpos+f;
        var s = emit(bin.pos, lfpos);
        bin.pos = lfpos+1;
        mask &= ~(1L<<f);
        return s;
      }
      vpos += VLEN;
      if(vpos <= bin.lim-VLEN) mask = computeSimpleMask();
      else if(vpos < bin.lim) mask = computeMask();
      else if(buffer() == 0) return rest();
    }
  }
  private long computeSimpleMask() {
    return ByteVector.fromArray(SPECIES, bin.buf, vpos).compare(VectorOperators.EQ, eolVector).toLong();
  }

  private long computeMask() {
    var excess = vpos+VLEN-bin.lim;
    var v = excess <= 0
        ? ByteVector.fromArray(SPECIES, bin.buf, vpos)
        : ByteVector.fromArray(SPECIES, bin.buf, vpos, VectorMask.fromLong(SPECIES, FULL_MASK >>> excess));
    // Seems to be slightly faster than comparing against a scalar:
    return v.compare(VectorOperators.EQ, eolVector).toLong();
  }
}


abstract non-sealed class DirectVectorizedLineTokenizer extends DirectLineTokenizer {
  private long vpos;
  private final long limit;
  private long mask = 0L;
  private final ByteVector eolVector;

  DirectVectorizedLineTokenizer(DirectBufferedInput bin, byte eolChar, byte preEolChar) throws IOException {
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
