package perfio;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/// An output filter for gzip compression similar to [java.util.zip.GZIPOutputStream], but with
/// support for asynchronous and parallel compression.
/// 
/// Asynchronous processing runs the compression on another thread. This has a very small overhead
/// and is usually faster unless the main thread can produce output at the maximum speed.
/// 
/// Parallel processing uses multiple threads to compress blocks (with a default size of 64k,
/// independent of the BufferedOutput block size) in parallel which is much faster. Output sizes
/// can be a bit larger due to using a new dictionary for each block.
public abstract class GzipBufferedOutput extends AsyncFilteringBufferedOutput<Deflater> {
  /// Create a new synchronous GzipBufferedOutput with default parameters.
  /// 
  /// This is the same as `sync(parent, Deflater.DEFAULT_COMPRESSION)`.
  public static GzipBufferedOutput sync(BufferedOutput parent) throws IOException {
    return sync(parent, Deflater.DEFAULT_COMPRESSION);
  }
  
  /// Create a new synchronous GzipBufferedOutput.
  /// 
  /// @param compression The compression level, one of the constants in {@link Deflater}. 
  public static GzipBufferedOutput sync(BufferedOutput parent, int compression) throws IOException {
    return new SequentialGzipBufferedOutput(parent, compression);
  }
  
  /// Create a new asynchronous GzipBufferedOutput.
  /// 
  /// See [AsyncFilteringBufferedOutput#AsyncFilteringBufferedOutput(perfio.BufferedOutput, boolean, int, boolean, int, int, boolean, java.util.concurrent.Executor)]
  /// for an explanation of most parameters.
  /// 
  /// @param compression The compression level, one of the constants in {@link Deflater}. 
  public static GzipBufferedOutput async(BufferedOutput parent, int depth, int minPartitionSize,
      int maxPartitionSize, boolean batchSubmit, Executor pool, int compression) throws IOException {
    return new SequentialGzipBufferedOutput(parent, depth, minPartitionSize, maxPartitionSize, batchSubmit, pool, compression);
  }
  
  /// Create a new asynchronous GzipBufferedOutput with default parameters.
  /// 
  /// This is the same as `async(parent, 3, 65536, 0, true, null, Deflater.DEFAULT_COMPRESSION)`.
  public static GzipBufferedOutput async(BufferedOutput parent) throws IOException {
    return async(parent, 3, 65536, 0, true, null, Deflater.DEFAULT_COMPRESSION);
  }

  /// Create a new parallel GzipBufferedOutput.
  /// 
  /// See [AsyncFilteringBufferedOutput#AsyncFilteringBufferedOutput(perfio.BufferedOutput, boolean, int, boolean, int, int, boolean, java.util.concurrent.Executor)]
  /// for an explanation of most parameters.
  /// 
  /// @param compression The compression level, one of the constants in {@link Deflater}. 
  public static GzipBufferedOutput parallel(BufferedOutput parent, int depth, int minPartitionSize,
      int maxPartitionSize, boolean batchSubmit, Executor pool, int compression) throws IOException {
    return new ParallelGzipBufferedOutput(parent, depth, minPartitionSize, maxPartitionSize, batchSubmit, pool, compression);
  }

  /// Create a new parallel GzipBufferedOutput with default parameters.
  /// This is the same as `parallel(parent, -1, 65536, 65536, true, null, Deflater.DEFAULT_COMPRESSION)`.
  public static GzipBufferedOutput parallel(BufferedOutput parent) throws IOException {
    return parallel(parent, -1, 65536, 65536, true, null, Deflater.DEFAULT_COMPRESSION);
  }
  

  protected final CRC32 crc = new CRC32();
  private final int compression;

  protected GzipBufferedOutput(BufferedOutput parent, boolean sequential, int depth,
      int minPartitionSize, int maxPartitionSize, boolean batchSubmit, Executor pool,
      int compression) throws IOException {
    super(parent, sequential, depth, true, minPartitionSize, maxPartitionSize, batchSubmit, pool);
    this.compression = compression;
    writeHeader(parent);
  }

  protected GzipBufferedOutput(BufferedOutput parent, int compression) throws IOException {
    super(parent, true);
    this.compression = compression;
    writeHeader(parent);
  }
  
  protected Deflater mkDeflater() { return new Deflater(compression, true); }
  
  private void writeHeader(BufferedOutput b) throws IOException {
    b.int64l(0x88b1f).int16l((short)0xff00);
  }

  protected void writeTrailer(BufferedOutput b, long crc, int len) throws IOException {
    b.int32l((int)crc).int32l(len);
  }
}


final class SequentialGzipBufferedOutput extends GzipBufferedOutput {
  private final Deflater defl;
  private boolean ended;

  SequentialGzipBufferedOutput(BufferedOutput parent, int compression) throws IOException {
    super(parent, compression);
    this.defl = mkDeflater();
  }

  SequentialGzipBufferedOutput(BufferedOutput parent, int depth, int minPartitionSize,
      int maxPartitionSize, boolean batchSubmit, Executor pool, int compression) throws IOException {
    super(parent, true, depth, minPartitionSize, maxPartitionSize, batchSubmit, pool, compression);
    this.defl = mkDeflater();
  }

  @Override protected void finish() throws IOException {
    if(!ended && !defl.finished()) {
      try {
        defl.finish();
        while(!defl.finished()) deflate();
        writeTrailer(parent, crc.getValue(), defl.getTotalIn());
      } finally {
        ended = true;
        defl.end();
      }
    }
  }

  @Override protected void flushPending() throws IOException {
    if(!flushPartial) super.flushPending();
  }

  private void deflate() throws IOException {
    parent.ensureAvailable(1);
    var l = defl.deflate(parent.buf, parent.pos, parent.lim - parent.pos);
    //System.out.println("compressed to "+l);
    parent.pos += l;
  }

  @Override protected void filterBlock(BufferedOutput b) throws IOException {
    if(!flushPartial) super.filterBlock(b);
    else {
      defl.setInput(b.buf, b.start, b.pos - b.start);
      while(!defl.needsInput()) deflate();
      crc.update(b.buf, b.start, b.pos - b.start);
      if(b.state != STATE_OPEN) releaseBlock(b);
    }
  }

  protected void filterAsync(FilterTask<Deflater> t) {
    try {
      if(t.isNew()) {
        crc.update(t.buf, t.start, t.end - t.start);
        defl.setInput(t.buf, t.start, t.end - t.start);
      }
      var o = t.to;
      o.pos += defl.deflate(o.buf, o.start, o.buf.length-o.start);
      if(defl.needsInput()) t.consume();
    } catch(Error | RuntimeException ex) {
      ended = true;
      try { defl.end(); } finally { throw ex; }
    }
  }
}


final class ParallelGzipBufferedOutput extends GzipBufferedOutput {

  ParallelGzipBufferedOutput(BufferedOutput parent, int depth, int minPartitionSize,
      int maxPartitionSize, boolean batchSubmit, Executor pool, int compression) throws IOException {
    super(parent, false, depth, minPartitionSize, maxPartitionSize, batchSubmit, pool, compression);
  }

  @Override protected void finish() throws IOException {
    parent.int16l((short)3);
    writeTrailer(parent, crc.getValue(), (int)totalBytesWritten());
  }

  @Override protected void filterBlock(BufferedOutput b) throws IOException {
    // Ideally we'd compute CRCs of each block in parallel and then merge them, but it's very
    // fast compared to the deflate compression and Java doesn't expose the existing
    // `crc32_combine()` from zlib for our use.
    crc.update(b.buf, b.start, b.pos - b.start);
    super.filterBlock(b);
  }

  protected void filterAsync(FilterTask<Deflater> t) {
    var defl = t.data != null ? t.data : (t.data = mkDeflater());
    try {
      var o = t.to;
      if(!t.isEmpty()) {
        if(t.isNew()) defl.setInput(t.buf, t.start, t.length());
        o.pos += defl.deflate(o.buf, o.pos, o.buf.length-o.pos);
      }
      if(defl.needsInput()) {
        if(t.isLast()) {
          int l;
          while((l = defl.deflate(o.buf, o.pos, o.buf.length-o.pos, Deflater.SYNC_FLUSH)) > 0) o.pos += l;
          if(o.pos < o.buf.length) {
            defl.end();
            t.consume();
          }
        } else t.consume();
      }
    } catch(Error | RuntimeException ex) {
      try { defl.end(); } finally { throw ex; }
    }
  }
}
