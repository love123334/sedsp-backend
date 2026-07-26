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

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
EXPOSE 8080

# Liveness only — full /actuator/health is 503 while DB is DOWN (Railway would fail deploy).
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=8 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/actuator/health/liveness" || exit 1

ENTRYPOINT ["sh", "-c", "echo \"[boot] PORT=${PORT:-8080} PROFILE=${SPRING_PROFILES_ACTIVE:-}\"; echo \"[boot] DB keys:\"; env | awk -F= '/^(DATABASE|POSTGRES|PG|REDIS|SPRING_DATASOURCE|SPRING_PROFILES)/ {print $1}'; exec java ${JAVA_OPTS:-} -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
