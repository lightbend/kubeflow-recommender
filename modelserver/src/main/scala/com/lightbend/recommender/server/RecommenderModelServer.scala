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

package com.lightbend.recommender.server

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import akka.stream.typed.scaladsl.{ActorFlow, ActorMaterializer}
import akka.util.Timeout
import com.lightbend.recommender.actor.{ModelURL, ServeData, TFModelServerBehaviour, TFRecommenderActor}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import com.lightbend.recommender.ModelServingConfiguration._
import com.lightbend.recommender.model.{DataRecord, ProductPredictions}

import scala.concurrent.duration._
import scala.util.Success

/**
  * Entry point for an application that uses TensorFlow Serving for scoring records with models.
  */
object RecommenderModelServer {

  // Initialization

  implicit val modelServer = ActorSystem(
    Behaviors.setup[TFRecommenderActor](
      context => new TFModelServerBehaviour(context)), "Recommender")

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = modelServer.executionContext
  implicit val askTimeout = Timeout(30.seconds)

  // Configuration properties for the Kafka topic.
  val dataSettings = ConsumerSettings(modelServer.toUntyped, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(KAFKA_BROKER)
    .withGroupId(DATA_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val modelSettings = ConsumerSettings(modelServer.toUntyped, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(KAFKA_BROKER)
    .withGroupId(MODELS_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def main(args: Array[String]): Unit = {

    println(s"Recommendation server : brokers $KAFKA_BROKER")

    // Model stream processing
    Consumer.atMostOnceSource(modelSettings, Subscriptions.topics(MODELS_TOPIC))
      .map(record => DataRecord.modelfromByteArray(record.value)).collect { case Success(a) => a }
      .via(ActorFlow.ask(1)(modelServer)((elem, replyTo : ActorRef[Done]) => ModelURL(replyTo, elem)))
      .runWith(Sink.ignore) // run the stream, we do not read the results directly

    // Data stream processing
    Consumer.atMostOnceSource(dataSettings, Subscriptions.topics(DATA_TOPIC))
      .map(record => DataRecord.datafromByteArray(record.value)).collect { case Success(a) => a }
      .via(ActorFlow.ask(1)(modelServer)((elem, replyTo : ActorRef[Option[ProductPredictions]]) => ServeData(replyTo, elem)))
      .collect{ case (Some(result)) => result}
      .runWith(Sink.foreach(result =>
        println(s"Recommendation model ${result.model} served in ${result.execution} ms, with result ${result.products} " )))
    // Rest Server
    startRest(modelServer)
  }

  def startRest(modelServerManager: ActorSystem[TFRecommenderActor]): Unit = {

    implicit val timeout = Timeout(10.seconds)
    implicit val system = modelServerManager.toUntyped

    val host = "0.0.0.0"
    val port = MODELSERVING_PORT
    val routes = RecommenderQueriesAkkaHttpResource.storeRoutes(modelServerManager)(modelServerManager.scheduler)

    val _ = Http().bindAndHandle(routes, host, port) map
      { binding =>
        println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
    }
  }
}
