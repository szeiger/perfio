package perfio.proto

import perfio.protoapi.PluginProtos
import perfio.{BufferedInput, BufferedOutput}

import scala.jdk.CollectionConverters._

import java.nio.charset.StandardCharsets

object Main extends App {
  val req = PluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.of(System.in))
  //System.err.println(req)

  val root = new RootNode(req)
  root.dump(System.err, "")
  System.err.println()

  val res = new PluginProtos.CodeGeneratorResponse
  res.setSupportedFeatures(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL.number)

  root.genFiles.foreach { genFile =>
    val fm = root.fileMap(genFile)
    System.err.println("**** "+fm.javaOutputFileName)
    val bo = BufferedOutput.growing()
    val to = bo.text()
    fm.emit(to, "")
    bo.close()
    res.addFile(
      new PluginProtos.CodeGeneratorResponse.File()
        .setName(fm.javaOutputFileName)
        .setContent(new String(bo.buffer(), 0, bo.length(), StandardCharsets.UTF_8)))
    //System.err.write(bo.buffer(), 0, bo.length())
  }

  for(f <- res.getFileList.asScala) {
    System.err.println("**** generated "+f.getName)
  }

  //val bo = BufferedOutput.of(System.out)
  //res.writeTo(bo)
  //bo.flush()

  val bo = BufferedOutput.growing()
  res.writeTo(bo)
  bo.close()
  System.out.write(bo.buffer(), 0, bo.length())
  System.out.flush()

  System.exit(0)
}
