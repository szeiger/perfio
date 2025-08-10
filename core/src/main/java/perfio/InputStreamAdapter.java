package perfio;

import java.io.IOException;
import java.io.InputStream;

final class InputStreamAdapter extends InputStream {
  private final BufferedInput bin;

  InputStreamAdapter(BufferedInput bin) { this.bin = bin; }

  public int read() throws IOException { return bin.read(); }

  @Override
  public int read(byte[] b, int off, int len) throws IOException { return bin.readSome(b, off, len); }

  @Override
  public int readNBytes(byte[] b, int off, int len) throws IOException { return bin.read(b, off, len); }

  @Override
  public long skip(long n) throws IOException { return bin.skipSome(n); }

  @Override
  public void skipNBytes(long n) throws IOException { bin.skip(n); }

  @Override
  public int available() throws IOException { return bin.available(); }

  @Override
  public void close() throws IOException { bin.close(true); }
}
