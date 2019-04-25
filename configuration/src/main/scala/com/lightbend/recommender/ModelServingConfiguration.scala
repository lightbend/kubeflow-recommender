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

  val DATA_GROUP = config.getString("kafka.datagroup")
  val MODELS_GROUP = config.getString("kafka.modelgroup")

  val MODELSERVING_PORT = config.getString("serving.port").toInt
  val DATAPUBLISHINTERVAL = config.getString("loader.publishinterval")

  val MINIO_URL = config.getString("minio.miniourl")
  val MINIO_KEY = config.getString("minio.miniokey")
  val MINIO_SECRET = config.getString("minio.miniosecret")

  val DEFAULT_URL = config.getString("recommender.defaulturl")
  val ALTERNATIVE_URL = config.getString("recommender.alternativeurl")
}
