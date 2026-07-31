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
src/main/java/com/example/secdsp
│
├── common/           # Shared utilities, exceptions, responses
├── config/           # Spring configuration classes
├── security/         # JWT & Spring Security
├── infrastructure/   # External integrations (Redis, AI, etc.)
├── modules/          # Business modules
│   ├── auth/
│   ├── user/
│   ├── product/
│   ├── inventory/
│   ├── order/
│   └── analytics/
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

> Docker Compose is provided for local development environments.

---

# 📖 API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

---

# 🔒 Security

The system implements:

- JWT-based Authentication
- Role-based Access Control (RBAC)
- Secure REST APIs with Spring Security
- Authentication flow:
- JWT Access Token
- Role-based authorization
- Stateless REST API security

---

# 🧱 Architecture Principles

The backend follows a modular layered architecture:

- Feature-based modular structure
- Separation of concerns
- DTO-based API communication
- RESTful API design
- Flyway database migration strategy
- JWT authentication & RBAC authorization
- Clean and maintainable code organization

---

# 🛠️ Development Rules

- Never modify existing Flyway migration files
- Use DTOs for all API requests/responses
- Keep controllers thin
- Business logic belongs in services
- Use constructor injection
- Follow RESTful naming conventions

---

# 🤖 AI chatbot (OpenRouter / OpenAI-compatible)

Token chỉ đặt trên **backend**. Frontend gọi `POST /api/v1/ai/chat`.

## Env backend (Railway / local)

```bash
AI_ENABLED=true
OPENROUTER_API_KEY=sk-or-v1-...
# optional:
# AI_API_BASE_URL=https://openrouter.ai/api/v1
# AI_MODEL=openrouter/free          # free router; hoặc openai/gpt-4o-mini khi có credit
```

Local: copy key vào `src/main/resources/application-local.yml` (file gitignored).

## Power BI

```bash
POWERBI_EMBED_URL=https://app.powerbi.com/view?r=...   # optional iframe
POWERBI_REPORT_TITLE=SEDSP Decision Dashboard
```

## API

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/api/v1/ai/status` | AI đã cấu hình? |
| POST | `/api/v1/ai/chat` | Chat proxy (JWT) |
| GET | `/api/v1/dss/demand/{productId}` | Moving average demand |
| GET | `/api/v1/dss/price/{productId}` | Price recommendation |
| GET | `/api/v1/dss/inventory` | ROP inventory |
| GET | `/api/v1/dss/insights/plan` | Metrics + AI commentary + embed URL |
| GET | `/api/v1/analytics/powerbi/sales` | Flat sales feed cho Power BI Web |

## Power BI Desktop

1. Get Data → Web → `https://YOUR-API/api/v1/analytics/powerbi/sales`
2. Header: `Authorization: Bearer <JWT seller/manager>`
3. Build report → Publish → copy Embed URL → set `POWERBI_EMBED_URL`
4. FE hub DSS hiển thị nhận xét (AI hoặc rule-based) + iframe nếu có embed

Không cần Power BI Premium để chạy path này.

---

# 📈 Future Improvements

- Advanced What-if Analysis
- Microservices Architecture
- CI/CD Pipeline
- Cloud Deployment
- Mobile Application Support

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