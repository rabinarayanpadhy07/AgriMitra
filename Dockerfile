# ==============================================================================
# Multi-Stage Dockerfile for AgriMitra Backend (Java 21 + Spring Boot 3.3)
# ==============================================================================

# Stage 1: Build the application using Maven and Temurin JDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight JRE 21 runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S agrimitra && adduser -S agrimitra -G agrimitra
USER agrimitra

# Copy the built jar from the builder stage
COPY --from=builder /build/target/shopeasy-auth-service-1.0.0.jar /app/app.jar

# Render assigns PORT dynamically; default to 8080 if not set
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
