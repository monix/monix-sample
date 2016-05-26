import sbt.Project.projectToRef

lazy val clients = Seq(client)
lazy val scalaV = "2.11.8"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd, gzip),
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    "com.vmunier" %% "play-scalajs-scripts" % "0.5.0",
    "org.webjars" % "jquery" % "1.12.3",
    "org.webjars.bower" % "epoch" % "0.6.0",
    "org.webjars" % "d3js" % "3.5.16",
    "io.monix" %% "monix" % "2.0-RC3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
  ),
  // Heroku specific
  herokuAppName in Compile := "monix-sample",
  herokuSkipSubProjects in Compile := false,

  // Play Framework
  routesGenerator := InjectedRoutesGenerator
).enablePlugins(PlayScala).
  aggregate(clients.map(projectToRef): _*).
  dependsOn(sharedJvm)

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJs)
  .settings(
    scalaVersion := scalaV,
    persistLauncher := true,
    persistLauncher in Test := false,
//    sourceMapsDirectories += sharedJs.base / "..",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.0",
      "io.monix" %%% "monix" % "2.0-RC3"
    )
  )

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(scalaVersion := scalaV)
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

// for Eclipse users
EclipseKeys.skipParents in ThisBuild := false
// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in (server, Compile))
