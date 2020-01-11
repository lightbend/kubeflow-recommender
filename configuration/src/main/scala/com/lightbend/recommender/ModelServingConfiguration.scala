package com.lightbend.recommender

import com.typesafe.config.ConfigFactory

/**
  * Various configuration parameters.
  */
object ModelServingConfiguration {

  val config = ConfigFactory.load()


  val KAFKA_BROKER = config.getString("kafka.brokers")

  val DATA_TOPIC = config.getString("kafka.datatopic")
  val MODELS_TOPIC = config.getString("kafka.modeltopic")
  val MODELS_URL_TOPIC = config.getString("kafka.modelurltopic")

  val DATA_GROUP = config.getString("kafka.datagroup")
  val MODELS_GROUP = config.getString("kafka.modelgroup")
  val MODELS_URL_GROUP = config.getString("kafka.modelurlgroup")

  val MODELSERVING_PORT = config.getString("serving.port").toInt
  val DATAPUBLISHINTERVAL = config.getString("loader.publishinterval")

  val MINIO_URL = config.getString("minio.miniourl")
  val MINIO_KEY = config.getString("minio.miniokey")
  val MINIO_SECRET = config.getString("minio.miniosecret")

  val DEFAULT_URL = config.getString("recommender.defaulturl")
  val ALTERNATIVE_URL = config.getString("recommender.alternativeurl")
}