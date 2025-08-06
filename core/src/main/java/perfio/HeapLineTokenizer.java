package perfio;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static perfio.internal.VectorSupport.*;


abstract sealed class HeapLineTokenizer extends LineTokenizer implements CloseableView permits HeapScalarLineTokenizer, HeapVectorizedLineTokenizer {
  final BufferedInput parentBin;
  final BufferedInput bin;

  HeapLineTokenizer(BufferedInput parentBin, byte eolChar, byte preEolChar) throws IOException {
    super(eolChar, preEolChar);
    this.parentBin = parentBin;
    this.bin = parentBin.identicalView(this);
  }

  abstract String makeString(byte[] buf, int start, int len);

  @Override
  public void markClosed() {
    super.markClosed();
  }

  public LineTokenizer detach() throws IOException {
    bin.detach();
    return this;
  }

  String emit(int start, int lfpos) {
    var end = lfpos > 0 && preEolChar != (byte)(-1) && bin.buf[lfpos-1] == preEolChar ? lfpos-1 : lfpos;
    return start == end ? "" : makeString(bin.buf, start, end-start);
  }

  public void close(boolean closeUpstream) throws IOException {
    if(!closed) {
      if(closeUpstream) parentBin.close();
      else bin.close();
      markClosed();
    }
  }
}


abstract non-sealed class HeapScalarLineTokenizer extends HeapLineTokenizer {
  static HeapScalarLineTokenizer of(BufferedInput in, Charset charset, byte eol, byte preEol) throws IOException {
    if(charset == StandardCharsets.ISO_8859_1) return new HeapScalarLineTokenizer(in, eol, preEol) {
      String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
    };
    else return new HeapScalarLineTokenizer(in, eol, preEol) {
      String makeString(byte[] buf, int start, int len) { return new String(buf, start, len, charset); }
    };
  }

  HeapScalarLineTokenizer(BufferedInput parentBin, byte eolChar, byte preEolChar) throws IOException {
    super(parentBin, eolChar, preEolChar);
  }

  private String rest(int p) {
    if(bin.pos < p) {
      var s = makeString(bin.buf, bin.pos, p-bin.pos);
      bin.pos = p;
      return s;
    } else return null;
  }

  public final String readLine() throws IOException {
    //System.out.println("readLine() pos="+bin.pos+", lim="+bin.lim+"");
    var eol = eolChar;
    var bp = bin.pos;
    var p = bin.pos;
    while(true) {
      while(p < bin.lim) {
        var b = bin.buf[p];
        p += 1;
        if(b == eol) {
          bin.pos = p;
          //System.out.println("  emit("+bp+", "+(p-1)+")");
          return emit(bp, p-1);
        }
      }
      var oldav = bin.available();
      //System.out.println("  before fill: pos="+bin.pos+", lim="+bin.lim+", buflen="+bin.buf.length);
      bin.prepareAndFillBuffer((p-bin.pos)+1);
      //System.out.println("  after fill: pos="+bin.pos+", lim="+bin.lim+", buflen="+bin.buf.length);
      p -= bp - bin.pos;
      if(oldav == bin.available()) {
        //System.out.println("  rest("+p+")");
        return rest(p);
      }
      bp = bin.pos;
    }
  }
}


/// A vectorized implementation of [[HeapLineTokenizer]].
///
/// The parser uses explicit SIMD loops with up to 512 bits / 64 lanes per vector (depending on
/// hardware and JVM support).
abstract non-sealed class HeapVectorizedLineTokenizer extends HeapLineTokenizer {
  static HeapVectorizedLineTokenizer of(BufferedInput in, Charset charset, byte eol, byte preEol) throws IOException {
    if(charset == StandardCharsets.ISO_8859_1) return new HeapVectorizedLineTokenizer(in, eol, preEol) {
      String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
    };
    else return new HeapVectorizedLineTokenizer(in, eol, preEol) {
      String makeString(byte[] buf, int start, int len) { return new String(buf, start, len, charset); }
    };
  }

  private int vpos; // start of the current vector in buf
  private long mask = 0L; // vector mask that marks the LFs
  private final ByteVector eolVector;

  HeapVectorizedLineTokenizer(BufferedInput parentBin, byte eolChar, byte preEolChar) throws IOException {
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
