# Recommender example for kubeflow

This is an end-to-end example showing how to use Kubeflow for both machine learning and model serving.

The example consists of the following steps:
* Building machine learning using [Kubeflow's Jupiter](https://www.kubeflow.org/docs/components/jupyter/)
The machine learning implementation is using [Collaborative filtering](https://en.wikipedia.org/wiki/Collaborative_filtering)
to recommend products to the user (out of the set of products) based on his purchsing history. An implementation
is first converts purchasing history to the rating matrix, based on this [blog post](https://medium.com/datadriveninvestor/how-to-build-a-recommendation-system-for-purchase-data-step-by-step-d6d7a78800b6)
and then uses this matrix to build a prediction model, following this [repository](https://github.com/Piyushdharkar/Collaborative-Filtering-Using-Keras)
THe actual notebook can be found [here](recommender/Recommender_Kubeflow.ipynb)
* Once model is build, the python code is exported ([see here](recommender/Recommender_Kubeflow.py)) and is used for building 
[TFJob](https://www.kubeflow.org/docs/components/tftraining/). The Dockerfile is [here](recommender/Dockerfile). In addition
there is a [bash file](recommender/build.sh).
* Model serving is based on [TF-serving](https://www.kubeflow.org/docs/components/tfserving_new/). Due to limitations of [TF-serving](https://www.tensorflow.org/tfx/serving/serving_config#configuring_one_model)
I have decided to run two instances of TF-serving and alterate their usage for serving.

Additional components included in this implementation include the following:
* [Data Publister](datapublisher) is a project used for preparing new data for
machine learning. Whichever code is necessary to get the list of users and their 
current purchasing history goes here. For the simple implementation here I am not doing 
here anything - just give a [code sample](datapublisher/src/main/scala/com/lightbend/recommender/datapublisher/DataPublisher.scala)
oh how to update data used for learning.
* [Model server](modelserver) is a project implementing an actual model serving. It gets a stream
of data and leverages TF Serving for the actual model serving. Additionally,
it implements the second stream, that allows to change the URL of TF-serving based on the model update
* [Model Publisher](modelpublisher) is a project responsible for updating model for Model server.
It reads a current TF-server from a data file, makes sure that it is operational (by sending it HTTP request)
and if it is, publishes a new model (new URL) to the model server. 
The acual code is [here](modelpublisher/src/main/scala/com/lightbend/recommender/modelpublisher/ModelPublisher.scala).
* [Client](client) is a project responsible for publishing recommendation requests
to the model server. Code is [here](client/src/main/scala/com/lightbend/recommender/client/client/DataProviderCloud.scala).

For storing data used in the project (models, data) we are using [Minio](https://min.io/), which is part of Kubeflow installation. 

Finally we are using [Kubeflow pipelines](https://www.kubeflow.org/docs/components/pipelines/) for organizing and scheduling overall execution.  

## Building

## Installation

## License

Copyright (C) 2019 Lightbend Inc. (https://www.lightbend.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
