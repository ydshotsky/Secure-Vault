# Step 1: Use official Eclipse Temurin JDK 21 (LTS) base image
FROM eclipse-temurin:21-jdk AS build

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

# Build the application JAR file (skip tests for faster build)
RUN ./mvnw clean package -DskipTests

# Step 2: Create lightweight runtime image
FROM eclipse-temurin:21-jre

# Set working directory inside container
WORKDIR /SecureVault

# Copy JAR from build stage
COPY --from=build /SecureVault/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=production


# Expose port (change if your app uses a different port)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
