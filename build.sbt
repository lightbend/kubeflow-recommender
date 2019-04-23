name in ThisBuild := "RecommenderKubeflow"
version in ThisBuild := "0.0.1"
organization in ThisBuild := "lightbend"
scalaVersion in ThisBuild := "2.12.8" 
scalacOptions in ThisBuild ++= Seq(
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-language:postfixOps",
  "-deprecation")

// settings for a native-packager based docker project based on sbt-docker plugin
def sbtdockerAppBase(id: String)(base: String = id): Project = Project(id, base = file(base))
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    dockerfile in docker := {
      val appDir = stage.value
      val targetDir = "/opt/app"

      new Dockerfile {
        from("lightbend/java-bash-base:0.0.1")
        copy(appDir, targetDir)
        run("chmod", "-R", "777", "/opt/app")
        entryPoint(s"$targetDir/bin/${executableScriptName.value}")
      }
    },

    // Set name for the image
    imageNames in docker := Seq(
      ImageName(namespace = Some(organization.value),
        repository = name.value.toLowerCase,
        tag = Some(version.value))
    ),

    buildOptions in docker := BuildOptions(cache = false)
  )


lazy val protobufs = (project in file("./protobufs"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value))

lazy val configuration = (project in file("./configuration"))
  .settings(libraryDependencies ++= Seq(Dependencies.typesafeConfig, Dependencies.ficus))

lazy val client = sbtdockerAppBase("kubeflow-recommender-data")("./client")
  .settings(mainClass in Compile := Some("com.lightbend.recommender.client.client.DataProviderCloud"))
  .settings(libraryDependencies ++= Dependencies.clientDependencies)
  .dependsOn(protobufs, configuration)

lazy val modelserver = sbtdockerAppBase("kubeflow-modelserver")("./modelserver")
  .settings(mainClass in Compile := Some("com.lightbend.recommender.server.RecommenderModelServer"))
  .settings(libraryDependencies ++= Dependencies.akkaServerDependencies)
  .settings(libraryDependencies ++= Seq(Dependencies.gson))
  .dependsOn(protobufs, configuration)

lazy val datapublisher = sbtdockerAppBase("kubeflow-datapublisher")("./datapublisher")
  .settings(mainClass in Compile := Some("com.lightbend.recommender.datapublisher.DataPublisher"))
  .settings(libraryDependencies ++= Seq(Dependencies.minio))
  .dependsOn(protobufs, configuration)

lazy val modelpublisher = sbtdockerAppBase("kubeflow-modelpublisher")("./modelpublisher")
  .settings(mainClass in Compile := Some("com.lightbend.recommender.modelpublisher.ModelPublisher"))
  .settings(libraryDependencies ++= Seq(Dependencies.minio, Dependencies.ScalajHTTP))
  .dependsOn(client)

lazy val root = (project in file(".")).
  aggregate(protobufs, client, configuration, modelserver, modelpublisher, datapublisher)
