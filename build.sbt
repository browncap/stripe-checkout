scalaVersion in ThisBuild := "2.13.4"

name := "stripe-checkout"

version := "0.1"

val http4sVersion                   = "0.21.2"
val catsVersion                     = "2.1.1"
val catsEffectVersion               = "2.1.1"
val circeVersion                    = "0.12.2"
val specs2Version                   = "4.7.0"
val scalaTestVersion                = "3.0.8"
val pureConfigVersion               = "0.14.0"
val fs2Version                      = "2.1.0"
val doobieVersion                   = "0.9.0"
val fuuidVersion                    = "0.4.0"
val http4sJwtVersion                = "0.0.5"
val testContainersPostgresqlVersion = "1.10.7"
val testContainersVersion           = "0.38.8"
val flyWayVersion                   = "6.0.8"
val enumeratumScalacheckVersion     = "1.5.16"
val log4CatsVersion                 = "1.1.1"
val tsecVersion                     = "0.2.0"

libraryDependencies ++= Seq(
  "org.http4s"            %% "http4s-dsl"                      % http4sVersion,
  "org.http4s"            %% "http4s-blaze-server"             % http4sVersion,
  "org.http4s"            %% "http4s-blaze-client"             % http4sVersion,
  "org.typelevel"         %% "cats-core"                       % catsVersion,
  "org.typelevel"         %% "cats-effect"                     % catsEffectVersion,
  "io.circe"              %% "circe-generic"                   % circeVersion,
  "io.circe"              %% "circe-generic-extras"            % circeVersion,
  "io.circe"              %% "circe-parser"                    % circeVersion,
  "io.circe"              %% "circe-refined"                   % circeVersion,
  "org.http4s"            %% "http4s-circe"                    % http4sVersion,
  "org.scalatest"         %% "scalatest"                       % scalaTestVersion % Test,
  "org.specs2"            %% "specs2-core"                     % specs2Version % Test,
  "org.specs2"            %% "specs2-matcher"                  % specs2Version % Test,
  "org.specs2"            %% "specs2-scalacheck"               % specs2Version % Test,
  "com.github.pureconfig" %% "pureconfig"                      % pureConfigVersion,
  "com.github.pureconfig" %% "pureconfig-http4s"               % pureConfigVersion,
  "co.fs2"                %% "fs2-io"                          % fs2Version,
  "org.tpolecat"          %% "doobie-core"                     % doobieVersion,
  "org.tpolecat"          %% "doobie-postgres"                 % doobieVersion,
  "org.tpolecat"          %% "doobie-specs2"                   % doobieVersion,
  "org.tpolecat"          %% "doobie-hikari"                   % doobieVersion,
  "io.chrisdavenport"     %% "fuuid-http4s"                    % fuuidVersion,
  "io.chrisdavenport"     %% "fuuid-doobie"                    % fuuidVersion,
  "io.chrisdavenport"     %% "fuuid-circe"                     % fuuidVersion,
  "dev.profunktor"        %% "http4s-jwt-auth"                 % http4sJwtVersion,
  "org.testcontainers"     % "postgresql"                      % testContainersPostgresqlVersion % Test,
  "com.dimafeng"          %% "testcontainers-scala"            % testContainersVersion % Test,
  "com.dimafeng"          %% "testcontainers-scala-postgresql" % testContainersVersion % Test,
  "org.flywaydb"           % "flyway-core"                     % flyWayVersion,
  "com.beachape"          %% "enumeratum-scalacheck"           % enumeratumScalacheckVersion % Test,
  "io.chrisdavenport"     %% "log4cats-slf4j"                  % log4CatsVersion,
  "io.github.jmcardon"    %% "tsec-common"                     % tsecVersion,
  "io.github.jmcardon"    %% "tsec-mac"                        % tsecVersion,
  "org.typelevel"         %% "cats-tagless-macros"             % "0.11"
)
addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0")
enablePlugins(DockerComposePlugin)
