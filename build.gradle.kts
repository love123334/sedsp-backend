plugins {
    java
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

// Railway Postgres is often 18.x — Boot 3.5.14 manages Flyway 11.7.2 (PG≤17 warning).
// Pin a newer Flyway that recognizes PostgreSQL 18 cleanly.
extra["flyway.version"] = "11.14.1"

// Version management
val mapstructVersion = "1.6.3"
val lombokMapstructBindingVersion = "0.2.0"
val jjwtVersion = "0.12.5"

dependencies {
    // --- OpenAI SDK ---
    implementation("com.google.genai:google-genai:0.3.0")

    // --- Spring Boot Starters ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // --- Database & Migration ---
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // --- Security & JWT ---
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // --- Utilities & Documentation ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.16")
    implementation("com.github.slugify:slugify:3.0.4")
    implementation("com.cloudinary:cloudinary-http44:1.39.0") // Thư viện Upload ảnh sản phẩm
    implementation("com.microsoft.onnxruntime:onnxruntime:1.23.2")

    // --- Lombok & MapStruct ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")

    // --- Testing ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("-Duser.timezone=Asia/Ho_Chi_Minh")
}

// Only the Boot fat JAR (avoids cp *.jar picking *-plain.jar in Docker)
tasks.named<Jar>("jar") {
    enabled = false
}
