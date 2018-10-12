name := "workflow-lib"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.17"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "org.testng" % "testng" % "6.14.2" % Test
