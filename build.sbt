import sbt.Project.projectToRef

lazy val mainScalaVersion = "2.11.9"
lazy val monixVersion = "2.2.4"

lazy val server = (project in file("server")).settings(
  scalaVersion := mainScalaVersion,

  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,

  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.0.0",
    "org.webjars" % "jquery" % "1.12.4",
    "org.webjars.bower" % "epoch" % "0.6.0",
    "org.webjars" % "d3js" % "3.5.17",
    "io.monix" %% "monix" % monixVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
  ),

  // Heroku specific
  herokuAppName in Compile := "monix-sample",
  herokuSkipSubProjects in Compile := false,

  // Play Framework
  routesGenerator := InjectedRoutesGenerator)
  .enablePlugins(PlayScala)
  .dependsOn(sharedJvm)

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)
  .settings(
    scalaVersion := mainScalaVersion,
    scalaJSUseMainModuleInitializer := true,
    scalaJSUseMainModuleInitializer in Test := false,
    // sourceMapsDirectories += sharedJs.base / "..",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "io.monix" %%% "monix" % monixVersion
    )
  )

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(scalaVersion := mainScalaVersion)
  .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

// for Eclipse users
EclipseKeys.skipParents in ThisBuild := false
// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in (server, Compile))
