package perfio;

import perfio.internal.BufferUtil;
import perfio.internal.MemoryAccessor;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/// A ReadableBuffer contains a byte array or ByteBuffer, position & limit indices and an
/// endian flag. It provides all the methods for reading data from a single block. Operations
/// for views and lifecycle management are part of [BufferedInput].
public abstract class ReadableBuffer {
  protected byte[] buf;
  protected int pos; // first used byte in buffer
  protected int lim; // last used byte + 1 in buffer
  protected ByteBuffer bb;
  protected boolean bigEndian;

  /// Return the number of bytes that are currently available in the buffer and can be read without
  /// re-buffering or blocking.
  public final int available() { return lim - pos; }

  /// Return the byte order of this buffer.
  public final ByteOrder order() { return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN; }

  /// Request `count` bytes to be available to read in the buffer. Less may be available if the end of the input
  /// is reached. This method may change the buffer references.
  protected final void requestAvailable(int count) throws IOException {
    if(available() < count) prepareAndFillBuffer(count);
  }

  /// Request `count` bytes to be available to read in the buffer, advance the buffer to the position after these
  /// bytes and return the previous position. Throws EOFException if the end of the input is reached before the
  /// requested number of bytes is available. This method may change the buffer references.
  protected final int fwd(int count) throws IOException {
    if(available() < count) {
      prepareAndFillBuffer(count);
      if(available() < count) throw new EOFException();
    }
    var p = pos;
    pos += count;
    return p;
  }

  /// Find the next occurrence of `b` at or after the current position and return its distance.
  /// All data up to this position is guaranteed to be in the buffer afterward. Returns -1 if the
  /// end of the input is reached or the maximum buffer size (a bit under 2 GB depending on
  /// alignment) would be exceeded before a match is found.
  ///
  /// This method may change the buffer references.
  protected final int findNext(byte b) throws IOException {
    var i = findInRange(pos, lim, b);
    if(i >= 0) return i-pos;
    while(true) {
      var a = available();
      var m = Math.max(a, 32);
      var dist = BufferUtil.growBuffer(m, m+1, 1);
      if(dist <= a) return -1; // buffer size limit reached
      requestAvailable(dist);
      if(available() <= a) return -1; // EOF reached
      i = findInRange(pos+a, lim, b);
      if(i >= 0) return i-pos;
    }
  }

  private int findInRange(int from, int end, byte b) {
    //System.out.println("findInRange("+from+", "+end+")");
    if(buf != null) {
      for(int i=from; i<end; i++)
        if(MemoryAccessor.INSTANCE.int8(buf, i) == b) return i;
    } else {
      for(int i=from; i<end; i++)
        if(MemoryAccessor.INSTANCE.int8(bb, i) == b) return i;
    }
    return -1;
  }

  /// Fill the buffer as much as possible without blocking (starting at [#lim]), but at least
  /// until `count` bytes are available starting at [#pos] even if this requires blocking or
  /// growing the buffer. Less data may only be made available when the end of the input has been
  /// reached. The field [#lim] is updated accordingly.
  ///
  /// This method may change the buffer references.
  protected abstract void prepareAndFillBuffer(int count) throws IOException;

  /// Read a signed 8-bit integer (`byte`).
  public final byte int8() throws IOException {
    var p = fwd(1);
    return buf != null ? MemoryAccessor.INSTANCE.int8(buf, p) : MemoryAccessor.INSTANCE.int8(bb, p);
  }

  /// Read an unsigned 8-bit integer into the lower 8 bits of an `int`.
  public final int uint8() throws IOException { return int8() & 0xFF; }

  /// Read a signed 16-bit integer (`short`) in the current byte [#order()].
  public final short int16()  throws IOException {
    var p = fwd(2);
    return buf != null ? MemoryAccessor.INSTANCE.int16(buf, p, bigEndian) : MemoryAccessor.INSTANCE.int16(bb, p, bigEndian);
  }
  /// Read a signed 16-bit integer (`short`) in the native byte order.
  public final short int16n()  throws IOException {
    var p = fwd(2);
    return buf != null ? MemoryAccessor.INSTANCE.int16n(buf, p) : MemoryAccessor.INSTANCE.int16n(bb, p);
  }
  /// Read a signed 16-bit integer (`short`) in big endian byte order.
  public final short int16b()  throws IOException {
    var p = fwd(2);
    return buf != null ? MemoryAccessor.INSTANCE.int16b(buf, p) : MemoryAccessor.INSTANCE.int16b(bb, p);
  }
  /// Read a signed 16-bit integer (`short`) in little endian byte order.
  public final short int16l()  throws IOException {
    var p = fwd(2);
    return buf != null ? MemoryAccessor.INSTANCE.int16l(buf, p) : MemoryAccessor.INSTANCE.int16l(bb, p);
  }

  /// Read an unsigned 16-bit integer (`char`) in the current byte [#order()].
  public final char uint16()  throws IOException {
    var p = fwd(2);
    return buf != null ? MemoryAccessor.INSTANCE.uint16(buf, p, bigEndian) : MemoryAccessor.INSTANCE.uint16(bb, p, bigEndian);
  }
  /// Read an unsigned 16-bit integer (`char`) in the native byte order.
  public final char uint16n()  throws IOException {
    var p = fwd(2);
    return buf != null ? MemoryAccessor.INSTANCE.uint16n(buf, p) : MemoryAccessor.INSTANCE.uint16n(bb, p);
  }
  /// Read an unsigned 16-bit integer (`char`) in big endian byte order.
  public final char uint16b()  throws IOException {
    var p = fwd(2);
    return buf != null ? MemoryAccessor.INSTANCE.uint16b(buf, p) : MemoryAccessor.INSTANCE.uint16b(bb, p);
  }
  /// Read an unsigned 16-bit integer (`char`) in little endian byte order.
  public final char uint16l()  throws IOException {
    var p = fwd(2);
    return buf != null ? MemoryAccessor.INSTANCE.uint16l(buf, p) : MemoryAccessor.INSTANCE.uint16l(bb, p);
  }

  /// Read a signed 32-bit integer (`int`) in the current byte [#order()].
  public final int int32()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.int32(buf, p, bigEndian) : MemoryAccessor.INSTANCE.int32(bb, p, bigEndian);
  }
  /// Read a signed 32-bit integer (`int`) in the native byte order.
  public final int int32n()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.int32n(buf, p) : MemoryAccessor.INSTANCE.int32n(bb, p);
  }
  /// Read a signed 32-bit integer (`int`) in big endian byte order.
  public final int int32b()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.int32b(buf, p) : MemoryAccessor.INSTANCE.int32b(bb, p);
  }
  /// Read a signed 32-bit integer (`int`) in little endian byte order.
  public final int int32l()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.int32l(buf, p) : MemoryAccessor.INSTANCE.int32l(bb, p);
  }

  /// Read an unsigned 32-bit integer into the lower 32 bits of a `long` in the current byte [#order()].
  public final long uint32() throws IOException { return int32() & 0xFFFFFFFFL; }
  /// Read an unsigned 32-bit integer into the lower 32 bits of a `long` in the native byte order.
  public final long uint32n() throws IOException { return int32n() & 0xFFFFFFFFL; }
  /// Read an unsigned 32-bit integer into the lower 32 bits of a `long` in big endian byte order.
  public final long uint32b() throws IOException { return int32b() & 0xFFFFFFFFL; }
  /// Read an unsigned 32-bit integer into the lower 32 bits of a `long` in little endian byte order.
  public final long uint32l() throws IOException { return int32l() & 0xFFFFFFFFL; }

  /// Read a signed 64-bit integer (`long`) in the current byte [#order()].
  public final long int64()  throws IOException {
    var p = fwd(8);
    return buf != null ? MemoryAccessor.INSTANCE.int64(buf, p, bigEndian) : MemoryAccessor.INSTANCE.int64(bb, p, bigEndian);
  }
  /// Read a signed 64-bit integer (`long`) in the native byte order.
  public final long int64n()  throws IOException {
    var p = fwd(8);
    return buf != null ? MemoryAccessor.INSTANCE.int64n(buf, p) : MemoryAccessor.INSTANCE.int64n(bb, p);
  }
  /// Read a signed 64-bit integer (`long`) in big endian byte order.
  public final long int64b()  throws IOException {
    var p = fwd(8);
    return buf != null ? MemoryAccessor.INSTANCE.int64b(buf, p) : MemoryAccessor.INSTANCE.int64b(bb, p);
  }
  /// Read a signed 64-bit integer (`long`) in little endian byte order.
  public final long int64l()  throws IOException {
    var p = fwd(8);
    return buf != null ? MemoryAccessor.INSTANCE.int64l(buf, p) : MemoryAccessor.INSTANCE.int64l(bb, p);
  }

  /// Read a 32-bit IEEE-754 floating point value (`float`) in the current byte [#order()].
  public final float float32()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.float32(buf, p, bigEndian) : MemoryAccessor.INSTANCE.float32(bb, p, bigEndian);
  }
  /// Read a 32-bit IEEE-754 floating point value (`float`) in the native byte order.
  public final float float32n()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.float32n(buf, p) : MemoryAccessor.INSTANCE.float32n(bb, p);
  }
  /// Read a 32-bit IEEE-754 floating point value (`float`) in big endian byte order.
  public final float float32b()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.float32b(buf, p) : MemoryAccessor.INSTANCE.float32b(bb, p);
  }
  /// Read a 32-bit IEEE-754 floating point value (`float`) in little endian byte order.
  public final float float32l()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.float32l(buf, p) : MemoryAccessor.INSTANCE.float32l(bb, p);
  }

  /// Read a 64-bit IEEE-754 floating point value (`double`) in the current byte [#order()].
  public final double float64()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.float64(buf, p, bigEndian) : MemoryAccessor.INSTANCE.float64(bb, p, bigEndian);
  }
  /// Read a 64-bit IEEE-754 floating point value (`double`) in the native byte order.
  public final double float64n()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.float64n(buf, p) : MemoryAccessor.INSTANCE.float64n(bb, p);
  }
  /// Read a 64-bit IEEE-754 floating point value (`double`) in big endian byte order.
  public final double float64b()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.float64b(buf, p) : MemoryAccessor.INSTANCE.float64b(bb, p);
  }
  /// Read a 64-bit IEEE-754 floating point value (`double`) in little endian byte order.
  public final double float64l()  throws IOException {
    var p = fwd(4);
    return buf != null ? MemoryAccessor.INSTANCE.float64l(buf, p) : MemoryAccessor.INSTANCE.float64l(bb, p);
  }


  /// Read the next unsigned 8-bit integer if there is more data, or return -1 if the end of the
  /// input has been reached. See [java.io.InputStream#read()].
  public int read() throws IOException {
    requestAvailable(1);
    if(pos == lim) return -1;
    var p = pos;
    pos++;
    return (buf != null ? MemoryAccessor.INSTANCE.int8(buf, p) : MemoryAccessor.INSTANCE.int8(bb, p)) & 0xFF;
  }

  /// Read up to `len` bytes into `a` starting at `off`, blocking only if the buffer is currently
  /// empty. At least 1 byte is returned unless the end of the input has been reached.
  /// This is equivalent to [java.io.InputStream#read(byte\[\], int, int)].
  /// 
  /// @return the number of bytes that were read
  public int readSome(byte[] a, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, a.length);
    requestAvailable(1);
    if(pos == lim) return 0;
    var l = Math.min(len, available());
    if(buf != null) System.arraycopy(buf, pos, a, off, l);
    else bb.get(pos, a, off, l);
    return l;
  }

  /// Read up to `len` bytes into `a` starting at `off`, blocking until all requested data is
  /// available. Less than `len` bytes can only be returned if the end of the input is reached.
  /// This is equivalent to [java.io.InputStream#readNBytes(byte\[\], int, int)()].
  /// 
  /// @return the number of bytes that were read
  public int read(byte[] a, int off, int len) throws IOException {
    // This method is not final so it can be overridden for direct inputs that copy the entire
    // data in one go.
    Objects.checkFromIndexSize(off, len, a.length);
    int l0 = len;
    while(len > 0) {
      requestAvailable(1);
      if(available() == 0) break;
      var l = Math.min(len, available());
      if(l > 0) {
        if(buf != null) System.arraycopy(buf, pos, a, off, l);
        else bb.get(pos, a, off, l);
        pos += l;
        off += l;
        len -= l;
      }
    }
    return l0-len;
  }
}
