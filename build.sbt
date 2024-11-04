val PROTOBUF_PATH = "/home/szeiger/protobuf"

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

Test / fork := true
run / fork := true
run / connectInput := true

Global / scalaVersion := "2.13.14"

val hedgehogVersion = "0.10.1"

lazy val main = (project in file("."))
  .aggregate(LocalProject("bench"), LocalProject("test"))
  .settings(
    crossPaths := false,
    autoScalaLibrary := false,
    name := "perfio",
  )

lazy val bench = (project in file("bench"))
  .dependsOn(main)
  .enablePlugins(JmhPlugin)
  .settings(
    scalacOptions ++= Seq("-feature", "-opt:l:inline", "-opt-inline-from:perfio.*"),
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "33.3.0-jre"
    ),
    name := "perfio-bench",
    publish / skip := true,
    //Jmh / javaOptions ++= Seq("-Xss32M", "--add-modules", "jdk.incubator.vector"),
    //Jmh / JmhPlugin.generateJmhSourcesAndResources / fork := true,
  )

lazy val test = (project in file("test"))
  .dependsOn(main)
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

lazy val proto = (project in file("proto"))
  .dependsOn(main)
  .settings(
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "4.29.0-RC2",
    libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.2" % "test",
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    name := "perfio-proto",
  )

lazy val protoc = taskKey[Int]("Run protoc with the perfio-proto plugin")
lazy val bootstrap = taskKey[Int]("Boostrap the perfIO-generated protobuf API")

def runProtoc(cp: Classpath, srcs: Iterable[String], target: String, mode: String, perfioPackage: Option[String] = None): Int = {
  val gen = new File("proto/protoc-gen-perfio").getAbsolutePath
  val protoc = s"$PROTOBUF_PATH/bin/protoc"
  val pb = new ProcessBuilder(Seq(protoc, s"--plugin=protoc-gen-perfio=$gen", s"--${mode}_out=$target") ++ srcs: _*).inheritIO()
  pb.environment().put("PERFIO_CLASSPATH", cp.iterator.map(_.data).mkString(sys.props.getOrElse("path.separator", ":")))
  perfioPackage.foreach { p => pb.environment().put("PERFIO_PACKAGE", p) }
  pb.start().waitFor()
}

protoc := {
  val cp = (proto / Compile / fullClasspath).value
  val srcs = Seq(s"proto/src/test/proto/simple.proto")
  runProtoc(cp, srcs, "proto/src/test/java", "java")
  runProtoc(cp, srcs, "proto/src/test/java", "perfio", Some("com.example.perfio"))
}

bootstrap := {
  val cp = (proto / Compile / fullClasspath).value
  val srcs = Seq(
    s"$PROTOBUF_PATH/include/google/protobuf/compiler/plugin.proto",
    s"$PROTOBUF_PATH/include/google/protobuf/descriptor.proto",
  )
  runProtoc(cp, srcs, "proto/src/main/java", "perfio", Some("perfio.protoapi"))
}
