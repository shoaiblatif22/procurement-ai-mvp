# syntax=docker/dockerfile:1.6

# ── Build stage ───────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Resolve dependencies first so this layer caches across source changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B dependency:go-offline

# Build the fat jar and extract Spring Boot layers (with launcher classes)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B clean package -DskipTests && \
    mv target/*.jar application.jar && \
    java -Djarmode=tools -jar application.jar extract --layers --launcher

# ── Runtime stage ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy layers least-changing → most-changing so a code edit only rebusts the last layer
COPY --from=builder /build/application/dependencies/          ./
COPY --from=builder /build/application/spring-boot-loader/    ./
COPY --from=builder /build/application/snapshot-dependencies/ ./
COPY --from=builder /build/application/application/           ./

# Uploads directory (overridden by S3 in production)
RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "org.springframework.boot.loader.launch.JarLauncher"]
