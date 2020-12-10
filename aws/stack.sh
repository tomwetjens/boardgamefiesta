#!/bin/bash

#./stack.sh create dev 294
#./stack.sh create prod 294
#./stack.sh update dev 294
#./stack.sh update prod 294

ACTION=$1
ENV=$2
VERSION=$3

STACK_NAME=boardgamefiesta-$ENV

echo "${ACTION} stack $STACK_NAME"

aws cloudformation $ACTION-stack --stack-name $STACK_NAME-vpc \
  --template-body file://vpc.yaml

#aws cloudformation $ACTION-stack --stack-name $STACK_NAME-db \
#  --template-body file://db.yaml \
#  --parameters ParameterKey=Suffix,ParameterValue=$SUFFIX

aws cloudformation $ACTION-stack --stack-name $STACK_NAME-auth \
  --template-body file://auth.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters ParameterKey=Environment,ParameterValue=$ENV

aws cloudformation $ACTION-stack --stack-name $STACK_NAME-api \
  --template-body file://api.yaml \
  --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
  --parameters ParameterKey=Environment,ParameterValue=$ENV ParameterKey=Version,ParameterValue=$VERSION