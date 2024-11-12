package perfio.proto

import perfio.protoapi.PluginProtos
import perfio.scalaapi.TextOutputContext
import perfio.{BufferedInput, BufferedOutput}

import java.nio.charset.StandardCharsets

@main
def Main: Unit =
  val req = PluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.of(System.in))
  val root = new RootNode(req)
  //root.dump(System.err, "")
  //System.err.println()

  val res = new PluginProtos.CodeGeneratorResponse
  res.setSupportedFeatures(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL.number)

  for genFile <- root.genFiles do
    val fm = root.fileMap(genFile)
    val bo = BufferedOutput.growing()
    val to = bo.text()
    fm.emit(using TextOutputContext(to))
    bo.close()
    res.addFile(
      new PluginProtos.CodeGeneratorResponse.File()
        .setName(fm.outputFileName)
        .setContent(new String(bo.buffer(), 0, bo.length(), StandardCharsets.UTF_8)))

  val bo = BufferedOutput.of(System.out)
  res.writeTo(bo)
  bo.flush()
