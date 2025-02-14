import rocks.muki.graphql.quietError
import rocks.muki.graphql.schema.SchemaLoader
import sbt.File
import sbt.Keys.libraryDependencies



name := "tdr-consignment-api"
version := "0.1.0-SNAPSHOT"

description := "The consignment API for TDR"

scalaVersion := "2.13.8"
scalacOptions ++= Seq("-deprecation", "-feature")

resolvers ++= Seq[Resolver](
  "Sonatype Releases" at "https://dl.bintray.com/mockito/maven/",
  "TDR Releases" at "s3://tdr-releases-mgmt"
)

(Compile / run / mainClass) := Some("uk.gov.nationalarchives.tdr.api.http.ApiServer")

graphqlSchemas += GraphQLSchema(
  "consignmentApi",
  "API schema from the schema.graphql file in the repository root",
  Def.task(
    GraphQLSchemaLoader
      .fromFile(baseDirectory.value.toPath.resolve("schema.graphql").toFile)
      .loadSchema()
  ).taskValue
)

val graphqlValidateSchemaTask = Def.inputTask[Unit] {
  val log = streams.value.log
  val changes = graphqlSchemaChanges.evaluated
  if (changes.nonEmpty) {
    changes.foreach(change => log.error(s" * ${change.description}"))
    quietError("Validation failed: Changes found")
  }
}

graphqlValidateSchema := graphqlValidateSchemaTask.evaluated

enablePlugins(GraphQLSchemaPlugin)

graphqlSchemaSnippet := "uk.gov.nationalarchives.tdr.api.graphql.GraphQlTypes.schema"

lazy val akkaHttpVersion = "10.2.9"
lazy val circeVersion = "0.14.1"
lazy val testContainersVersion = "0.40.3"

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria" % "3.0.0",
  "org.sangria-graphql" %% "sangria-slowlog" % "2.0.4",
  "org.sangria-graphql" %% "sangria-circe" % "1.3.2",
  "org.sangria-graphql" %% "sangria-spray-json" % "1.0.2",
  "org.sangria-graphql" %% "sangria-relay" % "2.1.0",

  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream"          % "2.6.19",

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-optics" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "uk.gov.nationalarchives" %% "consignment-api-db" % "0.0.69",
  "org.postgresql" % "postgresql" % "42.3.3",
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "ch.megard" %% "akka-http-cors" % "1.1.3",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.0.1",
  "org.jboss.logging" % "jboss-logging" % "3.4.3.Final",
  "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "3.0.4",
  "software.amazon.awssdk" % "rds" % "2.17.155",
  "software.amazon.awssdk" % "sts" % "2.17.155",
  "com.github.cb372" %% "scalacache-caffeine" % "0.28.0",
  "uk.gov.nationalarchives.oci" % "oci-tools-scala_2.13" % "0.2.0",
  "org.scalatest" %% "scalatest" % "3.2.11" % Test,
  "org.mockito" %% "mockito-scala" % "1.17.5" % Test,
  "org.mockito" %% "mockito-scala-scalatest" % "1.17.5" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.6.19" % Test,
  "com.tngtech.keycloakmock" % "mock" % "0.11.0" % Test,
  "uk.gov.nationalarchives" %% "tdr-auth-utils" % "0.0.29",
  "io.github.hakky54" % "logcaptor" % "2.7.9" % Test,
  "com.dimafeng" %% "testcontainers-scala-scalatest" % testContainersVersion % Test,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersVersion % Test
)

(Test / javaOptions) += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf"
(Test / fork) := true

(assembly / assemblyJarName) := "consignmentapi.jar"

(assembly / assemblyMergeStrategy) := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case _ => MergeStrategy.first
}

(assembly / mainClass) := Some("uk.gov.nationalarchives.tdr.api.http.ApiServer")
