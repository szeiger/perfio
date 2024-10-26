package perfio;

class LineBuffer {
  private byte[] buf = new byte[256];

  private byte[] extendBuffer(int len) {
    var buflen = buf.length;
    while(buflen < len) buflen *= 2;
    return new byte[buflen];
  }

  byte[] get(int len) {
    if(buf.length < len) buf = extendBuffer(len);
    return buf;
  }
}
