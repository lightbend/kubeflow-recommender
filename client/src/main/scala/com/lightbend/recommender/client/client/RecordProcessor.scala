package com.lightbend.recommender.client.client

import com.lightbend.recommender.client.RecordProcessorTrait
import org.apache.kafka.clients.consumer.ConsumerRecord

class RecordProcessor extends RecordProcessorTrait[Array[Byte], Array[Byte]] {
  override def processRecord(record: ConsumerRecord[Array[Byte], Array[Byte]]): Unit =
    println("Get Message")
}
