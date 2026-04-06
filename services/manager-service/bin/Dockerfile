# Stage 1: Build the JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy entire repo
COPY . .

# Download all dependencies offline
RUN mvn dependency:go-offline -B

# Build only manager-service module
RUN mvn -pl services/manager-service package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/manager-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]