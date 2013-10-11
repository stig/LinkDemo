name := "LinkDemo"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Spray Nightlies" at "http://nightlies.spray.io"

val akka = "2.2.1"
val spray = "1.2-20130710"

libraryDependencies ++=
    "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime" ::
    "com.typesafe.akka" %% "akka-actor" % akka ::
    "com.typesafe.akka" %% "akka-slf4j" % akka ::
    "io.spray" % "spray-caching" % spray ::
    "io.spray" % "spray-can" % spray ::
    "io.spray" % "spray-routing" % spray ::
    "io.spray" %% "spray-json" % "1.2.5" ::
    Nil


scalariformSettings

seq(Revolver.settings: _*)
