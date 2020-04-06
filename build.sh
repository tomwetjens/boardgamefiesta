#!/bin/bash

mvn package

pushd server
docker build -f src/main/docker/Dockerfile.jvm -t gwt .
popd
