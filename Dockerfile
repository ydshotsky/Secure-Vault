# Step 1: Build stage with GraalVM JDK 21 for native image compilation
FROM ghcr.io/graalvm/jdk:21 AS build

# Set working directory inside container
WORKDIR /SecureVault

# Copy Maven wrapper and project files (for caching dependencies)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy the rest of the application source
COPY src src

# Build the application JAR file with native image support (skip tests for faster build)
RUN ./mvnw clean package -DskipTests -Pnative

# Step 2: Create lightweight runtime image from distroless
FROM debian:12-slim

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory inside container
WORKDIR /SecureVault

# Copy native executable from build stage
COPY --from=build /SecureVault/target/SecureVault /app/securevault

# Change ownership to non-root user
RUN chown -R appuser:appuser /SecureVault

# Set environment variables for production
ENV SPRING_PROFILES_ACTIVE=production
ENV PORT=8080

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Note: Health checks should be configured at the Azure level
# Azure App Service, Container Instances, or Kubernetes will probe:
# GET http://localhost:8080/actuator/health

# Run the native executable (no JVM overhead!)
ENTRYPOINT ["/app/securevault"]
