# Railway / production — Spring Boot JAR
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version

COPY . .
RUN chmod +x gradlew \
  && ./gradlew bootJar -x test --no-daemon \
  && JAR="$(ls -1 build/libs/*.jar | grep -v plain | head -n 1)" \
  && cp "$JAR" /build/app.jar \
  && echo "Packed $JAR -> /build/app.jar"

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/app.jar /app/app.jar
COPY start.sh /app/start.sh
RUN sed -i 's/\r$//' /app/start.sh && chmod +x /app/start.sh

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=10 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/actuator/health/liveness" || curl -fsS "http://127.0.0.1:${PORT}/healthz" || exit 1

ENTRYPOINT ["/app/start.sh"]
