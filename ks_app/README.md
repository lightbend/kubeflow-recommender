#KSONNET
Here are ksonnet files for our solution. The following are parameters used
by our solution. The relevant files here are:
* [recommendermljob](components/recommendermljob.jsonnet) - tfJob definition for machine learning
* [recommender](components/recommender.jsonnet) and [recommender1](components/recommender1.jsonnet) are tensorflow serving
* [recommender-service](components/recommender-service.jsonnet) and [recommender1-service](components/recommender1-service.jsonnet) are tensorflow serving services
* [params](components/params.libsonnet) - parameters used by our services
