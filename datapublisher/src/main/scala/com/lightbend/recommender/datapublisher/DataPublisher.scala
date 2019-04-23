package com.lightbend.recommender.datapublisher

import java.io.ByteArrayInputStream
import java.io.InputStream

import com.lightbend.recommender.ModelServingConfiguration._
import io.minio.MinioClient

import scala.io.Source

// This is a simplified version of data publisher.
// Instead of reading current information from the database, this code is republishes the same data
object DataPublisher {

  val default_directory = "recommender"

  def main(args: Array[String]): Unit = {

    println(s"DataPublisher for model rebuild. Minio: url - $MINIO_URL, key - $MINIO_KEY, secret - $MINIO_SECRET")
    // Create a minioClient with the MinIO Server URL, Access key and Secret key.
    val minioClient = new MinioClient(MINIO_URL, MINIO_KEY, MINIO_SECRET)

    // For initial implementation we will not overwrite existing data, just update server
/*
    // Remove existing objects
    try {
      // Check whether the object exists using statObject().
      // If the object is not found, statObject() throws an exception,
      // else it means that the object exists.
      // Execution is successful.
      minioClient.statObject("data", "recommender/transactions.csv")
      minioClient.removeObject("data", "recommender/transactions.csv")
    } catch {case _: Throwable => }
    try {
      minioClient.statObject("data", "recommender/users.csv")
      minioClient.removeObject("data", "recommender/users.csv")
    } catch {case _: Throwable => }

    // Recreate them
    minioClient.putObject("data",  "recommender/transactions.csv", "data/transactions.csv" ,"application/octet-stream")
    minioClient.putObject("data",  "recommender/users.csv", "data/users.csv" ,"application/octet-stream")
*/
    // Read current directory
    var directory = ""
    var stream : InputStream = null
    try{
      stream = minioClient.getObject("data", "recommender/directory.txt")
      directory = Source.fromInputStream(stream).mkString
      stream.close
    } catch {case _: Throwable => }


    // calculate a new directory name
    directory = if(directory == default_directory) default_directory.concat("1") else default_directory

    // remove current directory file
    try{
      minioClient.removeObject("data", "recommender/directory.txt")
    } catch {case _: Throwable => }

    // Write directory name
    val bis = new ByteArrayInputStream(directory.getBytes("UTF-8"))
    minioClient.putObject("data", "recommender/directory.txt", bis, "application/octet-stream")

    // Remove any models in the directory
    val models = minioClient.listObjects("models",directory)
    models.forEach(model => {
      val name = model.get().objectName()
      println(s"removing model file ${model.get().objectName()}")
      minioClient.removeObject("models", name)
    })

    // Read it back
    stream = minioClient.getObject("data", "recommender/directory.txt")
    val result = Source.fromInputStream(stream).mkString
    stream.close
    println(result)
  }
}
