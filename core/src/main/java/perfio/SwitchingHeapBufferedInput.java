package perfio;

import java.io.EOFException;
import java.io.IOException;

final class SwitchingHeapBufferedInput extends HeapBufferedInput {
  private final BufferIterator it;
  private int seamOverlap = 0;

  SwitchingHeapBufferedInput(BufferIterator it, boolean bigEndian) {
    super(it.buffer(), it.start(), it.end(), Long.MAX_VALUE, null, null, bigEndian);
    this.it = it;
  }

  private SwitchingHeapBufferedInput(SwitchingHeapBufferedInput parent) {
    super(null, 0, 0, 0, null, parent, parent.bigEndian);
    this.it = parent.it;
  }

  @Override
  void copyBufferFrom(BufferedInput b) {
    super.copyBufferFrom(b);
    seamOverlap = ((SwitchingHeapBufferedInput)b).seamOverlap;
  }

  private void updateBuffer() {
    buf = it.buffer();
    pos = it.start();
    lim = it.end();
    totalBuffered += (lim - pos);
  }

  void prepareAndFillBuffer(int count) throws IOException {
    checkState();
    if(totalBuffered < totalReadLimit) {
      while(available() < count) {
        if(pos == lim) {
          if(seamOverlap != 0) {
            updateBuffer();
            pos += seamOverlap;
            seamOverlap = 0;
          } else {
            if(!it.next()) break;
            updateBuffer();
          }
        } else {
          if(pos + count > buf.length)
            shiftOrGrow(count);
          if(seamOverlap == 0) {
            if(!it.next()) break;
            var rem = count - available();
            var nlen = it.length();
            if(rem < nlen) { // at least 1 byte will remain in `next` -> create a seam
              seamOverlap = rem;
              System.arraycopy(it.buffer(), it.start(), buf, pos, rem);
              lim += rem;
              //root.next.start += rem;
              totalBuffered += rem;
            } else { // `next` will be exhausted -> copy without seam
              System.arraycopy(it.buffer(), it.start(), buf, pos, nlen);
              lim += nlen;
              totalBuffered += nlen;
            }
          } else { // existing seam
            if(lim - pos <= seamOverlap) { // pos is in the seam -> switch to next buffer
              var a = lim - pos;
              updateBuffer();
              pos -= a;
              pos += seamOverlap;
              seamOverlap = 0;
            } else {
              var rem = count - available();
              var nlen = it.length();
              if(rem < nlen) { // extend the current seam
                seamOverlap += rem;
                System.arraycopy(it.buffer(), it.start(), buf, pos, rem);
                lim += rem;
                //root.next.start += rem;
                totalBuffered += rem;
              } else { // copy the rest of the next buffer
                System.arraycopy(it.buffer(), it.start(), buf, pos, nlen);
                lim += nlen;
                totalBuffered += nlen;
                seamOverlap = 0;
              }
            }
          }
        }
      }
      if(totalBuffered > totalReadLimit) {
        var e = (int)(totalBuffered - totalReadLimit);
        excessRead += e;
        totalBuffered -= e;
        lim -= e;
      }
    }
  }

  BufferedInput createEmptyView() { return new SwitchingHeapBufferedInput(this); }

  // `bytes` and `skip` are implemented in terms of `request(1)` so they don't have to interact
  // with the low-level buffer management. Requesting 1 byte will never create a seam so there
  // is no unnecessary copying.

  public void bytes(byte[] a, int off, int len) throws IOException {
    var tot = totalBytesRead() + len;
    if(tot < 0 || tot > totalReadLimit) throw new EOFException();
    while(len > 0) {
      request(1);
      if(available() == 0) throw new EOFException();
      var l = Math.min(len, available());
      if(l > 0) {
        System.arraycopy(buf, pos, a, off, l);
        pos += l;
        off += l;
        len -= l;
      }
    }
  }

  public long skip(final long bytes) throws IOException {
    checkState();
    final var limited = Math.min(bytes, totalReadLimit - totalBytesRead());
    var rem = limited;
    while(rem > 0) {
      request(1);
      if(available() == 0) return limited - rem;
      var l = Math.min(rem, available());
      if(l > 0) {
        pos += (int)l;
        rem -= l;
      }
    }
    return limited;
  }
}
