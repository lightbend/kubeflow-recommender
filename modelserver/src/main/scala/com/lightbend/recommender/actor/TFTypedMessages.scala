package com.lightbend.recommender.actor

import akka.Done
import akka.actor.typed.ActorRef
import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.recommendationrequest.RecommendationRequest
import com.lightbend.recommender.model.{ModelToServeStats, ProductPredictions}

// Controller
trait TFRecommenderActor
case class ServeData(reply: ActorRef[Option[ProductPredictions]], record : RecommendationRequest) extends TFRecommenderActor
case class ModelURL(reply: ActorRef[Done], model : ModelDescriptor) extends TFRecommenderActor
case class GetState(reply: ActorRef[ModelToServeStats]) extends TFRecommenderActor
