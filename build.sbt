import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

val PROTOBUF_HOME = sys.env.getOrElse("PROTOBUF_HOME", s"${sys.props("user.home")}/protobuf")

Global / organization := "com.novocode"

Global / version := "0.1-SNAPSHOT"

//cancelable in Global := false

val runtimeOpts = Seq(
  "--add-modules", "jdk.incubator.vector",
  "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
  "--add-opens", "java.base/java.lang=ALL-UNNAMED",
)
val compileOpts = Seq(
  "--add-modules", "jdk.incubator.vector",
)

javaOptions in Global ++= runtimeOpts
javacOptions in Global ++= compileOpts

// javaOptions in Global += "-Djmh.blackhole.autoDetect=false"

scalacOptions ++= Seq("-feature")

ThisBuild / Test / fork := true
ThisBuild / run / fork := true
ThisBuild / run / connectInput := true

Global / scalaVersion := "3.5.2"

val hedgehogVersion = "0.10.1"

lazy val main = (project in file("."))
  .aggregate(LocalProject("bench"), LocalProject("test"), LocalProject("scalaApi"))
  .settings(
    crossPaths := false,
    autoScalaLibrary := false,
    name := "perfio",
  )

lazy val scalaApi = (project in file("scalaapi"))
  .dependsOn(main)
  .settings(
    name := "perfio-scala",
  )

lazy val bench = (project in file("bench"))
  .dependsOn(scalaApi)
  .enablePlugins(JmhPlugin)
  .settings(
    scalacOptions ++= Seq("-feature", "-opt:l:inline", "-opt-inline-from:perfio.*"),
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "33.3.0-jre"
    ),
    name := "perfio-bench",
    publish / skip := true,
  )

lazy val test_ = Project("test", file("test"))
  .dependsOn(scalaApi)
  .settings(
    libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.2" % "test",
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    libraryDependencies ++= Seq(
      "qa.hedgehog" %% "hedgehog-core" % hedgehogVersion % "test",
      "qa.hedgehog" %% "hedgehog-runner" % hedgehogVersion % "test",
      "qa.hedgehog" %% "hedgehog-sbt" % hedgehogVersion % "test",
    ),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    testFrameworks += TestFramework("hedgehog.sbt.Framework"),
    name := "perfio-test",
    publish / skip := true,
  )

lazy val protoRuntime = (project in file("proto-runtime"))
  .dependsOn(main)
  .settings(
    name := "perfio-proto-runtime",
  )

lazy val proto = (project in file("proto"))
  .dependsOn(protoRuntime, scalaApi)
  .settings(
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "4.29.0-RC2" % "test",
    libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.2" % "test",
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    name := "perfio-proto",
    Test / sourceGenerators += Def.task {
      val srcDir = new File((Test / sourceDirectory).value, "proto")
      val srcs = srcDir.listFiles(_.getName.endsWith(".proto")).toSet
      val out = (Test / sourceManaged).value
      val cp = (Compile / fullClasspathAsJars).value
      val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "proto") { (in: Set[File]) =>
        val pin = in.intersect(srcs)
        val outs = runProtoc(cp, pin.map(_.getPath), out.toPath, Seq("java", "perfio"), Some("com.example.perfio"), Some(srcDir.getPath), true).toSet
        println("Generated "+outs.mkString(", "))
        outs
      }
      val outs = cachedCompile(srcs ++ cp.iterator.map(_.data).toSet).toSeq.sorted
      outs
    }.taskValue,
  )

lazy val protoBench = (project in file("proto-bench"))
  .dependsOn(proto)
  .enablePlugins(JmhPlugin)
  .settings(
    scalacOptions ++= Seq("-feature", "-opt:l:inline"),
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "4.29.0-RC2",
    name := "perfio-proto-bench",
    publish / skip := true,
  )

lazy val bootstrapProto = taskKey[Unit]("Boostrap the perfIO-generated protobuf API")

bootstrapProto := {
  val cp = (proto / Compile / fullClasspath).value
  val srcs = Seq(
    s"$PROTOBUF_HOME/include/google/protobuf/compiler/plugin.proto",
    s"$PROTOBUF_HOME/include/google/protobuf/descriptor.proto",
  )
  val target = Path.of("proto/src/main/java")
  runProtoc(cp, srcs, target, Seq("perfio"), Some("perfio.protoapi"), None, false)
}

def runProtoc(cp: Classpath, srcs: Iterable[String], target: Path, modes: Seq[String], perfioPackage: Option[String], protoPath: Option[String], isTest: Boolean): Seq[File] = {
  val gen = new File("proto/protoc-gen-perfio").getAbsolutePath
  val protoc = s"$PROTOBUF_HOME/bin/protoc"
  if(target.toFile.exists()) IO.delete(target.toFile.listFiles())
  else Files.createDirectories(target)
  val temp = Files.createTempDirectory("protoc-out")
  try {
    modes.foreach { mode =>
      val pb = new ProcessBuilder(Seq(protoc, s"--plugin=protoc-gen-perfio=$gen", s"--${mode}_out=$temp") ++ protoPath.map(s => s"--proto_path=$s") ++ srcs: _*).inheritIO()
      pb.environment().put("PERFIO_CLASSPATH", cp.iterator.map(_.data).mkString(sys.props.getOrElse("path.separator", ":")))
      perfioPackage.foreach { p => pb.environment().put("PERFIO_PACKAGE", p) }
      if(isTest) pb.environment().put("PERFIO_TEST", "true")
      val ret = pb.start().waitFor()
      if(ret != 0) throw new RuntimeException(s"protoc failed with exit code $ret")
    }
    val tempFiles = Files.walk(temp).iterator().asScala.toVector
    val tempOuts = tempFiles.filter(p => p.toFile.getName.endsWith(".java"))
    val outs = tempOuts.map { t =>
      val o = target.resolve(temp.relativize(t))
      Files.createDirectories(o.getParent)
      Files.move(t, o)
      o.toAbsolutePath.toFile
    }
    outs
  } finally IO.delete(temp.toFile)
}

val javap = inputKey[Unit]("Run javap")

test_ / Test / javap := {
  import complete.DefaultParsers._
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  val cp = (test_ / Test / fullClasspath).value

  val pb = new ProcessBuilder(Seq("javap", "-cp", cp.iterator.map(_.data).mkString(sys.props.getOrElse("path.separator", ":"))) ++ args: _*).inheritIO()
  pb.start().waitFor()
}
