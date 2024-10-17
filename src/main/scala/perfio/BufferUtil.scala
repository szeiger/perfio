package perfio

import java.lang.invoke.MethodHandles
import java.nio.ByteOrder
import scala.reflect.ClassTag

private object BufferUtil {
  /** Compute a new buffer size for the given size alignment (and assuming the current size respects
   * this alignment, clamped at the maximum aligned value <= Int.MaxValue */
  def growBuffer(current: Int, target: Int, align: Int): Int = {
    var l = current
    while(l < target) {
      l *= 2
      if(l < 0) return Int.MaxValue - align + 1
    }
    l
  }

  val BA_LONG_BIG = bavh[Long](true)
  val BA_INT_BIG = bavh[Int](true)
  val BA_SHORT_BIG = bavh[Short](true)
  val BA_CHAR_BIG = bavh[Char](true)
  val BA_DOUBLE_BIG = bavh[Double](true)
  val BA_FLOAT_BIG = bavh[Float](true)

  val BA_LONG_LITTLE = bavh[Long](false)
  val BA_INT_LITTLE = bavh[Int](false)
  val BA_SHORT_LITTLE = bavh[Short](false)
  val BA_CHAR_LITTLE = bavh[Char](false)
  val BA_DOUBLE_LITTLE = bavh[Double](false)
  val BA_FLOAT_LITTLE = bavh[Float](false)

  @inline private[this] def bavh[T](@inline be: Boolean)(implicit @inline ct: ClassTag[Array[T]]) =
    MethodHandles.byteArrayViewVarHandle(ct.runtimeClass, if(be) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

  val BB_LONG_BIG = bbvh[Long](true)
  val BB_INT_BIG = bbvh[Int](true)
  val BB_SHORT_BIG = bbvh[Short](true)
  val BB_CHAR_BIG = bbvh[Char](true)
  val BB_DOUBLE_BIG = bbvh[Double](true)
  val BB_FLOAT_BIG = bbvh[Float](true)

  val BB_LONG_LITTLE = bbvh[Long](false)
  val BB_INT_LITTLE = bbvh[Int](false)
  val BB_SHORT_LITTLE = bbvh[Short](false)
  val BB_CHAR_LITTLE = bbvh[Char](false)
  val BB_DOUBLE_LITTLE = bbvh[Double](false)
  val BB_FLOAT_LITTLE = bbvh[Float](false)

  @inline private[this] def bbvh[T](@inline be: Boolean)(implicit @inline ct: ClassTag[Array[T]]) =
    MethodHandles.byteBufferViewVarHandle(ct.runtimeClass, if(be) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
}
