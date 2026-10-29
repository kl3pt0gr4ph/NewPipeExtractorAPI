# Build stage
FROM maven:3.9-eclipse-temurin-25-alpine AS builder
WORKDIR /build

COPY app/pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY app/src ./src
RUN mvn -B -ntp package -DskipTests \
 && cp target/*.jar app.jar

# Split jar into Spring Boot's layers
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted \
 && mkdir -p extracted/dependencies extracted/spring-boot-loader \
             extracted/snapshot-dependencies extracted/application

# Runtime
FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S -g 1001 app \
    && adduser -S -D -H -u 1001 -G app -s /sbin/nologin app

WORKDIR /app

COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

USER 1001:1001

ENV SERVER_PORT=8080

#   MaxRAMPercentage      heap = 75% of the container limit
#   UseG1GC               pinned: below 2 CPUs or ~1.75 GB the JVM picks Serial
#   ExitOnOutOfMemoryError  crash on OOM so the restart policy recovers it
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
