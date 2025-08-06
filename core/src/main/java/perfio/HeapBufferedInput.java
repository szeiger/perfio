package perfio;

import perfio.internal.BufferUtil;

abstract class HeapBufferedInput extends BufferedInput {
  HeapBufferedInput(byte[] buf, int pos, int lim, long totalReadLimit, BufferedInput viewParent, boolean bigEndian) {
    super(pos, lim, totalReadLimit, viewParent, bigEndian, null, null);
    this.buf = buf;
  }

  /// Shift the remaining buffer data to the left and/or reallocate the buffer to make room for
  /// `count` bytes past the current `pos`.
  void shiftOrGrow(int count) {
    var a = available();
    // Buffer shifts must be aligned to the vector size, otherwise VectorizedLineTokenizer
    // performance will tank after rebuffering even when all vector reads are aligned.
    var offset = a > 0 ? pos % BufferUtil.VECTOR_LENGTH : 0;
    if(count + offset > buf.length) {
      var buflen = buf.length;
      while(buflen < count + offset) buflen *= 2;
      var buf2 = new byte[buflen];
      if(a > 0) System.arraycopy(buf, pos, buf2, offset, a);
      buf = buf2;
    } else if(a > 0 && pos != offset) {
      System.arraycopy(buf, pos, buf, offset, a);
    }
    pos = offset;
    lim = a + offset;
  }
}
