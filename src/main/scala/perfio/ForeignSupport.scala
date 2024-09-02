package perfio

import java.lang.foreign.{Arena, MemorySegment}
import java.nio.channels.FileChannel
import java.nio.file.{Path, StandardOpenOption}

private object ForeignSupport {
  def mapRO(file: Path): MemorySegment = {
    val a = Arena.ofAuto()
    val ch = FileChannel.open(file, StandardOpenOption.READ)
    try ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size(), a)
    finally ch.close()
  }
}
