package perfio;

import perfio.internal.LineBuffer;
import perfio.internal.StringInternals;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;


abstract sealed class DirectLineTokenizer extends LineTokenizer implements CloseableView permits DirectScalarLineTokenizer, DirectVectorizedLineTokenizer {
  final DirectBufferedInput bin;
  final MemorySegment ms;
  long start;

  DirectLineTokenizer(DirectBufferedInput bin, byte eolChar, byte preEolChar) throws IOException {
    super(eolChar, preEolChar);
    this.bin = bin;
    this.ms = bin.ms;
    this.start = bin.bbStart + bin.pos;
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
      if(closeUpstream) {
        bin.unlock();
        bin.close();
      } else {
        bin.reposition(start);
        bin.unlock();
      }
      markClosed();
    }
  }
}
