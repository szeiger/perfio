package perfio;

import perfio.internal.BufferUtil;

import java.io.IOException;

class SwitchingHeapBufferedInput extends HeapBufferedInput {
  protected final BufferIterator it;
  // bufferId1 is the currently used buffer. When there is a seam, the iterator has already been
  // advanced to the next buffer and bufferId2 holds its ID, otherwise null.
  private Object bufferId1, bufferId2;
  // The number of bytes that have already been copied from the iterator's current buffer.
  private int seamOverlap = 0;

  SwitchingHeapBufferedInput(BufferIterator it, boolean bigEndian) {
    super(BufferUtil.EMPTY_BUFFER, 0, 0, Long.MAX_VALUE, null, bigEndian, null);
    this.it = it;
  }

  protected SwitchingHeapBufferedInput(SwitchingHeapBufferedInput parent) {
    super(null, 0, 0, 0, parent, parent.bigEndian, null);
    this.it = parent.it;
  }

  @Override
  void copyBufferFrom(BufferedInput b) {
    super.copyBufferFrom(b);
    var bb = (SwitchingHeapBufferedInput)b;
    seamOverlap = bb.seamOverlap;
    bufferId1 = bb.bufferId1;
    bufferId2 = bb.bufferId2;
  }

  private void updateBuffer() {
    buf = it.buffer();
    pos = it.start();
    lim = it.end();
    totalBuffered += (lim - pos - seamOverlap);
    pos += seamOverlap;
    seamOverlap = 0;
  }

  String showContent() {
    StringBuilder b = new StringBuilder("[");
    for(int i = pos; i < lim; i++) {
      if(i != pos) b.append(",");
      b.append(Integer.toHexString(buf[i] & 0xff));
    }
    return b.append("]").toString();
  }

  protected void prepareAndFillBuffer(int count) throws IOException {
    checkState();
    //System.out.println("prepareAndFillBuffer("+count+") "+show()+", bufLen="+buf.length+", seamOverlap="+seamOverlap+", bufferId1="+bufferId1);
    if(totalBuffered < totalReadLimit) {
      while(available() < count) {
        //System.out.println("  available="+available());
        if(pos == lim) {
          if(seamOverlap == 0) {
            //System.out.println("  at limit, no seam: "+show()+", bufLen="+buf.length+", seamOverlap="+seamOverlap);
            if(bufferId1 != null) it.returnBuffer(bufferId1);
            if((bufferId1 = it.next()) == null) break;
            //System.out.println("    new bufferId1: "+bufferId1);
            updateBuffer();
          } else {
            assert(bufferId1 != null);
            //System.out.println("  at limit, seam: "+show()+", bufLen="+buf.length+", seamOverlap="+seamOverlap);
            assert(bufferId2 != null);
            it.returnBuffer(bufferId1);
            bufferId1 = bufferId2;
            bufferId2 = null;
            updateBuffer();
          }
        } else {
          assert(bufferId1 != null);
          if(pos + count > buf.length)
            shiftOrGrow(count);
          if(seamOverlap == 0) {
            //System.out.println("  has rest, no seam: "+show()+", bufLen="+buf.length+", seamOverlap="+seamOverlap);
            //System.out.println("    "+showContent());
            if((bufferId2 = it.next()) == null) break;
            var rem = count - available();
            var nlen = it.length();
            //System.out.println("    rem "+rem+", nlen "+nlen);
            if(rem < nlen) { // at least 1 byte will remain in `next` -> create a seam
              seamOverlap = rem;
              System.arraycopy(it.buffer(), it.start(), buf, lim, rem);
              lim += rem;
              //root.next.start += rem;
              totalBuffered += rem;
            } else { // `next` will be exhausted -> copy without seam
              System.arraycopy(it.buffer(), it.start(), buf, lim, nlen);
              lim += nlen;
              totalBuffered += nlen;
              it.returnBuffer(bufferId2);
              bufferId2 = null;
            }
          } else { // existing seam
            //System.out.println("  has rest, seam: "+show()+", bufLen="+buf.length+", seamOverlap="+seamOverlap);
            //System.out.println("    "+showContent());
            if(lim - pos <= seamOverlap) { // pos is in the seam -> switch to next buffer
              var a = lim - pos;
              assert(bufferId2 != null);
              it.returnBuffer(bufferId1);
              bufferId1 = bufferId2;
              bufferId2 = null;
              updateBuffer();
              pos -= a;
            } else {
              var rem = count - available();
              var nlen = it.length();
              if(rem < nlen) { // extend the current seam
                seamOverlap += rem;
                System.arraycopy(it.buffer(), it.start(), buf, lim, rem);
                lim += rem;
                //root.next.start += rem;
                totalBuffered += rem;
              } else { // copy the rest of the next buffer
                System.arraycopy(it.buffer(), it.start(), buf, lim, nlen);
                lim += nlen;
                totalBuffered += nlen;
                seamOverlap = 0;
                it.returnBuffer(bufferId2);
                bufferId2 = null;
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
    //System.out.println("done: "+showContent()+", bufferId1="+bufferId1);
  }

  BufferedInput createEmptyView() { return new SwitchingHeapBufferedInput(this); }
}
