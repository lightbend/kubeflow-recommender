local env = std.extVar("__ksonnet/environments");
local params = std.extVar("__ksonnet/params").components.recommendermljob;

local k = import "k.libsonnet";

local name = params.name;
local namespace = env.namespace;
local image = "gcr.io/kubeflow/tf-benchmarks-cpu:v20171202-bdab599-dirty-284af3";

local tfjob = {
  apiVersion: "kubeflow.org/v1beta1",
  kind: "TFJob",
  metadata: {
    name: name,
    namespace: namespace,
  },
  spec: {
    tfReplicaSpecs: {
      Master: {
        replicas: 1,
        template: {
          spec: {
            containers: [
              {
                env: [
                  {
                    name: "MINIO_URL",
                    value: "minio-service:9000",
                  },
                  {
                    name: "MINIO_KEY",
                    value: "minio",
                  },
                  {
                    name: "MINIO_SECRET",
                    value: "minio123",
                  },
                ],
                image: params.image,
                imagePullPolicy: "Always",
                name: "tensorflow",
              },
            ],
            restartPolicy: "OnFailure",
          },  // spec
        },  // template
      },  // master
    },  // tfReplicaSpecs
  },  // spec
};

k.core.v1.list.new([
  tfjob,
])
