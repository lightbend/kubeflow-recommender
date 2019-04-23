/*
 * Copyright (C) 2017-2019  Lightbend
 *
 * This file is part of the Lightbend model-serving-tutorial (https://github.com/lightbend/model-serving-tutorial)
 *
 * The model-serving-tutorial is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License Version 2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Versions._
import sbt._

object Dependencies {

  val kafka                 = "org.apache.kafka"        %% "kafka"                              % kafkaVersion
  val curator               = "org.apache.curator"      % "curator-test"                        % curatorVersion                 // ApacheV2
  val commonIO              = "commons-io"              % "commons-io"                          % commonIOVersion
  
  val alpakkaKafka          = "com.typesafe.akka"       %% "akka-stream-kafka"                  % alpakkaKafkaVersion

  val akkaStreamTyped       = "com.typesafe.akka"       %% "akka-stream-typed"                  % akkaVersion
  val akkaHttp              = "com.typesafe.akka"       %% "akka-http"                          % akkaHttpVersion
  val akkaHttpJsonJackson   = "de.heikoseeberger"       %% "akka-http-jackson"                  % akkaHttpJsonVersion
  val akkatyped             = "com.typesafe.akka"       %% "akka-actor-typed"                   % akkaVersion

  val gson                  = "com.google.code.gson"     % "gson"                               % gsonVersion

  val slf4jlog4j            = "org.slf4j"                % "slf4j-log4j12"                      % slf4jlog4jVersion
  
  val minio                 = "io.minio"                 % "minio"                              % minioVersion
  
  val ScalajHTTP            = "org.scalaj"              %% "scalaj-http"                        %  ScalajHTTPVersion

  val typesafeConfig        = "com.typesafe"            %  "config"                             % TypesafeConfigVersion
  val ficus                 = "com.iheart"              %% "ficus"                              % FicusVersion

  // Adds the @silencer annotation for suppressing deprecation warnings we don't care about.
  val silencer              = "com.github.ghik"         %% "silencer-lib"                       % silencerVersion     % Provided
  val silencerPlugin        = "com.github.ghik"         %% "silencer-plugin"                    % silencerVersion     % Provided

  val silencerDependencies = Seq(compilerPlugin(silencerPlugin), silencer)

  val clientDependencies = Seq(kafka, curator, commonIO, slf4jlog4j) ++ silencerDependencies
  val akkaServerDependencies = Seq(alpakkaKafka, akkaStreamTyped, akkatyped, akkaHttp, akkaHttpJsonJackson, slf4jlog4j) ++ silencerDependencies
}
