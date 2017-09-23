val ghost4jVersion = "1.0.1"
val log4sVersion = "1.3.4"
val pureConfigVersion = "0.7.0"
val scalazVersion = "7.2.15"
val scalazStreamVersion = "0.8.6a"
val scalaTestVersion = "3.0.1"
val shapelessVersion = "2.3.2"
val logbackVersion = "1.1.7"
val javaxMailVersion = "1.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "pdfexperiment",
    scalaVersion := "2.11.6",
    libraryDependencies ++= Seq("org.ghost4j"  % "ghost4j" % ghost4jVersion,
                                "org.scalaz"   %% "scalaz-core" % scalazVersion,
                                "org.scalaz"   %% "scalaz-concurrent" % scalazVersion,
                                "org.log4s"    %% "log4s"                 % log4sVersion,
                                "ch.qos.logback" % "logback-classic" % logbackVersion,
                                "com.github.pureconfig" %% "pureconfig"   % pureConfigVersion,
                                "javax.mail" % "javax.mail-api" % javaxMailVersion,
                                "com.sun.mail" % "javax.mail" % javaxMailVersion,
                                "com.sun.mail" % "pop3" % javaxMailVersion)

  )