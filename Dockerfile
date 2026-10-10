# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and build package
COPY src ./src
RUN mvn clean package -DskipTests -B

# Production runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for container health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Run as non-privileged user for security
RUN groupadd -r ledgergroup && useradd -r -g ledgergroup ledgeruser
USER ledgeruser

# Copy artifact from build stage
COPY --from=build /workspace/target/*.jar app.jar

# Dynamic PORT support for Render and cloud environments
ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${PORT}/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT} -jar app.jar"]
