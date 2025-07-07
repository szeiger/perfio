package perfio;

import perfio.internal.BufferUtil;
import perfio.internal.LineBuffer;
import perfio.internal.MemoryAccessor;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

// This could be a lot simpler if we didn't have to do pagination but ByteBuffer is limited
// to 2 GB and direct MemorySegment access is much, much slower as of JDK 22.
final class DirectBufferedInput extends BufferedInput {
  MemorySegment bbSegment;
  final MemorySegment ms;
  private final Closeable closeable;

  DirectBufferedInput(ByteBuffer bb, MemorySegment bbSegment, int pos, int lim, long totalReadLimit, MemorySegment ms, Closeable closeable, BufferedInput parent, LineBuffer linebuf) {
    super(pos, lim, totalReadLimit, parent, bb.order() == ByteOrder.BIG_ENDIAN, linebuf);
    this.bbSegment = bbSegment;
    this.ms = ms;
    this.bb = bb;
    this.closeable = closeable;
  }

  long bbStart = 0L;

  @Override
  void bufferClosed(boolean closeUpstream) throws IOException {
    if(closeable != null && closeUpstream) closeable.close();
  }

  void copyBufferFrom(BufferedInput b) {
    super.copyBufferFrom(b);
    var db = (DirectBufferedInput)b;
    bbSegment = db.bbSegment;
    bbStart = db.bbStart;
  }

  @Override
  void clearBuffer() {
    super.clearBuffer();
    bbSegment = null;
  }

  protected void prepareAndFillBuffer(int count) throws IOException {
    checkState();
    if(ms != null && totalBuffered < totalReadLimit) {
      var a = available();
      var newStart = parentTotalOffset+totalBuffered-a;
      var newLen = Math.max(Math.min(totalReadLimit+parentTotalOffset-newStart, MaxDirectBufferSize), 0);
      bbSegment = ms.asSlice(newStart, newLen);
      bb = bbSegment.asByteBuffer().order(bb.order());
      bbStart = newStart;
      pos = 0;
      lim = (int)newLen;
      totalBuffered += newLen - a;
      if(totalBuffered >= totalReadLimit) {
        excessRead = (int)(totalBuffered-totalReadLimit);
        lim -= excessRead;
        totalBuffered -= excessRead;
      }
    }
  }

  public void bytes(byte[] a, int off, int len) throws IOException {
    var p = fwd(len);
    bb.get(p, a, off, len);
  }

  BufferedInput createEmptyView() { return new DirectBufferedInput(bb, null, 0, 0, 0L, ms, null, this, linebuf); }

  public long skip(long bytes) throws IOException {
    checkState();
    if(bytes > 0) {
      var skipAv = (int)Math.min(bytes, available());
      pos += skipAv;
      var rem = bytes - skipAv;
      var remTotal = totalReadLimit - totalBuffered;
      rem = Math.min(rem, remTotal);
      if(rem > 0 && ms != null) {
        var newStart = parentTotalOffset+totalBuffered+rem;
        var newLen = Math.min(totalReadLimit+parentTotalOffset-newStart, MaxDirectBufferSize);
        bb = ms.asSlice(newStart, newLen).asByteBuffer().order(bb.order());
        totalBuffered += rem + newLen;
        pos = 0;
        lim = (int)newLen;
        return skipAv + rem;
      } else return skipAv;
    } else return 0;
  }

  /// Move this buffer to the given absolute position, updating the ByteBuffer and local position if necessary.
  /// If the position is beyond the absolute limit, the buffer is moved to the limit instead.
  void reposition(long absPos) {
    absPos = Math.min(absPos, totalReadLimit);
    if(absPos < bbStart || absPos > bbStart + lim) {
      var offset = absPos % BufferUtil.VECTOR_LENGTH;
      var newStart = absPos - offset;
      var shift = newStart - bbStart;
      totalBuffered += shift;
      var newLen = Math.min(totalReadLimit+parentTotalOffset-newStart, MaxDirectBufferSize);
      bbSegment = ms.asSlice(newStart, newLen);
      bb = bbSegment.asByteBuffer().order(bb.order());
      bbStart = newStart;
      lim = (int)newLen;
    }
    pos = (int)(absPos - bbStart);
  }
}
