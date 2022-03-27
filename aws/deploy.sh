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
# Usage:
#./deploy.sh dev
#./deploy.sh prod

set -e

ENV=$1
TIMESTAMP=$(date -u +%FT%T.000Z)

STACK_PREFIX=boardgamefiesta-$ENV
LAMBDA_S3_BUCKET=boardgamefiesta-builds

echo "Deploying $ENV"

aws cloudformation deploy --stack-name $STACK_PREFIX-db \
  --template-file db.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides Environment=$ENV

LAMBDA_COGNITO_VERSION=$(md5sum "../lambda-cognito/target/function.zip" | cut -c-32)
LAMBDA_COGNITO_S3_KEY=lambda-cognito.$LAMBDA_COGNITO_VERSION.zip
aws s3 ls s3://$LAMBDA_S3_BUCKET/$LAMBDA_COGNITO_S3_KEY || aws s3 cp ../lambda-cognito/target/function.zip s3://$LAMBDA_S3_BUCKET/$LAMBDA_COGNITO_S3_KEY

aws cloudformation deploy --stack-name $STACK_PREFIX-auth \
  --template-file auth.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides Environment=$ENV \
    LambdaS3Bucket=$LAMBDA_S3_BUCKET \
    LambdaS3Key=$LAMBDA_COGNITO_S3_KEY \
    DynamoDbStackName=$STACK_PREFIX-db

LAMBDA_WEBSOCKET_VERSION=$(md5sum "../lambda-websocket/target/function.zip" | cut -c-32)
LAMBDA_WEBSOCKET_S3_KEY=lambda-websocket.$LAMBDA_WEBSOCKET_VERSION.zip
aws s3 ls s3://$LAMBDA_S3_BUCKET/$LAMBDA_WEBSOCKET_S3_KEY || aws s3 cp ../lambda-websocket/target/function.zip s3://$LAMBDA_S3_BUCKET/$LAMBDA_WEBSOCKET_S3_KEY

aws cloudformation deploy --stack-name $STACK_PREFIX-ws \
  --template-file ws.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides Environment=$ENV \
    LambdaS3Bucket=$LAMBDA_S3_BUCKET \
    LambdaS3Key=$LAMBDA_WEBSOCKET_S3_KEY \
    DynamoDbStackName=$STACK_PREFIX-db \
    CognitoStackName=$STACK_PREFIX-auth

LAMBDA_AUTOMA_VERSION=$(md5sum "../lambda-automa/target/function.zip" | cut -c-32)
LAMBDA_AUTOMA_S3_KEY=lambda-automa.$LAMBDA_AUTOMA_VERSION.zip
aws s3 ls s3://$LAMBDA_S3_BUCKET/$LAMBDA_AUTOMA_S3_KEY || aws s3 cp ../lambda-automa/target/function.zip s3://$LAMBDA_S3_BUCKET/$LAMBDA_AUTOMA_S3_KEY

aws cloudformation deploy --stack-name $STACK_PREFIX-automa \
  --template-file automa.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides Environment=$ENV \
    LambdaS3Bucket=$LAMBDA_S3_BUCKET \
    LambdaS3Key=$LAMBDA_AUTOMA_S3_KEY \
    DynamoDbStackName=$STACK_PREFIX-db \
    WebSocketStackName=$STACK_PREFIX-ws

LAMBDA_REST_VERSION=$(md5sum "../lambda-rest/target/function.zip" | cut -c-32)
LAMBDA_REST_S3_KEY=lambda-rest.$LAMBDA_REST_VERSION.zip
aws s3 ls s3://$LAMBDA_S3_BUCKET/$LAMBDA_REST_S3_KEY || aws s3 cp ../lambda-rest/target/function.zip s3://$LAMBDA_S3_BUCKET/$LAMBDA_REST_S3_KEY

aws cloudformation deploy --stack-name $STACK_PREFIX-apigw \
  --template-file apigw.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides Environment=$ENV \
    LambdaS3Bucket=$LAMBDA_S3_BUCKET \
    LambdaS3Key=$LAMBDA_REST_S3_KEY \
    DynamoDbStackName=$STACK_PREFIX-db \
    CognitoStackName=$STACK_PREFIX-auth \
    AutomaStackName=$STACK_PREFIX-automa \
    WebSocketStackName=$STACK_PREFIX-ws