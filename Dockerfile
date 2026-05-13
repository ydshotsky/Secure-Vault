# Production Dockerfile - GraalVM Native Image Build
# Optimized for Azure Container deployment
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /SecureVault

# Install required build tools and locale support in one layer
RUN apt-get update && apt-get install -y --no-install-recommends \
    locales \
    ca-certificates \
    git \
    build-essential \
    pkg-config \
    zlib1g-dev \
    libssl-dev \
    && sed -i 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
    && locale-gen en_US.UTF-8 \
    && update-locale LANG=en_US.UTF-8 \
    && rm -rf /var/lib/apt/lists/*

# Set locale environment variables
ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy application source
COPY src src

# Build native executable with GraalVM native-image
# The native-maven-plugin will download and use GraalVM's native-image tool
RUN ./mvnw clean package -DskipTests -Pnative

# Runtime stage - lightweight container
FROM debian:12-slim

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy the native executable from build stage
COPY --from=0 /SecureVault/target/SecureVault /app/securevault

# Fix permissions
RUN chown -R appuser:appuser /app

# Environment for production
ENV SPRING_PROFILES_ACTIVE=production \
    PORT=8080

# Switch to non-root user
USER appuser

EXPOSE 8080

# Note: Health checks should be configured at the Azure level
# Azure App Service, Container Instances, or Kubernetes will probe:
# GET http://localhost:8080/actuator/health

# Run the native executable
ENTRYPOINT ["/app/securevault"]

