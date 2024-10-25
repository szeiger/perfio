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

  // The view is already positioned at the start of the line so we can simply return control to the parent
  public BufferedInput end() throws IOException {
    if(!closed) {
      bin.close();
      markClosed();
    }
    return parentBin;
  }

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

  public void close() throws IOException {
    if(!closed) {
      parentBin.close();
      markClosed();
    }
  }
}
