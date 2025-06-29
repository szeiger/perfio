package perfio;

import java.io.IOException;


abstract sealed class HeapLineTokenizer extends LineTokenizer implements CloseableView permits HeapScalarLineTokenizer, HeapVectorizedLineTokenizer {
  final HeapBufferedInput parentBin;
  final HeapBufferedInput bin;

  HeapLineTokenizer(HeapBufferedInput parentBin, byte eolChar, byte preEolChar) throws IOException {
    super(eolChar, preEolChar);
    this.parentBin = parentBin;
    this.bin = (HeapBufferedInput)parentBin.identicalView();
    bin.closeableView = this;
  }

  abstract String makeString(byte[] buf, int start, int len);

  @Override
  public void markClosed() {
    super.markClosed();
    bin.closeableView = null;
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
