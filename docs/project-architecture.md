# SEDSP Backend Architecture

## Tech Stack

- Spring Boot
- PostgreSQL
- Flyway
- Redis
- Docker
- JPA/Hibernate

---

# Project Structure

```txt
src/main/java/com/example/secdsp
│
├── common/
├── config/
├── security/
├── infrastructure/
├── modules/
```

---

# Architecture Rules

## 1. Modular Structure

Each business feature must be organized into its own module.

Example:

```txt
modules/
├── auth/
├── user/
├── product/
├── inventory/
├── order/
```

---

## 2. Layered Architecture

Each module should contain:

```txt
product/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── entity/
├── repository/
├── service/
├── mapper/
```

---

# Coding Rules

## Controllers

Controllers should:
- Handle HTTP requests/responses only
- Never contain business logic
- Return standardized API responses

---

## Services

Services should:
- Contain business logic
- Handle transactions
- Validate business rules

---

## Repositories

Repositories should:
- Only access database
- Never contain business logic

---

# DTO Rules

- Never expose entities directly
- Use request DTOs for input
- Use response DTOs for output
- Validate DTOs using Jakarta Validation

Example:

```java
@NotBlank
private String productName;
```

---

# API Response Format

All APIs should follow this structure:

```json
{
  "success": true,
  "message": "Request successful",
  "data": {}
}
```

---

# Database Rules

## Flyway

- Never modify existing migration files
- Always create new migration versions

Example:

```txt
V1__core_schema.sql
V2__create_users.sql
V3__create_products.sql
```

---

# Naming Conventions

## Classes

- PascalCase
- Example:
    - ProductService
    - OrderController

## Variables

- camelCase

## Database Tables

- snake_case

Example:
- product_items
- order_details

---

# Security Rules

- JWT Authentication
- Role-based authorization
- Passwords must be encrypted
- Never expose sensitive information

---

# Transaction Rules

Use @Transactional for:
- Order creation
- Inventory updates
- Payment operations

---

# Cursor AI Instructions

When generating code:
- Follow modular architecture strictly
- Keep controllers thin
- Use DTO validation
- Avoid duplicate logic
- Use clean code principles
- Follow existing project structure