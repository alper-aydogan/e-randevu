# Multi-stage build with Eclipse Temurin for Apple Silicon
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy all Maven wrapper files
COPY .mvn .mvn

# Copy project files
COPY mvnw pom.xml ./

# Make mvnw executable
RUN chmod +x mvnw

# Copy source code
COPY src ./src

# Build application (using container's Java)
RUN ./mvnw clean package -DskipTests -B -Dmaven.compiler.fork=false

# Runtime stage - JRE only for smaller image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy built jar from build stage
COPY --from=build /app/target/e-randevu-*.jar app.jar

# Create non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Expose port 8081
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
