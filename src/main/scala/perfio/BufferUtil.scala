package perfio

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
}
