#!/usr/bin/env python
# coding: utf-8

# # Kubeflow pipeline
# This is a fairly simple pipeline, containing sequential steps:
# 1. Update data - implemented by lightbend/kubeflow-datapublisher:0.0.1 image
# 2. Run model training. Ideally we would run TFJob, but due to the current limitations for pipelines, we will directly use an image implementing training lightbend/ml-tf-recommender:0.0.1
# 3. Update serving model - implemented by lightbend/kubeflow-modelpublisher:0.0.1

# Setup

from kfp import compiler
import kfp.dsl as dsl
from kubernetes import client as k8s_client


# # Define a Pipeline

@dsl.pipeline(
  name='Recommender model update',
  description='Demonstrate usage of pipelines for multi-step model update'
)
def recommender_pipeline():
    # Load new data
  data = dsl.ContainerOp(
      name='updatedata',
      image='lightbend/kubeflow-datapublisher:0.0.1') \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_URL',value='http://minio-service:9000/')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_KEY', value='minio')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_SECRET', value='minio123'))
    # Train the model
  train = dsl.ContainerOp(
      name='trainmodel',
      image='lightbend/ml-tf-recommender:0.0.1') \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_URL',value='minio-service:9000')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_KEY', value='minio')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_SECRET', value='minio123'))
  train.after(data)
    # Publish new model model
  publish = dsl.ContainerOp(
      name='publishmodel',
      image='lightbend/kubeflow-modelpublisher:0.0.1') \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_URL',value='http://minio-service:9000/')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_KEY', value='minio')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_SECRET', value='minio123')) \
    .add_env_variable(k8s_client.V1EnvVar(name='KAFKA_BROKERS', value='strimzi-kafka-brokers:9092')) \
    .add_env_variable(k8s_client.V1EnvVar(name='DEFAULT_RECOMMENDER_URL', value='http://recommender-service:8500')) \
    .add_env_variable(k8s_client.V1EnvVar(name='ALTERNATIVE_RECOMMENDER_URL', value='http://recommender1-service:8500'))
  publish.after(train)

# Compile pipeline

compiler.Compiler().compile(recommender_pipeline, 'pipeline.tar.gz')
