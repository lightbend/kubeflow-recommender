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
