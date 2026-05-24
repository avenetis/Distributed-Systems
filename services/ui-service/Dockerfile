# Stage 1: Build the JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy entire repo
COPY . .

# Download dependencies
RUN mvn dependency:go-offline -B

# Build only ui-service module
RUN mvn -pl services/ui-service package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/services/ui-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]