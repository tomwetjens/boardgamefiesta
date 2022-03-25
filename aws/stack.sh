#!/bin/bash

#
# Board Game Fiesta
# Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

#./stack.sh create dev 294
#./stack.sh create prod 294
#./stack.sh update dev 294
#./stack.sh update prod 294

ACTION=$1-stack
ENV=$2
VERSION=$3
TIMESTAMP=$(date -u +%FT%T.000Z)

STACK_NAME=boardgamefiesta-$ENV

echo "${ACTION}: $STACK_NAME"

#aws cloudformation $ACTION --stack-name $STACK_NAME-vpc \
#  --template-body file://vpc.yaml
#
#aws cloudformation $ACTION --stack-name $STACK_NAME-db \
#  --template-body file://db.yaml \
#  --parameters ParameterKey=Environment,ParameterValue=$ENV

#aws s3 cp ../lambda-cognito/target/function.zip s3://boardgamefiesta-builds/$TIMESTAMP/lambda-cognito.zip
#
#aws cloudformation $ACTION --stack-name $STACK_NAME-auth \
#  --template-body file://auth.yaml \
#  --capabilities CAPABILITY_NAMED_IAM \
#  --parameters ParameterKey=Environment,ParameterValue=$ENV ParameterKey=Version,ParameterValue=$TIMESTAMP

#aws s3 cp ../lambda-websocket/target/function.zip s3://boardgamefiesta-builds/$TIMESTAMP/lambda-websocket.zip
#
#aws cloudformation $ACTION --stack-name $STACK_NAME-ws \
#  --template-body file://ws.yaml \
#  --capabilities CAPABILITY_NAMED_IAM \
#  --parameters ParameterKey=Environment,ParameterValue=$ENV \
#    ParameterKey=Version,ParameterValue=$TIMESTAMP \
#    ParameterKey=DbStackName,ParameterValue=$STACK_NAME-db

#aws s3 cp ../lambda-automa/target/function.zip s3://boardgamefiesta-builds/$TIMESTAMP/lambda-automa.zip
#
#aws cloudformation $ACTION --stack-name $STACK_NAME-automa \
#  --template-body file://automa.yaml \
#  --capabilities CAPABILITY_NAMED_IAM \
#  --parameters ParameterKey=Environment,ParameterValue=$ENV \
#    ParameterKey=Version,ParameterValue=$TIMESTAMP \
#    ParameterKey=DbStackName,ParameterValue=$STACK_NAME-db \
#    ParameterKey=WebSocketStackName,ParameterValue=$STACK_NAME-ws

aws s3 cp ../lambda-rest/target/function.zip s3://boardgamefiesta-builds/$TIMESTAMP/lambda-rest.zip

aws cloudformation $ACTION --stack-name $STACK_NAME-apigw \
  --template-body file://apigw.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters ParameterKey=Environment,ParameterValue=$ENV \
    ParameterKey=Version,ParameterValue=$TIMESTAMP \
    ParameterKey=DbStackName,ParameterValue=$STACK_NAME-db \
    ParameterKey=AutomaStackName,ParameterValue=$STACK_NAME-automa
    ParameterKey=WebSocketStackName,ParameterValue=$STACK_NAME-ws

#aws cloudformation $ACTION --stack-name $STACK_NAME-api \
#  --template-body file://api.yaml \
#  --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
#  --parameters ParameterKey=Environment,ParameterValue=$ENV ParameterKey=Version,ParameterValue=$VERSION