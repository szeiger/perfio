package perfio;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

class ScalarLineTokenizer {
  private ScalarLineTokenizer() {}

  static LineTokenizer of(BufferedInput in, Charset charset, byte eol, byte preEol) throws IOException {
    if(in instanceof HeapBufferedInput h) {
      if(charset == StandardCharsets.ISO_8859_1) return new HeapScalarLineTokenizer(h, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
      };
      else return new HeapScalarLineTokenizer(h, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, start, len, charset); }
      };
    } else {
      var i = (DirectBufferedInput)in;
      if(charset == StandardCharsets.ISO_8859_1) {
        if(StringInternals.internalAccessEnabled) return new DirectScalarLineTokenizer(i, eol, preEol) {
          String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
          @Override String makeString(MemorySegment buf, long start, long llen) { return makeStringLatin1Internal(buf, start, llen); }
        };
        else return new DirectScalarLineTokenizer(i, eol, preEol) {
          String makeString(byte[] buf, int start, int len) { return new String(buf, 0, start, len); }
        };
      } else return new DirectScalarLineTokenizer(i, eol, preEol) {
        String makeString(byte[] buf, int start, int len) { return new String(buf, start, len, charset); }
      };
    }
  }

  static LineTokenizer of(BufferedInput in, Charset charset) throws IOException { return of(in, charset, (byte)'\n', (byte)'\r'); }
}


abstract non-sealed class HeapScalarLineTokenizer extends HeapLineTokenizer {
  HeapScalarLineTokenizer(HeapBufferedInput parentBin, byte eolChar, byte preEolChar) throws IOException {
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
    var eol = eolChar;
    var bp = bin.pos;
    var p = bin.pos;
    while(true) {
      while(p < bin.lim) {
        var b = bin.buf[p];
        p += 1;
        if(b == eol) {
          bin.pos = p;
          return emit(bp, p-1);
        }
      }
      var oldav = bin.available();
      bin.prepareAndFillBuffer(1);
      p -= bp - bin.pos;
      if(oldav == bin.available()) return rest(p);
      bp = bin.pos;
    }
  }
}


abstract non-sealed class DirectScalarLineTokenizer extends DirectLineTokenizer {
  private long pos;
  private final long limit;

  DirectScalarLineTokenizer(DirectBufferedInput bin, byte eolChar, byte preEolChar) throws IOException {
    super(bin, eolChar, preEolChar);
    this.pos = start;
    limit = bin.totalReadLimit;
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
