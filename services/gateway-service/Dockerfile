# Multi-stage Dockerfile for gateway-service
# Stage 1: Build the application
FROM gradle:8.5.0-jdk17 AS builder
WORKDIR /app

# Copy the gateway-service project
COPY gateway-service ./

# Copy the shared gradle folder
COPY gradle/libs.versions.toml ./gradle/libs.versions.toml

# Build the Spring Boot jar
# The build process itself is typically the same for all environments
RUN gradle bootJar --no-daemon

# ----------------------------------------------------------------

# Stage 2: Create the final production-ready runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Set the environment variable to activate the 'prod' profile
ENV SPRING_PROFILES_ACTIVE=prod

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set the entrypoint to run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]