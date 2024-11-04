package perfio.proto

import com.google.protobuf.ByteString
import com.google.protobuf.compiler.PluginProtos
import perfio.BufferedOutput

object Main extends App {
  val req = PluginProtos.CodeGeneratorRequest.parseFrom(System.in)
  //System.err.println(req)

  val root = new RootNode(req)
  root.dump(System.err, "")
  System.err.println()

  val res = PluginProtos.CodeGeneratorResponse.newBuilder()
  res.setSupportedFeatures(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL_VALUE)

  root.genFiles.foreach { genFile =>
    val fm = root.fileMap(genFile)
    System.err.println("**** "+fm.javaOutputFileName)
    val bo = BufferedOutput.growing()
    val to = bo.text()
    fm.emit(to, "")
    bo.close()
    val fb = res.addFileBuilder()
    fb.setName(fm.javaOutputFileName)
    fb.setContentBytes(ByteString.copyFrom(bo.buffer(), 0, bo.length()))
    //System.err.write(bo.buffer(), 0, bo.length())
  }

  res.build().writeTo(System.out)

  System.exit(0)
}
