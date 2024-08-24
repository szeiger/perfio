package perfio

import jdk.incubator.vector.ByteVector

private object VectorSupport {
  final val species = {
    val s = ByteVector.SPECIES_PREFERRED
    // We need to store the mask as a Long field instead of a VectorMask[Byte]
    // to get proper optimization from HotSpot so we're limited to 64 lanes.
    if(s.length() > 64) ByteVector.SPECIES_512 else s
  }
  final val lfs = ByteVector.broadcast(species, '\n'.toByte)
  final val vlen = species.length()
  final val fullMask = -1L << (64-vlen) >>> (64-vlen)
}
