val scala3Version = "3.7.3"

inThisBuild(
  List(
    version           := "0.1.0-SNAPSHOT",
    organization      := "io.github.guihardbastien",
    scalaVersion      := scala3Version,
    semanticdbEnabled := true,
    // Target Java 25 LTS regardless of the JDK used to compile
    scalacOptions     += "-java-output-version:25"
  )
)

// LIB VERSIONS
val munitVersion     = "1.2.1"
val scalaSwingVersion = "3.0.0"

// DEPENDENCIES
val munit      = "org.scalameta"        %% "munit"       % munitVersion     % Test
val scalaSwing = "org.scala-lang.modules" %% "scala-swing" % scalaSwingVersion

// APPS
lazy val endGame =
  (project in file("app/end-game"))
    .settings(
      name := "endGame",
      libraryDependencies ++= Seq(munit),
      // Fork a separate JVM so the app classloader is isolated from sbt's
      // classloader — required for resource loading and Swing to work correctly.
      run / fork := true,
      // Suppress sun.misc.Unsafe deprecation warnings introduced in Java 23 (JEP 471).
      // objectFieldOffset is used internally by Scala's LazyVals mechanism.
      javaOptions += "--sun-misc-unsafe-memory-access=allow"
    )
    .dependsOn(exploration, combat, socialInteraction, infra, commons)

// COMMONS
lazy val commons =
  (project in file("commons"))
    .settings(
      name := "commons",
      libraryDependencies ++= Seq(munit)
    )

// CORE
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

// INFRA
lazy val infra =
  (project in file("infra/"))
    .settings(
      name := "infra",
      libraryDependencies ++= Seq(munit, scalaSwing)
    )
    .dependsOn(combat, exploration, socialInteraction)
