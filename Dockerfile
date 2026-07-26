# Multi-stage build for Render / Docker
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version

COPY . .
RUN chmod +x gradlew \
  && ./gradlew bootJar -x test --no-daemon \
  && cp build/libs/*.jar /build/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/app.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh && chmod +x /app/docker-entrypoint.sh

ENV SPRING_PROFILES_ACTIVE=render
ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/actuator/health" || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
