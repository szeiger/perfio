package perfio;

import perfio.internal.LineBuffer;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// This could be a lot simpler if we didn't have to do pagination but ByteBuffer is limited
// to 2 GB and direct MemorySegment access is much, much slower as of JDK 22.
final class DirectBufferedInput extends BufferedInput {

  DirectBufferedInput(ByteBuffer bb, int pos, int lim, long totalReadLimit, MemorySegment ms, Closeable closeable, BufferedInput parent, LineBuffer linebuf) {
    super(null, pos, lim, totalReadLimit, parent, bb.order() == ByteOrder.BIG_ENDIAN, linebuf, ms, bb, closeable);
  }

  void copyBufferFrom(BufferedInput b) {
    super.copyBufferFrom(b);
    var db = (DirectBufferedInput)b;
  }

  protected void prepareAndFillBuffer(int count) throws IOException {
    checkState();
    if(ms != null && totalBuffered < totalReadLimit) {
      var a = available();
      var newStart = parentTotalOffset+totalBuffered-a;
      var newLen = Math.max(Math.min(totalReadLimit+parentTotalOffset-newStart, MaxDirectBufferSize), 0);
      MemorySegment bbSegment = ms.asSlice(newStart, newLen);
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

  BufferedInput createEmptyView() { return new DirectBufferedInput(bb, 0, 0, 0L, ms, null, this, linebuf); }

  @Override
  String show() {
    return super.show()+", bbStart="+bbStart+", ms.size="+ms.byteSize();
  }

  public long skip(long bytes) throws IOException {
    checkState();
    if(bytes > 0) {
      //System.out.println("before skip("+bytes+"): "+show());
      var skipAv = (int)Math.min(bytes, available());
      pos += skipAv;
      var rem = bytes - skipAv;
      var remTotal = totalReadLimit - totalBuffered;
      rem = Math.min(rem, remTotal);
      if(rem > 0 && ms != null) {
        var newStart = parentTotalOffset+totalBuffered+rem;
        var newLen = Math.min(totalReadLimit+parentTotalOffset-newStart, MaxDirectBufferSize);
        bb = ms.asSlice(newStart, newLen).asByteBuffer().order(bb.order());
        bbStart = newStart;
        totalBuffered += rem + newLen;
        pos = 0;
        lim = (int)newLen;
        //System.out.println("after skip("+bytes+") 1: pos="+pos+", bbStart="+bbStart+", ret="+(skipAv+rem));
        return skipAv + rem;
      } else {
        //System.out.println("after skip("+bytes+") 2: pos="+pos+", bbStart="+bbStart+", ret="+skipAv);
        return skipAv;
      }
    } else return 0;
  }
}
