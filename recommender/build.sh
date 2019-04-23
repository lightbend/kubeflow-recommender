#!/bin/bash

img='lightbend/ml-tf-recommender'
tag='0.0.1'
docker build -t $img:$tag recommender

