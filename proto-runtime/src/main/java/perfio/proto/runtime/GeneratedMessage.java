package perfio.proto.runtime;

import perfio.BufferedOutput;

import java.io.IOException;

public abstract class GeneratedMessage {
  public abstract void writeTo(BufferedOutput out) throws IOException;
}
