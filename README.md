# Board Game Fiesta

# Build
```
./mvnw package
```

# Run locally
```
./mvnw install 
./mvnw quarkus:dev -pl !lambda*
```
Note: currently this needs some resources to be created on AWS. Easiest way is to deploy the `dev` stack on AWS and point the local server to it by editing the `application.properties`.

# Deploy to AWS
After you have ran `mvn package`:
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