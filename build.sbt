//cancelable in Global := false

val jvmopts = Seq(
  "--add-modules", "jdk.incubator.vector",
  "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
)

javaOptions in Global ++= jvmopts
javacOptions in Global ++= jvmopts

// javaOptions in Global += "-Djmh.blackhole.autoDetect=false"

scalacOptions ++= Seq("-feature")

Test / fork := true
run / fork := true
run / connectInput := true

Global / scalaVersion := "2.13.14"

val hedgehogVersion = "0.10.1"

lazy val main = (project in file("."))
  .settings(
    libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.2" % "test",
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    scalacOptions ++= Seq("-feature", "-opt:l:inline", "-opt-inline-from:perfio.*"),
    libraryDependencies ++= Seq(
      "qa.hedgehog" %% "hedgehog-core" % hedgehogVersion % "test",
      "qa.hedgehog" %% "hedgehog-runner" % hedgehogVersion % "test",
      "qa.hedgehog" %% "hedgehog-sbt" % hedgehogVersion % "test",
    ),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    testFrameworks += TestFramework("hedgehog.sbt.Framework")
  )

lazy val bench = (project in file("bench"))
  .dependsOn(main)
  .enablePlugins(JmhPlugin)
  .settings(
    scalacOptions ++= Seq("-feature", "-opt:l:inline", "-opt-inline-from:perfio.*"),
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "33.3.0-jre"
    ),
    //Jmh / javaOptions ++= Seq("-Xss32M", "--add-modules", "jdk.incubator.vector"),
    //Jmh / JmhPlugin.generateJmhSourcesAndResources / fork := true,
  )
