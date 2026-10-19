# Build stage
FROM maven:3.9-eclipse-temurin-26-alpine AS builder

WORKDIR /build

COPY app/pom.xml ./
RUN mvn dependency:go-offline -B

COPY app/src ./src
RUN mvn clean package -DskipTests -B

RUN echo "=== Available JAR files ===" && \
    ls -la target/*.jar

# Production stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser

COPY --from=builder /build/target/*.jar app.jar

RUN mkdir -p /app/data && chown -R appuser:appuser /app

USER appuser

EXPOSE 5050

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5050/actuator/health || exit 1

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
