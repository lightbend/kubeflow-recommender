FROM  tensorflow/tensorflow:2.0.0a0-py3
RUN pip3 install pandas
RUN pip3 install keras
RUN pip3 install minio
RUN mkdir -p /opt/kubeflow
COPY Recommender_Kubeflow.py /opt/kubeflow/
ENTRYPOINT ["python3", "/opt/kubeflow/Recommender_Kubeflow.py"]