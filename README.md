# Board Game Fiesta

# Build
```
./mvnw package
```

# Run locally
```
./mvnw install 
```
Then start the server for local development:
```
./mvnw quarkus:dev -pl server -am
```
Note: currently this needs some resources to be created on AWS. Easiest way is to deploy the `dev` stack on AWS and point the local server to it by editing the `application.properties`.

The `server` module only exists for local development. On AWS all requests are handled by the `lambda-http` and `lambda-websocket` modules deployed as AWS Lambdas.

# Deploy to AWS
After you have run `mvn package`:
```
cd aws
./deploy.sh dev
./deploy.sh prod
```

# Documentation
```
./mvnw generate-resources
```
Open `target/generated-docs/index.html` in a browser.