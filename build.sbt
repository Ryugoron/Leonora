lazy val commonSettings = Seq(
  version := "1",
  scalaVersion := "2.11.6"
)

lazy val leo = (project in file(".")).
  settings(commonSettings:_*).
  settings(
    organization := "org.leo",
    
    name := "Leonora",
    
    description := "A Higher-Order Problem Preprocessor.",
    
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
      "org.scalatest" % "scalatest_2.10" % "2.0" % "test"),
    
    scalacOptions ++= Seq("-Xelide-below","401"),
    
    mainClass in (Compile, packageBin) := Some("leo.NormalizationMain"),
    
    mainClass in (Compile, run) := Some("leo.NormalizationMain"),
    
    javaOptions += "-Xss4m",
    
    parallelExecution in Test := false,
    
    logLevel in compile := Level.Warn,
    
    exportJars := true
  )
