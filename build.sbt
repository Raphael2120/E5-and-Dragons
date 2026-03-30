val scala3Version = "3.7.3"

inThisBuild(
  List(
    version           := "0.1.0-SNAPSHOT",
    organization      := "io.github.guihardbastien",
    scalaVersion      := scala3Version,
    semanticdbEnabled := true,
    scalacOptions     += "-java-output-version:25"
  )
)

// ── LIB VERSIONS ─────────────────────────────────────────────────────────────
val munitVersion      = "1.2.1"
val http4sVersion     = "0.23.29"
val circeVersion      = "0.14.10"
val catsEffectVersion = "3.5.7"
val logbackVersion    = "1.5.18"
val munitCEVersion    = "2.0.0"

// ── DEPENDENCIES ─────────────────────────────────────────────────────────────
val munit             = "org.scalameta" %% "munit"               % munitVersion   % Test
val munitCE           = "org.typelevel" %% "munit-cats-effect"   % munitCEVersion % Test

val http4sEmberServer = "org.http4s"    %% "http4s-ember-server" % http4sVersion
val http4sDsl         = "org.http4s"    %% "http4s-dsl"          % http4sVersion
val http4sCirce       = "org.http4s"    %% "http4s-circe"        % http4sVersion
val circeCore         = "io.circe"      %% "circe-core"          % circeVersion
val circeGeneric      = "io.circe"      %% "circe-generic"       % circeVersion
val circeParser       = "io.circe"      %% "circe-parser"        % circeVersion
val logback           = "ch.qos.logback" % "logback-classic"     % logbackVersion % Runtime

// ── APPS ─────────────────────────────────────────────────────────────────────
lazy val endGame =
  (project in file("app/end-game"))
    .settings(
      name      := "endGame",
      mainClass := Some("Main"),
      libraryDependencies ++= Seq(munit, logback),
      javaOptions += "--sun-misc-unsafe-memory-access=allow",
      // Fat JAR for Docker
      assembly / assemblyJarName := "e5-dragons.jar",
      assembly / assemblyMergeStrategy := {
        case PathList("META-INF", "services", _*) => MergeStrategy.concat
        case PathList("META-INF", _*)             => MergeStrategy.discard
        case "reference.conf"                     => MergeStrategy.concat
        case "logback.xml"                        => MergeStrategy.first
        case _                                    => MergeStrategy.first
      }
    )
    .dependsOn(exploration, combat, socialInteraction, infra, commons)

// ── COMMONS ───────────────────────────────────────────────────────────────────
lazy val commons =
  (project in file("commons"))
    .settings(
      name := "commons",
      libraryDependencies ++= Seq(munit)
    )

// ── CORE ─────────────────────────────────────────────────────────────────────
lazy val exploration =
  (project in file("core/exploration"))
    .settings(
      name := "exploration",
      libraryDependencies ++= Seq(munit)
    )
    .dependsOn(commons)

lazy val combat =
  (project in file("core/combat"))
    .settings(
      name := "combat",
      libraryDependencies ++= Seq(munit)
    )
    .dependsOn(commons)

lazy val socialInteraction =
  (project in file("core/social-interaction"))
    .settings(
      name := "socialInteraction",
      libraryDependencies ++= Seq(munit)
    )
    .dependsOn(commons)

// ── INFRA ─────────────────────────────────────────────────────────────────────
lazy val infra =
  (project in file("infra/"))
    .settings(
      name := "infra",
      libraryDependencies ++= Seq(
        munit,
        munitCE,
        http4sEmberServer,
        http4sDsl,
        http4sCirce,
        circeCore,
        circeGeneric,
        circeParser
      )
    )
    .dependsOn(combat, exploration, socialInteraction)
