# Board Game Fiesta

# Build
```
./mvnw package
```

# Run locally
```
./mvnw compile 
./mvnw quarkus:dev -pl server
```

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