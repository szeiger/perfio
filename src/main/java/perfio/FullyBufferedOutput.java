package perfio;

import java.io.IOException;
import java.util.Arrays;

public final class FullyBufferedOutput extends CacheRootBufferedOutput {

  FullyBufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, int initialBufferSize, boolean fixed) {
    super(buf, bigEndian, start, pos, lim, initialBufferSize, fixed, Long.MAX_VALUE);
  }

  private byte[] outbuf = null;
  private int outstart, outpos = 0; //TODO is outstart used correctly?

  void flushBlocks(boolean forceFlush) throws IOException {
    while(next != this) {
      var b = next;
      if(!b.closed) return;
      var blen = b.pos - b.start;
      if(blen > 0) {
        if(b.sharing == SHARING_LEFT) {
          var n = b.next;
          n.start = b.start;
          n.totalFlushed -= blen;
          b.unlinkAndReturn();
        } else flushSingle(b, true);
      } else b.unlinkAndReturn();
    }
  }

  private void flushSingle(BufferedOutput b, boolean unlink) throws IOException {
    var blen = b.pos - b.start;
    if(outbuf == null) {
      outbuf = b.buf;
      outstart = b.start;
      outpos = b.pos;
      if(unlink) b.unlinkOnly();
    } else {
      var tot = totalBytesWritten();
      if((outbuf.length - outpos) < tot) {
        if(tot + outpos > Integer.MAX_VALUE) throw new IOException("Buffer exceeds maximum array length");
        growOutBuffer((int)(tot + outpos));
      }
      System.arraycopy(b.buf, b.start, outbuf, outpos, blen);
      outpos += blen;
      if(unlink) b.unlinkAndReturn();
    }
  }

  private void growOutBuffer(int target) {
    var buflen = BufferUtil.growBuffer(outbuf.length, target, 1);
    if(buflen > outbuf.length) outbuf = Arrays.copyOf(outbuf, buflen);
  }

  @Override
  void closeUpstream() throws IOException {
    flushSingle(this, false);
  }

  void flushUpstream() {}

  private void checkClosed() throws IOException {
    if(!closed) throw new IOException("Cannot access buffer before closing");
  }

  public byte[] copyToByteArray() throws IOException { return Arrays.copyOf(buffer(), length()); }

  public byte[] buffer() throws IOException {
    checkClosed();
    return outbuf;
  }

  public int length() throws IOException {
    checkClosed();
    return outpos;
  }
}
