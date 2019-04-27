package com.lightbend.recommender.client.client

import com.lightbend.recommender.client
import com.lightbend.recommender.ModelServingConfiguration._

object DataReader {

  def main(args: Array[String]) {

    println(s"Using kafka brokers at ${KAFKA_BROKER}")

    val listener = client.MessageListener(KAFKA_BROKER, MODELS_TOPIC, MODELS_GROUP, new RecordProcessor())
    listener.start()
  }
}
