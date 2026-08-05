FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build

# Copy the Gradle wrapper first so dependency downloads can be cached.
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Git executable permissions are not preserved reliably on Windows.
# Grant the permission explicitly so Linux builders (including Railway) can run it.
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
