#!/bin/bash

# Fail on first error
set -e

mvn package

pushd server
docker build -f src/main/docker/Dockerfile.jvm -t gwt .
popd
