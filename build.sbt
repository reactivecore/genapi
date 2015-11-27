import SonatypeKeys._

sonatypeSettings

sbtPlugin := true

organization := "net.reactivecore"

name := """genapi"""

version := "0.1"

profileName := "net.reactivecore"

// To sync with Maven central, you need to supply the following information:
pomExtra := {
  <url>https://github.com/reactivecore/genapi</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://opensource.org/licenses/MIT</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:@github.com:reactivecore/genapi.git</connection>
      <url>git@github.com:reactivecore/genapi.git</url>
    </scm>
    <developers>
      <developer>
        <id>nob13</id>
        <name>Norbert Schultz</name>
        <url>https://www.reactivecore.de</url>
      </developer>
    </developers>
}

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += Resolver.sonatypeRepo("snapshots")

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"
