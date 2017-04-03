// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("heroku-sbt-plugin-releases",
  url("https://dl.bintray.com/heroku/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.play"       % "sbt-plugin"        % "2.5.13")
addSbtPlugin("org.scala-js"            % "sbt-scalajs"       % "0.6.15")
addSbtPlugin("com.vmunier"             % "sbt-web-scalajs"   % "1.0.3")
addSbtPlugin("com.typesafe.sbt"        % "sbt-gzip"          % "1.0.0")
addSbtPlugin("com.typesafe.sbt"        % "sbt-digest"        % "1.1.1")
addSbtPlugin("com.heroku"              % "sbt-heroku"        % "1.0.0")
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")