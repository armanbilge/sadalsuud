ThisBuild / tlBaseVersion := "0.0"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "Arman Bilge"
ThisBuild / developers += tlGitHubDev("armanbilge", "Arman Bilge")
ThisBuild / startYear := Some(2022)

ThisBuild / tlUntaggedAreSnapshots := false
ThisBuild / tlSonatypeUseLegacyHost := false

val Scala3 = "3.2.2"
ThisBuild / crossScalaVersions := Seq(Scala3)

val CatsVersion = "2.9.0"
val CatsEffectVersion = "3.4.6"
val Fs2Version = "3.7.0"
val SchrodingerVersion = "0.4-fb30534"
val MunitVersion = "1.0.0-M7"
val MunitCatsEffectVersion = "2.0.0-M3"

ThisBuild / scalacOptions ++= Seq("-new-syntax", "-indent", "-source:future")
ThisBuild / Test / testOptions += Tests.Argument("+l")

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))
ThisBuild / tlJdkRelease := Some(8)

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "sadalsuud",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsVersion,
      "org.typelevel" %%% "cats-effect" % CatsEffectVersion,
      "co.fs2" %%% "fs2-core" % Fs2Version,
      "com.armanbilge" %%% "schrodinger-monte-carlo" % SchrodingerVersion,
      "org.scalameta" %%% "munit-scalacheck" % MunitVersion % Test,
      "org.typelevel" %%% "munit-cats-effect" % MunitCatsEffectVersion % Test,
    ),
  )
