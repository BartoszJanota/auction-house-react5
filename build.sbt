name := "auction_house"

version := "1.0"

logLevel := Level.Info

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
  "org.scalatest" %% "scalatest" % "1.9.2-SNAP2" % "test",
  "ch.qos.logback" % "logback-classic" % "1.0.7",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.6"
)

