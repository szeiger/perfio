package perfio;

import java.io.IOException;
import java.io.OutputStream;

final class OutputStreamAdapter extends OutputStream {
  private final BufferedOutput bout;

  OutputStreamAdapter(BufferedOutput bout) { this.bout = bout; }

  public void write(int b) throws IOException { bout.uint8(b); }

  @Override
  public void write(byte[] b, int off, int len) throws IOException { bout.write(b, off, len); }

  @Override
  public void flush() throws IOException { bout.flush(); }

  @Override
  public void close() throws IOException { bout.close(true); }
}
