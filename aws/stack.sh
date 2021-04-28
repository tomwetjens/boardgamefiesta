#!/bin/bash

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

#aws cloudformation $ACTION --stack-name $STACK_NAME-db \
#  --template-body file://db.yaml \
#  --parameters ParameterKey=Suffix,ParameterValue=$SUFFIX

aws s3 cp ../dynamodb-triggers/target/function.zip s3://boardgamefiesta-builds/$TIMESTAMP/dynamodb-triggers.zip

aws cloudformation $ACTION --stack-name $STACK_NAME-triggers \
  --template-body file://triggers.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters ParameterKey=Environment,ParameterValue=$ENV ParameterKey=Version,ParameterValue=$TIMESTAMP

aws s3 cp ../cognito/target/function.zip s3://boardgamefiesta-builds/$TIMESTAMP/cognito.zip

aws cloudformation $ACTION --stack-name $STACK_NAME-auth \
  --template-body file://auth.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters ParameterKey=Environment,ParameterValue=$ENV ParameterKey=Version,ParameterValue=$TIMESTAMP

#aws cloudformation $ACTION --stack-name $STACK_NAME-api \
#  --template-body file://api.yaml \
#  --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
#  --parameters ParameterKey=Environment,ParameterValue=$ENV ParameterKey=Version,ParameterValue=$VERSION