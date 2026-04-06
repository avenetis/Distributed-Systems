# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy entire repo so parent POM is present
COPY ../../ ./

# Download dependencies for all modules
RUN mvn dependency:go-offline -B

# Build only ui-service module
RUN mvn -pl services/ui-service package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/ui-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
