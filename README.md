# Smart E-Commerce Decision Support Platform (SEDSP) - Backend

## 📌 Overview

Smart E-Commerce Decision Support Platform (SEDSP) is an intelligent e-commerce platform integrated with Decision Support System (DSS) functionalities.

The system not only supports traditional e-commerce operations but also provides analytical insights and intelligent decision-making support for businesses.

This project is developed as a Capstone Project for the Information Systems major at FPT University.

---

# 🏗️ System Architecture

```text
Backend    : Spring Boot
Database   : PostgreSQL
Cache      : Redis
Automation : n8n
Analytics  : Power BI
```

---

# 🚀 Technologies Used

## Backend
- Java 17
- Spring Boot 3.5.14
- Spring Security (JWT Authentication)
- Spring Data JPA
- Hibernate

## Database & Caching
- PostgreSQL
- Redis

## Tools & DevOps
- Flyway Migration
- Swagger / OpenAPI
- Docker & Docker Compose
- Gradle

---

# 📂 Project Structure

```text
src/main/java/com/example
├── config          # Configuration classes
├── controller      # REST API controllers
├── service         # Business logic layer
├── repository      # Data access layer
├── entity          # JPA entities
├── dto             # Data Transfer Objects
├── security        # JWT & Spring Security
└── util            # Utility classes
```

---

# 🔐 System Roles

| Role | Description |
|------|-------------|
| ADMIN | Full system management |
| MANAGER | Business monitoring & analytics |
| SELLER | Product and order management |
| CUSTOMER | Shopping and purchasing |

---

# 📊 Decision Support Features

- 📈 Sales Analytics
- 💰 Revenue Growth Analysis
- 🏷️ Competitor Price Tracking
- 📦 Inventory Recommendation
- 🔍 What-if Analysis

---

# ⚙️ Getting Started

## 1️⃣ Clone Repository

```bash
git clone https://github.com/linhtnt2004/smart-ecommerce-dssp.git
cd sedsp-backend
```

---

## 2️⃣ Configure Database

Update `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sedsp
    username: postgres
    password: your_password
```

---

## 3️⃣ Run Application

### Linux / macOS

```bash
./gradlew bootRun
```

### Windows

```bash
gradlew.bat bootRun
```

---

# 🐳 Docker Support

```bash
docker-compose up --build
```

> Docker deployment is currently under development.

---

# 📖 API Documentation

After starting the application, access Swagger UI at:

```text
http://localhost:8080/swagger-ui.html
```

---

# 🔒 Security

The system implements:

- JWT-based Authentication
- Role-based Access Control (RBAC)
- Secure REST APIs with Spring Security

---

# 📈 Future Improvements

- 🤖 AI-based Demand Prediction
- 📊 Advanced What-if Analysis
- 🧩 Microservices Architecture
- 🔄 CI/CD Pipeline
- ☁️ Cloud Deployment
- 📱 Mobile Application Support

---

# 👨‍🎓 Capstone Project Information

| Item | Details |
|------|---------|
| University | FPT University |
| Major | Information Systems |
| Duration | 05/2026 – 08/2026 |
| Supervisor | Mr. Trần Thanh Nguyên |

---

# 🤝 Contributors

Developed by the SEDSP Capstone Team.

---

# 📜 License

This project is developed for academic and educational purposes.