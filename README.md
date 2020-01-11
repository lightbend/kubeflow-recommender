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
* KubeFlow Pipeline used to coordinate execution of steps. Notebook for creation and execution of pipeline is [here](pipelines/Pipelines.ipynb). Python code
for pipeline creation is [here](pipelines/Pipelines.py). Once the Python code runs, it creates a file, called `pipeline.tar.gz`. Following this
[example](https://github.com/kubeflow/examples/tree/master/pipelines/mnist-pipelines), the definition can be uploaded to the pipelines UI. Now we can view the pipeline there
![Pipelines](images/static_recommender.png). In addition we can also run pipeline from there, that produces
the following result:
![Pipelines](images/Pipeline.png)
Currectly pipelines do not allow to define periodic definition in the pipeline definition, but from UI, it is possible 
to configure run as recurring and specify how often the run is executed
![Pipelines](images/periodic.png) 

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

The overall architecture of implementation is presented below:
![Overal Architecture](images/RecommenderKubeflow.png)

## Building

Different pieces are build differently. Python code - recommender ML - is directly build into docker (see above)
The rest of the code is is leveraging [SBT Docker plugin] and can be build using the following command:
```` 
sbt docker
````
that produces all images locally. These images have to be pushed into repository accessable from the cluster.
I was using [Docker Hub](https://hub.docker.com/)

## Installation

Installation requires several steps:
* install kubeflow following the [blog posts](https://www.lightbend.com/blog/how-to-deploy-kubeflow-on-lightbend-platform-openshift-introduction)
* Install kafka as described [here](kafka/README.md)
* Populate minio with [test data](data) following [this post](https://www.lightbend.com/blog/how-to-deploy-kubeflow-on-lightbend-platform-openshift-support-components-kubeflow)
* Start Jupiter, following this [blog post](https://www.lightbend.com/blog/how-to-deploy-kubeflow-on-lightbend-platform-openshift-jupyterhub-with-kubeflow) and
test the [notebook](recommender/Recommender_Kubeflow.ipynb)
* Try usage of TFJob for machine learning, following this [blog post](https://www.lightbend.com/blog/how-to-deploy-kubeflow-on-lightbend-platform-openshift-kubeflow-tensorflow-jobs)
Ksonnet definitions for these can be found [here](ks_app/README.md)
* Deploy model serving components recommender and recommender1 following [this blog posts](https://www.lightbend.com/blog/how-to-deploy-kubeflow-on-lightbend-platform-openshift-kubeflow-model-serving) 
Ksonnet definitions for these can be found [here](ks_app/README.md)
* Deploy Strimzi following this [documentation](https://developer.lightbend.com/docs/fast-data-platform/current-OpenShift/#strimzi-operator-kafka). 
After the operator is installed, use this [yaml file](kafka/kafka.yaml) to create Kafka cluster 
* Deploy model server and request provider using this [chart](recommenderchart)
* Enable usage of Argo following [blog post](https://www.lightbend.com/blog/how-to-deploy-kubeflow-on-lightbend-platform-openshift-support-components-kubeflow)
* Enable usage of Kubeflow pipelines following [blog post](https://www.lightbend.com/blog/how-to-deploy-kubeflow-on-lightbend-platform-openshift-deploying-kubeflow-pipelines)
* Test pipeline from the [notebook](pipelines/Pipelines.ipynb)
* Build pipeline definition using [Python code](pipelines/Pipelines.py) and upload it to the pipeline UI
* Start recurring pipeline execution.

## Installation update for version 0.6

* install kubeflow following the following [documentation](https://www.kubeflow.org/docs/started/k8s/kfctl-k8s-istio/). To run successfully on OpenShift (4.1)
set the following service account to scc (this is a superset) anyuid:
````
system:serviceaccount:kubeflow:admission-webhook-service-account,
system:serviceaccount:kubeflow:default,
system:serviceaccount:kubeflow:katib-controller,
system:serviceaccount:kubeflow:katib-ui,
system:serviceaccount:kubeflow:ml-pipeline,
system:serviceaccount:istio-system:prometheus,
system:serviceaccount:kubeflow:argo-ui,
system:serviceaccount:istio-system:istio-citadel-service-account,
system:serviceaccount:istio-system:istio-galley-service-account,
system:serviceaccount:istio-system:istio-mixer-service-account,
system:serviceaccount:istio-system:istio-pilot-service-account,
system:serviceaccount:istio-system:istio-egressgateway-service-account,
system:serviceaccount:istio-system:istio-ingressgateway-service-account,
system:serviceaccount:istio-system:istio-sidecar-injector-service-account,
system:serviceaccount:istio-system:grafana,
system:serviceaccount:istio-system:default,
system:serviceaccount:kubeflow:jupyter,
system:serviceaccount:kubeflow:jupyter-notebook,
system:serviceaccount:kubeflow:jupyter-hub,
system:serviceaccount:boris:default-editor,
system:serviceaccount:kubeflow:tf-job-operator,
system:serviceaccount:istio-system:kiali-service-account,
system:serviceaccount:boris:strimzi-cluster-operator
````
set the following service account to scc (this is a superset) privileged:
````
system:serviceaccount:openshift-infra:build-controller,
system:serviceaccount:kubeflow:admission-webhook-service-account,
system:serviceaccount:kubeflow:default,
system:serviceaccount:kubeflow:katib-controller,
system:serviceaccount:kubeflow:katib-ui,
system:serviceaccount:kubeflow:ml-pipeline,
system:serviceaccount:istio-system:jaeger,
system:serviceaccount:bookinfo:default,
system:serviceaccount:kubeflow:jupyter-web-app-service-account,
system:serviceaccount:kubeflow:argo,
system:serviceaccount:kubeflow:pipeline-runner
````
* To make Kiali running, update Kiali cluster role as follows
````
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: kiali
  selfLink: /apis/rbac.authorization.k8s.io/v1/clusterroles/kiali
  uid: cd0de883-b9ff-11e9-bd33-023708277e46
  resourceVersion: '11149746'
  creationTimestamp: '2019-08-08T17:13:11Z'
  labels:
    app: kiali
    chart: kiali
    heritage: Tiller
    release: istio
rules:
  - verbs:
      - get
      - list
      - watch
    apiGroups:
      - ''
    resources:
      - configmaps
      - endpoints
      - namespaces
      - nodes
      - pods
      - services
      - replicationcontrollers
  - verbs:
      - get
      - list
      - watch
    apiGroups:
      - extensions
      - apps
      - apps.openshift.io
    resources:
      - deployments
      - deploymentconfigs
      - statefulsets
      - replicasets
  - verbs:
      - get
      - list
      - watch
    apiGroups:
      - project.openshift.io
    resources:
      - projects
  - verbs:
      - get
      - list
      - watch
    apiGroups:
      - autoscaling
    resources:
      - horizontalpodautoscalers
  - verbs:
      - get
      - list
      - watch
    apiGroups:
      - batch
    resources:
      - cronjobs
      - jobs
  - verbs:
      - create
      - delete
      - get
      - list
      - patch
      - watch
    apiGroups:
      - config.istio.io
    resources:
      - apikeys
      - authorizations
      - checknothings
      - circonuses
      - deniers
      - fluentds
      - handlers
      - kubernetesenvs
      - kuberneteses
      - listcheckers
      - listentries
      - logentries
      - memquotas
      - metrics
      - opas
      - prometheuses
      - quotas
      - quotaspecbindings
      - quotaspecs
      - rbacs
      - reportnothings
      - rules
      - solarwindses
      - stackdrivers
      - statsds
      - stdios
  - verbs:
      - create
      - delete
      - get
      - list
      - patch
      - watch
    apiGroups:
      - networking.istio.io
    resources:
      - destinationrules
      - gateways
      - serviceentries
      - virtualservices
  - verbs:
      - create
      - delete
      - get
      - list
      - patch
      - watch
    apiGroups:
      - authentication.istio.io
    resources:
      - policies
      - meshpolicies
  - verbs:
      - create
      - delete
      - get
      - list
      - patch
      - watch
    apiGroups:
      - rbac.istio.io
    resources:
      - clusterrbacconfigs
      - rbacconfigs
      - serviceroles
      - servicerolebindings
  - verbs:
      - get
    apiGroups:
      - monitoring.kiali.io
    resources:
      - monitoringdashboards
````
* Populate minio with [test data](data) following using the following commands:
````
mc mb minio/data
mc mb minio/models
mc cp /Users/boris/Projects/Recommender/data/users.csv minio/data/recommender/users.csv
mc cp /Users/boris/Projects/Recommender/data/transactions.csv minio/data/recommender/transactions.csv
mc cp /Users/boris/Projects/Recommender/data/directory.txt minio/data/recommender/directory.txt
````
* Starting Jupiter server. Had to do several things:
    * Following [issue](https://github.com/kubeflow/kubeflow/issues/3086) update notebooks-controller-role to include notebooks/finalizers and os adm policy
    * Following [issue](https://github.com/kubeflow/kubeflow/issues/3232) created service account and gave it anyuid role
* Creating TFJob. Several things:
    * Without ksonet, it is necessary to create a [yaml file](tfjob/tf_job_recommender.yaml) for TFJob. Note, that container name has to be tensorflow
    * tf-job-operator has to be added to anyuid
    * Add tfjobs/finalizers to tf-job-operator role
    * TFJob UI is not integrated yet. Go to <Istio Ingress>/tfjobs/ui/
    

* According to [Kubeflow documentation](https://www.kubeflow.org/docs/components/serving/tfserving_new/), Tensorflow serving has not yet been converted to kustomize. So 
we are using a [custom deployment](tfserving/chart) (modeled after deployment in Kubeflow 0.4).
* Argo. Several things: 
    * update argo and argo-ui role to add workflows/finalizers
    * update workflow-controller-configmap to add - containerRuntimeExecutor: k8sapi


## Installation update for version 0.7 on Openshift 4.1

First install Istio following this [documentation](https://docs.openshift.com/container-platform/4.1/service_mesh/service_mesh_arch/understanding-ossm.html)
Next, install KNative following this [documentation](https://docs.openshift.com/container-platform/4.1/serverless/understanding-serverless.html)
Follow installation steps [here](https://www.kubeflow.org/docs/started/k8s/kfctl-k8s-istio/), setting up 
for the [later deployment](https://www.kubeflow.org/docs/started/k8s/kfctl-k8s-istio/#alternatively-set-up-your-configuration-for-later-deployment).
Go to kfctl_k8s_istio.0.7.0.yaml and comment out Istio and KNative installs. Also go to the generated [kustomize files](openshift/kustomize/istio/base/kf-istio-resources.yaml)
and update the last definition of the file to:
```` 
apiVersion: rbac.istio.io/v1alpha1
kind: RbacConfig
metadata:
  name: default
spec:
  mode: $(clusterRbacConfig)
````
FInally run the following commands:
````
oc adm policy add-scc-to-user anyuid -z admission-webhook-service-account -nkubeflow
oc adm policy add-scc-to-user anyuid -z katib-controller -nkubeflow
oc adm policy add-scc-to-user anyuid -z katib-ui -nkubeflow
oc adm policy add-scc-to-user anyuid -z default -nkubeflow
oc adm policy add-scc-to-user anyuid -z ml-pipeline -nkubeflow
oc adm policy add-scc-to-user anyuid -z pipeline-runner -nkubeflow
````
Install Kubeflow using `kfctl` command.
Make sure that in your Istio configuration contains kubeflow namespace in 
`ServiceMeshMemberRoll`
````
apiVersion: maistra.io/v1
kind: ServiceMeshMemberRoll
metadata:
  selfLink: /apis/maistra.io/v1/namespaces/istio-system/servicemeshmemberrolls/default
  resourceVersion: '63163135'
  name: default
  uid: eedb5e27-da19-11e9-aa18-12a7ea357834
  creationTimestamp: '2019-09-18T13:40:52Z'
  generation: 2
  namespace: istio-system
  ownerReferences:
    - apiVersion: maistra.io/v1
      kind: ServiceMeshControlPlane
      name: basic-install
      uid: a2fde1b5-d9a1-11e9-aa18-12a7ea357834
  finalizers:
    - maistra.io/istio-operator
spec:
  members:
    - knative-serving
    - kfserving-system
    - kubeflow
status:
  configuredMembers:
    - knative-serving
    - kubeflow
  meshGeneration: 2
  observedGeneration: 2
````
And a kubeflow-gateway is added to a `ServiceMeshControlPlane` 
````
apiVersion: maistra.io/v1
kind: ServiceMeshControlPlane
metadata:
  creationTimestamp: '2019-09-17T23:19:45Z'
  finalizers:
    - maistra.io/istio-operator
  generation: 2
  name: basic-install
  namespace: istio-system
  resourceVersion: '63163077'
  selfLink: >-
    /apis/maistra.io/v1/namespaces/istio-system/servicemeshcontrolplanes/basic-install
  uid: a2fde1b5-d9a1-11e9-aa18-12a7ea357834
spec:
  istio:
    gateways:
      istio-egressgateway:
        autoscaleEnabled: false
      istio-ingressgateway:
        autoscaleEnabled: false
      kubeflow-gateway:
        autoscaleEnabled: false
....
````
Once this is done you can use Istio Ingress to access Kubeflow.

Notebook image
````
gcr.io/kubeflow-images-public/tensorflow-1.13.1-notebook-cpu:v-base-08f3cbc-1166369568336121856
````
## License

Copyright (C) 2019 Lightbend Inc. (https://www.lightbend.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
