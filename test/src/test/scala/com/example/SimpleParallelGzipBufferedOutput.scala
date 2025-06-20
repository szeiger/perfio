package com.example

import perfio.{AsyncFilteringBufferedOutput, BufferedOutput, FilterTask, UncheckedOutput as U}
import java.util.zip.{CRC32, Deflater}

class SimpleParallelGzipBufferedOutput(parent: BufferedOutput)
    extends AsyncFilteringBufferedOutput[Deflater](parent, false, -1, true, 65536, 65536, true, null):
  private val crc = new CRC32
  parent.int64l(0x88b1f).int16l(0xff00.toShort)

  override protected def finish(): Unit =
    parent.int16l(3.toShort).int32l(crc.getValue.toInt).int32l(totalBytesWritten.toInt)

  override protected def filterBlock(b: BufferedOutput): Unit =
    crc.update(U.buf(b), U.start(b), U.used(b))
    super.filterBlock(b)

  override protected def filterAsync(t: FilterTask[Deflater]): Unit =
    if(t.data == null) t.data = new Deflater(Deflater.DEFAULT_COMPRESSION, true)
    val d = t.data
    val o = t.to
    if(!t.isEmpty)
      if(t.isNew) d.setInput(t.buf, t.start, t.length)
      val p = U.position(o)
      U.position(o, p + d.deflate(U.buf(o), p, U.available(o)))
    if(d.needsInput)
      if(t.isLast)
        var l = 0
        while({ l = d.deflate(U.buf(o), U.position(o), U.available(o), Deflater.SYNC_FLUSH); l > 0 })
          U.position(o, U.position(o) + l)
        if(U.position(o) < U.buf(o).length)
          d.end()
          t.consume()
      else t.consume()
