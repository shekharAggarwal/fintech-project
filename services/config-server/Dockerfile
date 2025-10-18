FROM gradle:8.5.0-jdk17 AS builder
WORKDIR /app

# Copy the config-server project (including the updated settings.gradle)
COPY config-server ./

# Copy the shared gradle folder to the conventional location
# This is what Gradle's automatic detection will find.
COPY gradle/libs.versions.toml ./gradle/libs.versions.toml

# Build the Spring Boot jar
RUN gradle bootJar --no-daemon

# Final lightweight runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]