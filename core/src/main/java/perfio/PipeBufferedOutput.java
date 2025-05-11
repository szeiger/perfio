package perfio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/// A [BufferedOutput] which makes its data available for concurrent reading from another thread
/// via [#toBufferedInput()]  or [#toInputStream()].
public final class PipeBufferedOutput extends BlockFlushingBufferedOutput {
  private final AtomicBoolean connected = new AtomicBoolean(false);
  private final BlockingQueue<BufferedOutput> queue;
  private final BlockingQueue<BufferedOutput> returnQueue = new LinkedBlockingQueue<>();

  PipeBufferedOutput(boolean bigEndian, int initialBufferSize, int blockDepth) {
    super(bigEndian, initialBufferSize);
    queue = new ArrayBlockingQueue<>(blockDepth);
  }

  @Override
  public PipeBufferedOutput order(ByteOrder order) {
    super.order(order);
    return this;
  }

  void put(BufferedOutput b) throws IOException {
    BufferedOutput r;
    while((r = returnQueue.poll()) != null)
      if(r != this) returnToCache((NestedBufferedOutput)r);
    try { queue.put(b); }
    catch(InterruptedException ex) { throw new IOException("Pipe transfer interrupted", ex); }
  }

  @Override
  void closeUpstream() throws IOException {
    super.closeUpstream();
    put(QueuedBufferIterator.END_MARKER);
  }

  /// Create a new [InputStream] that reads the data as it is written. This method is
  /// thread-safe. Only one call to [#toInputStream()] or [#toBufferedInput()] is allowed.
  public InputStream toInputStream() throws IOException {
    return new BufferIteratorInputStream(bufferIterator());
  }

  /// Create a new [BufferedInput] that reads the data as it is written. This method is
  /// thread-safe. Only one call to [#toInputStream()] or [#toBufferedInput()] is allowed.
  public BufferedInput toBufferedInput() throws IOException {
    return new SwitchingHeapBufferedInput(bufferIterator(), bigEndian);
  }

  BufferIterator bufferIterator() throws IOException {
    if(!connected.compareAndSet(false, true)) throw new IOException("Pipe is already connected");
    return new QueuedBufferIterator(queue, returnQueue);
  }
}


final class QueuedBufferIterator extends BufferIterator {
  public static final BufferedOutput END_MARKER = BufferedOutput.growing(0);

  static { try { END_MARKER.close(); } catch (IOException ignored) {} }

  private final BlockingQueue<BufferedOutput> queue, returnQueue;
  private byte[] buffer;
  private int start, end;
  private boolean finished;

  QueuedBufferIterator(BlockingQueue<BufferedOutput> queue, BlockingQueue<BufferedOutput> returnQueue) {
    this.queue = queue;
    this.returnQueue = returnQueue;
  }

  public byte[] buffer() { return buffer; }
  public int start() { return start; }
  public int end() { return end; }

  public Object next() throws IOException {
    if(finished) return null;
    BufferedOutput b;
    try { b = queue.take(); }
    catch(InterruptedException ex) { throw new IOException("Pipe transfer interrupted", ex); }
    //System.out.println("Took "+(b.pos-b.start));
    if(b == END_MARKER) {
      finished = true;
      buffer = null;
      return null;
    }
    buffer = b.buf;
    start = b.start;
    end = b.pos;
    return b;
  }

  public void returnBuffer(Object id) {
    if(!finished) returnQueue.offer((BufferedOutput)id);
  }
}
