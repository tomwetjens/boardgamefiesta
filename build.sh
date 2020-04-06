#!/bin/bash

mvn package -Pnative -Dquarkus.native.container-build=true

pushd server
docker build -f src/main/docker/Dockerfile.native -t gwt .
popd
