package com.lightbend.recommender.model

import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.recommendationrequest.RecommendationRequest

import scala.util.Try

/**
  * Model serving statistics definition
  */
case class ModelToServeStats(
  name: String = "",
  description: String = "",
  since: Long = 0,
  var usage: Long = 0,
  var duration: Double = .0,
  var min: Long = Long.MaxValue,
  var max: Long = Long.MinValue) {

  /**
    * Increment model serving statistics; invoked after scoring every record.
    * @arg execution Long value for the milliseconds it took to score the record.
    */
  def incrementUsage(execution : Long) : ModelToServeStats = {
    usage = usage + 1
    duration = duration + execution
    if(execution < min) min = execution
    if(execution > max) max = execution
    this
  }
}

object ModelToServeStats {
  def apply(m : ModelDescriptor): ModelToServeStats = ModelToServeStats(m.name, m.description, System.currentTimeMillis())
}

// Case classes for result of prediction
case class ProductPrediction(product : Long, prediction : Double)
case class ProductPredictions(model : String, execution: Long, products: Seq[ProductPrediction])

// Data converter
object DataRecord {

  def datafromByteArray(message: Array[Byte]): Try[RecommendationRequest] = Try {
//    println("New data request")
    RecommendationRequest.parseFrom(message)
  }

  def modelfromByteArray(message: Array[Byte]): Try[ModelDescriptor] = Try {
//    println("New model request")
    ModelDescriptor.parseFrom(message)
  }
}