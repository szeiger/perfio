package perfio;

import java.io.IOException;
import java.io.InputStream;

/// A [BufferedOutput] which accumulates all data written to it. After closing, the data can be
/// retrieved repeatedly with [#toBufferedInput()]  or [#toInputStream()].
public abstract class AccumulatingBufferedOutput extends CacheRootBufferedOutput {

  AccumulatingBufferedOutput(byte[] buf, boolean bigEndian, int start, int pos, int lim, int initialBufferSize, boolean fixed, long totalLimit) {
    super(buf, bigEndian, start, pos, lim, initialBufferSize, fixed, totalLimit);
  }

  /// Create a new [BufferedInput] that can read the data. This method does not consume the data
  /// and can be used repeatedly or together with other buffer access methods. 
  public abstract BufferedInput toBufferedInput() throws IOException;

  /// Create a new [InputStream] that can read the data. This method does not consume the data
  /// and can be used repeatedly or together with other buffer access methods. 
  public abstract InputStream toInputStream() throws IOException;
}
