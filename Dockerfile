# ==========================================
# Stage 1: Build & Package (Maven + JDK 21)
# ==========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Pre-fetch dependencies to leverage Docker layer caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy application source code and compile optimized production JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Production Runtime (JRE 21 Alpine)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security best practice: Create non-root application user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy packaged JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Enforce secure file ownership
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

# Production memory management flags tuned for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
