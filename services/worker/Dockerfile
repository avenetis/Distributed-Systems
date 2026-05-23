# Stage 1: Build the JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy entire repo
COPY . .

# Download all dependencies offline
RUN mvn dependency:go-offline -B

# Build only worker module
RUN mvn -pl services/worker package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/services/worker/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]