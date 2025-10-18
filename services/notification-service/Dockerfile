# Multi-stage Dockerfile for notification-service
# Stage 1: Build the application
FROM gradle:8.5.0-jdk17 AS builder
WORKDIR /app

# Copy the actual source code for ALL modules
#COPY module/security ./module/security
COPY services/notification-service ./notification-service

COPY services/gradle/libs.versions.toml ./gradle/libs.versions.toml


WORKDIR /app/notification-service
# Now run the build from the root, letting Gradle handle the modules
RUN gradle bootJar --no-daemon

# ----------------------------------------------------------------

# Stage 2: Create the final production-ready runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app/notification-service

# Set the environment variable to activate the 'prod' profile
ENV SPRING_PROFILES_ACTIVE=prod

# Copy the built jar from the builder stage
COPY --from=builder /app/notification-service/build/libs/*.jar app.jar

# Set the entrypoint to run the application
ENTRYPOINT ["java","-jar","/app/notification-service/app.jar"]