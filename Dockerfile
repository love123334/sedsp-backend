# Railway / production — Spring Boot JAR
# Builder dùng image Gradle có sẵn — tránh tải gradle-8.14.4-bin.zip lúc build (Railway hay lỗi mạng).
FROM gradle:8.14.4-jdk17 AS builder
WORKDIR /build

ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dorg.gradle.daemon=false"

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY . .
RUN mkdir -p /build/models/demand

RUN chmod +x gradlew \
  && gradle bootJar -x test --no-daemon --stacktrace \
  && JAR="$(ls -1 build/libs/*.jar | grep -v plain | head -n 1)" \
  && test -n "$JAR" \
  && cp "$JAR" /build/app.jar \
  && echo "Packed $JAR -> /build/app.jar"

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/app.jar /app/app.jar
COPY --from=builder /build/models /app/models
COPY start.sh /app/start.sh
RUN sed -i 's/\r$//' /app/start.sh && chmod +x /app/start.sh

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=10 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/actuator/health/liveness" || curl -fsS "http://127.0.0.1:${PORT}/healthz" || exit 1

ENTRYPOINT ["/app/start.sh"]
