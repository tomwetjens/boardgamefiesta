#!/bin/sh

set -e

java -cp target/function.zip com.boardgamefiesta.dynamodb.triggers.ClearTable -- boardgamefiesta-dev
