package com.lightbend.recommender.actor

import akka.{Done, japi}
import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.lightbend.recommender.model._

/**
  * This actor forwards requests to score records to TensorFlow Serving
  */
class TFModelServerBehaviour(context: ActorContext[TFRecommenderActor]) extends AbstractBehavior[TFRecommenderActor] {

  var currentState = new ModelToServeStats()
  val gson = new Gson
  var uri : Option[String] = None

  println(s"Creating a new recommender Actor")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher


  /**
    * When passed a record, it creates a request to pass over HTTP to TensorFlow Serving to score the record.
    * A handler is set up to process the result when returned to the Future. If successful, the result is packaged
    * and returned to the sender. The other supported msg is a request for the current state. Note also how errors
    * are handled.
    * @param msg
    */
  override def onMessage(msg: TFRecommenderActor): Behavior[TFRecommenderActor] = {
    msg match {
      case record: ServeData => // Serve data
        // Check whether we have URI
        uri match {
          case Some(server) =>
            // Create request
            val start = System.currentTimeMillis()
            val products = record.record.products.map(Array(_)).toArray
            val users = record.record.products.map(p => Array(record.record.user)).toArray
            val request = Request("", RequestInputs(products, users))

            // Post request
            val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(
              method = HttpMethods.POST,
              uri = server,
              entity = HttpEntity(ContentTypes.`application/json`, gson.toJson(request))
            ))

            // Get Result
            responseFuture
              .onComplete {
                case Success(res) =>
                  Unmarshal(res.entity).to[String].map(pString => {
                    val prediction = gson.fromJson(pString, classOf[TFPredictionResult])
                    val predictions = prediction.outputs.recommendations.map(_ (0))
                      .zip (record.record.products) map (r => ProductPrediction(r._2, r._1))
                    val duration = System.currentTimeMillis() - start
                     val result = ProductPredictions(currentState.name, duration, predictions)
                    // Update state
                    currentState = currentState.incrementUsage(duration)
                    // result
                    record.reply ! Some(result)
                  })
                case Failure(_) => sys.error("something wrong")
                  record.reply ! None
              }
          case _ => record.reply ! None
        }
      case model : ModelURL => // update model
        println(s"New model ${model.model.name}, ${model.model.description}, ${model.model.url}")
        uri = Some(model.model.url)
        currentState = ModelToServeStats(model.model)
        model.reply ! Done
      case getState: GetState => // State query
        getState.reply ! currentState
    }
    this
  }
}

// Case classes for json mapping
case class RequestInputs(products : Array[Array[Long]], users : Array[Array[Long]])
case class Request(signature_name : String, inputs : RequestInputs)
case class RecommendationOutputs(@SerializedName("model-version")model : Array[Int], recommendations : Array[Array[Double]])
case class TFPredictionResult(outputs : RecommendationOutputs)
