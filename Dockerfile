FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build

COPY . .

RUN ./gradlew bootJar -x test

FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]