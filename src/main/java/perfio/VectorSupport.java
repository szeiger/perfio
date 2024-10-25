package perfio;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;

class VectorSupport {
  private VectorSupport() {}

  /// Do not check! This class cannot even be initialized if the Vector API is not available.
  /// Use BufferUtil.VECTOR_ENABLED instead.
  static final boolean __ENABLED;

  static final VectorSpecies<Byte> SPECIES;
  static final int VLEN;
  static final long FULL_MASK;

  static {
    var s = ByteVector.SPECIES_PREFERRED;

    // We need to store the mask as a Long field instead of a VectorMask[Byte]
    // to get proper optimization from HotSpot so we're limited to 64 lanes.
    if(s.length() > 64) s = ByteVector.SPECIES_512;

    // Just a sanity check. We accept any reasonable size. Even 64-bit vectors (SWAR) are faster than scalar.
    // Hopefully this will guarantee that the preferred species is actually vectorized (which is not the case
    // with the experimental preview API at the moment).
    __ENABLED = s.length() >= 8 && !"true".equals(System.getProperty("perfio.disableVectorized"));

    SPECIES = s;
    VLEN = SPECIES.length();
    FULL_MASK = -1L << (64-VLEN) >>> (64-VLEN);
  }
}
