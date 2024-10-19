package perfio

import jdk.incubator.vector.ByteVector

import scala.sys.BooleanProp

private object VectorSupport {
  final val species = {
    val s = ByteVector.SPECIES_PREFERRED
    // We need to store the mask as a Long field instead of a VectorMask[Byte]
    // to get proper optimization from HotSpot so we're limited to 64 lanes.
    if(s.length() > 64) ByteVector.SPECIES_512 else s
  }
  final val vlen = species.length()
  final val fullMask = -1L << (64-vlen) >>> (64-vlen)

  val isEnabled: Boolean = {
    // Just a sanity check. We accept any reasonable size. Even 64-bit vectors (SWAR) are faster than scalar.
    // Hopefully this will guarantee that the preferred species is actually vectorized (which is not the case
    // with the experimental preview API at the moment).
    try ByteVector.SPECIES_PREFERRED.length() >= 8 && !BooleanProp.valueIsTrue("perfio.disableVectorized")
    catch { case _: NoClassDefFoundError => false }
  }
}
