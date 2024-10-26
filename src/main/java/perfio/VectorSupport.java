package perfio;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;

class VectorSupport {
  private VectorSupport() {}

  static final VectorSpecies<Byte> SPECIES;
  static final int VLEN;
  static final long FULL_MASK;

  static {
    var s = ByteVector.SPECIES_PREFERRED;

    // We need to store the mask as a Long field instead of a VectorMask[Byte]
    // to get proper optimization from HotSpot so we're limited to 64 lanes.
    if(s.length() > 64) s = ByteVector.SPECIES_512;

    SPECIES = s;
    VLEN = SPECIES.length();
    FULL_MASK = -1L << (64-VLEN) >>> (64-VLEN);
  }
}
