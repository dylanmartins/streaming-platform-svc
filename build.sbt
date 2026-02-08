ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "streaming-platform",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.http4s" %% "http4s-ember-server" % "0.23.27",
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.10.9",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.10.9",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "ch.qos.logback" % "logback-classic" % "1.5.6",

      // ✅ make Host/Port literals available
      "com.comcast" %% "ip4s-core" % "3.6.0",

      // Tapir OpenAPI docs
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % "1.10.9",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.10.9"
    )
  )
