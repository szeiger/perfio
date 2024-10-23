package perfio;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;


abstract sealed class DirectLineTokenizer extends LineTokenizer implements CloseableView permits DirectScalarLineTokenizer, DirectVectorizedLineTokenizer {
  final DirectBufferedInput bin;
  protected final MemorySegment ms;
  protected long start;

  protected DirectLineTokenizer(DirectBufferedInput bin, byte eolChar, byte preEolChar) throws IOException {
    super(eolChar, preEolChar);
    this.bin = bin;
    this.ms = bin.ms;
    this.start = bin.bbStart + bin.pos;
    bin.lock();
    bin.closeableView = this;
  }

  private byte[] linebuf = new byte[256];

  public BufferedInput end() {
    if(!closed) {
      bin.reposition(start);
      bin.unlock();
      markClosed();
    }
    return bin;
  }

  @Override
  public void markClosed() {
    super.markClosed();
    bin.closeableView = null;
  }

  public LineTokenizer detach() { return this; }

  protected abstract String makeString(byte[] buf, int start, int len);

  private byte[] extendBuffer(int len) {
    var buflen = linebuf.length;
    while(buflen < len) buflen *= 2;
    return new byte[buflen];
  }

  protected String makeString(MemorySegment buf, long start, long llen) {
    return makeStringGeneric(buf, start, llen);
  }

  protected final String makeStringGeneric(MemorySegment buf, long start, long llen) {
    var len = (int)llen;
    if(linebuf.length < len) linebuf = extendBuffer(len);
    MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, start, linebuf, 0, len);
    return makeString(linebuf, 0, len);
  }

  protected final String makeStringLatin1Internal(MemorySegment buf, long start, long llen) {
    var len = (int)llen;
    var a = new byte[len];
    MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, start, a, 0, len);
    return StringInternals.newString(a, StringInternals.LATIN1);
  }

  protected String emit(long start, long lfpos) {
    var end = lfpos > 0 && preEolChar != (byte)(-1) && ms.get(ValueLayout.JAVA_BYTE, lfpos-1) == preEolChar ? lfpos-1 : lfpos;
    return start == end ? "" : makeString(ms, start, end-start);
  }

  public void close() throws IOException {
    if(!closed) {
      bin.unlock();
      bin.close();
      markClosed();
    }
  }
}
