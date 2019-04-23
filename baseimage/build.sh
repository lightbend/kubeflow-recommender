#!/bin/bash

img='lightbend/java-bash-base'
tag='0.0.1'
docker build -t $img:$tag baseimage

