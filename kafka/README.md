#Kafka
The sample relies on Kafka. Installation is done using [strimzi](https://strimzi.io/) Kafka operator. To install Strimzi run:
````
curl -L https://github.com/strimzi/strimzi-kafka-operator/releases/download/0.9.0/strimzi-cluster-operator-0.9.0.yaml | \
sed "s/myproject/kubeflow/" | oc apply -f - -n kubeflow
````  
To create a simple clister, run the following:
````
oc apply -f kafka/kafka.yaml -n kubeflow
````
where kafka.yaml is [here](kafka.yaml)