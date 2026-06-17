# AI Coding Rules

## Database Rules

The PostgreSQL schema already exists.

Database dump is the single source of truth.

Database schema is immutable.

Never generate:

* Flyway migrations
* SQL scripts
* CREATE TABLE statements
* ALTER TABLE statements
* DROP TABLE statements
* INSERT seed data
* Enum migrations

Never modify:

src/main/resources/db/migration/**

Assume all tables already exist.

Generate only Java code that maps to existing tables.

If a table definition is missing, ask for clarification instead of creating migrations.

---

## Architecture Rules

Project architecture:
Modular Monolith
Layered Architecture

Controller
→ Service
→ Repository

Never:
Controller → Repository

Never:
Service → Repository of another module

Cross-module communication:

Service → Service only

---

## Entity Rules

Entities represent existing tables only.

No business logic inside entities.

Lifecycle hooks are allowed:

* @PrePersist
* @PreUpdate

Use:

* Long ids
* GenerationType.IDENTITY

---

## DTO Rules

Never expose entities.

All APIs use DTOs.

Request DTO:
dto/request

Response DTO:
dto/response

---

## Dependency Injection

Constructor injection only.

Use:
@RequiredArgsConstructor

Never use:
@Autowired field injection.

---

## API Rules

All APIs return:

ApiResponse<T>

Controllers must remain thin.

Business logic belongs in services.

---

## Transaction Rules

Read:
@Transactional(readOnly = true)

Write:
@Transactional

Controllers never use transactions.

---

## Repository Rules

Use Spring Data JPA.

Use Pageable.

Use soft delete queries.

Do not create native SQL unless absolutely necessary.

---

## Code Generation Rules

Generate only files required by the requested module.

Do not generate code for unrelated modules.

Do not create migrations.

Do not modify existing migrations.

Do not redesign database schema.
