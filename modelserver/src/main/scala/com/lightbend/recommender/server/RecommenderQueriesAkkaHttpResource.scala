package com.lightbend.recommender.server

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.lightbend.recommender.actor.{GetState, TFRecommenderActor}
import com.lightbend.recommender.model.ModelToServeStats
import de.heikoseeberger.akkahttpjackson.JacksonSupport

import scala.concurrent.duration._

object RecommenderQueriesAkkaHttpResource extends JacksonSupport {

  implicit val askTimeout = Timeout(30.seconds)

  def storeRoutes(modelserver: ActorRef[TFRecommenderActor])(implicit scheduler: Scheduler) : Route =
    get {
      // Get statistics
      path("state") {
        onSuccess(modelserver ? ((replyTo: ActorRef[ModelToServeStats]) => GetState(replyTo))) {
          case stats : ModelToServeStats =>
            complete(stats)
        }
      }
    }
}
