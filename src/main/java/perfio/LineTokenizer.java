package perfio;

import java.io.Closeable;
import java.io.IOException;

/// Read text from a [BufferedInput] and split it into lines. If the input ends with a newline, no
/// additional empty line is returned (same as [java.io.BufferedReader]).
///
/// Parsing is performed on raw byte data so it only works for charsets that are compatible with
/// ASCII line splitting. These include UTF-8, ASCII, the ISO-8859 family and other 8-bit
/// ASCII-based charsets. This avoids the inefficiency of more general parsers (like
/// [java.io.BufferedInputStream]) of first decoding the entire input to 16-bit chars and then
/// attempting to compress it back to bytes when creating the Strings.
///
/// With the default settings for `eol` and `preEol` a LineTokenizer will recognize both LF (Unix) and CRLF (Windows)
/// line endings. Automatic recognition of pure CR line endings (classic MacOS) at the same time is not supported but
/// can be configured manually with `eol = '\r', preEol = -1`.
public abstract sealed class LineTokenizer implements Closeable permits HeapLineTokenizer, DirectLineTokenizer {
  final byte eolChar, preEolChar;

  LineTokenizer(byte eolChar, byte preEolChar) {
    this.eolChar = eolChar;
    this.preEolChar = preEolChar;
  }

  boolean closed = false;

  /// Returns the next line of text, or null if the end of the input has been reached.
  public abstract String readLine() throws IOException;

  /// Close this LineTokenizer without closing the underlying BufferedInput. The BufferedInput continues reading
  /// from the current position of this LineTokenizer (i.e. directly after the last end-of-line character that was
  /// read). A subsequent call to [#close()] has no effect.
  ///
  /// @return The underlying BufferedInput.
  public abstract BufferedInput end() throws IOException;

  void checkState() throws IOException {
    if(closed) throw new IOException("LineTokenizer has already been closed");
  }

  /// Prevent reuse of this view. This ensures that it stays closed when a new view or LineTokenizer is created from
  /// the parent BufferedInput.
  public abstract LineTokenizer detach() throws IOException;

  void markClosed() {
    closed = true;
  }
}
