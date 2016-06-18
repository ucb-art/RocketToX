scalaVersion in ThisBuild := "2.11.7"

val chiselVersion = "e9e5bb28ac230ab7c54aab9ca30fbe164bbb84be"

lazy val chisel = ProjectRef(
  uri("git://github.com/ucb-bar/chisel.git#%s".format(chiselVersion)),
  "chisel"
)

val prjSettings = Project.defaultSettings ++ Seq(
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:reflectiveCalls",
                        "-language:implicitConversions", "-language:existentials"),
  libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ ) 
)

lazy val root = Project(
  id = "rocket-to-x",
  base = file("."),
  settings = prjSettings
).dependsOn(chisel
).aggregate(chisel)

