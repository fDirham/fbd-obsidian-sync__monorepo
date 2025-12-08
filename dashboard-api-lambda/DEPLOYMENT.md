# Deployment Guide

## Running Locally

### Option 1: Using Gradle Wrapper (Recommended)
```bash
./gradlew bootRun
```

### Option 2: Build and Run JAR
```bash
# Build the JAR
./gradlew build

# Run the JAR
java -jar build/libs/fbd-obsidian-sync-dashboard-api-0.0.1-SNAPSHOT.jar
```

## Testing the Endpoint

Once running, test the endpoint:
```bash
curl http://localhost:8080/hello
```

Expected response: `Hello World!`

## Deployment Options

### 1. Docker Deployment

Create `Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
./gradlew build
docker build -t fbd-api .
docker run -p 8080:8080 fbd-api
```

### 2. AWS Elastic Beanstalk
```bash
# Build JAR
./gradlew build

# Deploy (requires AWS CLI and EB CLI)
eb init -p corretto-21 fbd-api
eb create fbd-api-env
eb deploy
```

### 3. AWS ECS/Fargate
- Build Docker image
- Push to Amazon ECR
- Create ECS task definition
- Deploy to Fargate cluster

### 4. Traditional Server
```bash
# Build JAR
./gradlew build

# Copy JAR to server
scp build/libs/*.jar user@server:/opt/app/

# Run as systemd service
sudo systemctl start fbd-api
```

## Configuration

Set server port (optional) in `application.properties`:
```properties
server.port=8080
```
