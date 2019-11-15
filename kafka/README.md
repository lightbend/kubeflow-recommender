#Kafka
The sample relies on Kafka. Installation is done using [strimzi](https://strimzi.io/) Kafka operator. To install,
follow instructions [here](https://developer.lightbend.com/docs/fast-data-platform/current-OpenShift/#strimzi-operator-kafka) 
````
helm install strimzi/strimzi-kafka-operator --name my-strimzi --namespace strimzi --version 0.14.0 --debug
````  
To create a simple clister, run the following:
````
oc apply -f kafka/kafka.yaml -n kubeflow
````
where kafka.yaml is [here](kafka.yaml)