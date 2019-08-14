#Kafka
The sample relies on Kafka. Installation is done using [strimzi](https://strimzi.io/) Kafka operator. To install,
follow instructions [here](https://developer.lightbend.com/docs/fast-data-platform/current-OpenShift/#strimzi-operator-kafka) 
  
To create a simple clister, run the following:
````
oc apply -f kafka/kafka.yaml -n kubeflow
````
where kafka.yaml is [here](kafka.yaml)