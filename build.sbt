import sbt.Keys._

// Common dependency versions
val akkaVersion = "2.3.10"
val akkaHttpVersion = "1.0-RC2"
val springVersion = "4.1.6.RELEASE"
val springBootVersion = "1.2.3.RELEASE"

lazy val root = (project in file(".")).
  settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*).
  settings(
    organization        := "com.github.scalaspring",
    name                := "akka-http-spring-boot",
    description         := "Integrates Akka HTTP and Spring Boot for rapid, robust service development using minimal configuration",
    scalaVersion        := "2.11.6",
    crossScalaVersions  := Seq("2.10.5"),
    javacOptions        := Seq("-source", "1.7", "-target", "1.7"),
    resolvers           += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.+",
      //"com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "org.springframework" % "spring-context" % springVersion,
      "org.springframework.boot" % "spring-boot-starter" % springBootVersion,
      "com.typesafe.akka" %% "akka-http-scala-experimental" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion,
      "com.github.scalaspring" %% "akka-spring-boot" % "0.1.2-SNAPSHOT"
      // The following dependencies support configuration validation
      //"javax.validation" % "validation-api" % "1.1.0.Final",
      //"javax.el" % "javax.el-api" % "3.0.1-b04",
      //"org.hibernate" % "hibernate-validator" % "5.1.3.Final",
    ),
    // Runtime dependencies
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.2"
    ).map { _ % "runtime" },
    // Test dependencies
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.4",
      "com.github.scalaspring" %% "scalatest-spring" % "0.2.1-SNAPSHOT",
      "org.springframework" % "spring-test" % springVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % akkaHttpVersion,
      "com.jsuereth" %% "scala-arm" % "1.4"
    ).map { _ % "test" },
    // Publishing settings
    publishMavenStyle       := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra :=
      <url>http://github.com/scalaspring/akka-http-spring-boot</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:scalaspring/akka-http-spring-boot.git</url>
        <connection>scm:git:git@github.com:scalaspring/akka-http-spring-boot.git</connection>
      </scm>
      <developers>
        <developer>
          <id>lancearlaus</id>
          <name>Lance Arlaus</name>
          <url>http://lancearlaus.github.com</url>
        </developer>
      </developers>
  )
