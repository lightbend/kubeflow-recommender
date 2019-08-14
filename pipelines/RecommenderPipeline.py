#!/usr/bin/env python
# coding: utf-8

# # Kubeflow pipeline
# This is a fairly simple pipeline, containing sequential steps:
# 
# 1. Update data - implemented by lightbend/kubeflow-datapublisher:0.0.1 image
# 2. Run model training. Ideally we would run TFJob, but due to the current limitations for pipelines, we will directly use an image implementing training lightbend/ml-tf-recommender:0.0.1
# 3. Update serving model - implemented by lightbend/kubeflow-modelpublisher:0.0.1

# # Setup

# In[1]:


from kfp import compiler
import kfp.dsl as dsl
from kubernetes import client as k8s_client


@dsl.pipeline(
  name='Recommender model update',
  description='Demonstrate usage of pipelines for multi-step model update'
)
def recommender_pipeline():
    # Load new data
  data = dsl.ContainerOp(
      name='updatedata',
      output_artifact_paths={
        'mlpipeline-ui-metadata': '/output/mlpipeline-ui-metadata.json',
        'mlpipeline-metrics': '/output/mlpipeline-metrics.json',
      },  
      image='lightbend/kubeflow-datapublisher:0.0.1') \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_URL',value='http://minio-service.kubeflow.svc.cluster.local:9000')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_KEY', value='minio')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_SECRET', value='minio123')) \
    .add_volume(k8s_client.V1Volume(name='outputs', empty_dir=k8s_client.V1EmptyDirVolumeSource())) \
    .add_volume_mount(k8s_client.V1VolumeMount(name='outputs', mount_path='/output')) 
    # Train the model
  train = dsl.ContainerOp(
      name='trainmodel',
      output_artifact_paths={
        'mlpipeline-ui-metadata': '/output/mlpipeline-ui-metadata.json',
        'mlpipeline-metrics': '/output/mlpipeline-metrics.json',
      },  
      image='lightbend/ml-tf-recommender:0.0.1') \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_URL',value='minio-service.kubeflow.svc.cluster.local:9000')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_KEY', value='minio')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_SECRET', value='minio123')) \
    .add_volume(k8s_client.V1Volume(name='outputs', empty_dir=k8s_client.V1EmptyDirVolumeSource())) \
    .add_volume_mount(k8s_client.V1VolumeMount(name='outputs', mount_path='/output')) 
  train.after(data)
    # Publish new model model
  publish = dsl.ContainerOp(
      name='publishmodel',
      output_artifact_paths={
        'mlpipeline-ui-metadata': '/output/mlpipeline-ui-metadata.json',
        'mlpipeline-metrics': '/output/mlpipeline-metrics.json',
      },  
      image='lightbend/kubeflow-modelpublisher:0.0.1') \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_URL',value='http://minio-service.kubeflow.svc.cluster.local:9000')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_KEY', value='minio')) \
    .add_env_variable(k8s_client.V1EnvVar(name='MINIO_SECRET', value='minio123')) \
    .add_env_variable(k8s_client.V1EnvVar(name='KAFKA_BROKERS', value='strimzi-kafka-brokers.boris.svc.cluster.local:9092')) \
    .add_env_variable(k8s_client.V1EnvVar(name='DEFAULT_RECOMMENDER_URL', value='http://recommender.boris.svc.cluster.local:8501')) \
    .add_env_variable(k8s_client.V1EnvVar(name='ALTERNATIVE_RECOMMENDER_URL', value='http://recommender1.boris.svc.cluster.local:8501')) \
    .add_volume(k8s_client.V1Volume(name='outputs', empty_dir=k8s_client.V1EmptyDirVolumeSource())) \
    .add_volume_mount(k8s_client.V1VolumeMount(name='outputs', mount_path='/output')) 
  publish.after(train)


# # Compile pipeline
compiler.Compiler().compile(recommender_pipeline, 'pipeline.tar.gz')



