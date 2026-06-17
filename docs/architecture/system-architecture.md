# SECDSP Backend Architecture

## Project Type

* Architecture Style: Modular Monolith
* Pattern: Layered Architecture
* Language: Java 17
* Framework: Spring Boot 3.5
* Database: PostgreSQL
* Cache: Redis
* ORM: Spring Data JPA + Hibernate
* Migration: Flyway
* Authentication: JWT Stateless Authentication
* API Documentation: Swagger/OpenAPI

---

# Architectural Principles

1. Database schema is immutable.
2. Database is the single source of truth.
3. Modules communicate through services, never repositories.
4. Controllers must remain thin.
5. Business logic belongs in services only.
6. Entities must not contain business logic.
7. DTOs must be used for all API requests and responses.
8. Never expose entities directly through APIs.
9. Prefer composition over duplication.
10. Follow package-by-module organization.

---

# Package Structure

src/main/java/com/example/secdsp

common/
config/
infrastructure/
security/

modules/
├── analytics
├── auth
├── brand
├── cart
├── category
├── chatbot
├── inventory
├── order
├── product
├── recommendation
├── user
└── wishlist

---

# Standard Module Structure

modules/<module>

├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── mapper
├── repository
├── service
│   ├── impl
│   └── interfaces
└── exception

---

# Layer Responsibilities

Controller

* Receive requests
* Validate DTOs
* Call services
* Return ApiResponse
* No business logic
* No repository access

Service

* Business logic
* Validation beyond DTO validation
* Transaction management
* Orchestration between modules

Repository

* Database access only
* No business logic

Mapper

* Entity ↔ DTO conversion
* Use MapStruct

Entity

* Database mapping only
* No business logic
* Lifecycle hooks allowed (@PrePersist, @PreUpdate)

---

# Response Standard

All APIs must return:

ApiResponse<T>

Example:

ApiResponse.success(data)
ApiResponse.success(message, data)
ApiResponse.error(...)

---

# Exception Handling

Use GlobalExceptionHandler.

BusinessException
ResourceNotFoundException
UnauthorizedException
ValidationException

Never return raw exceptions.

---

# Security

Authentication:

* JWT Stateless Authentication
* BCrypt password hashing
* Spring Security
* Method Security via @PreAuthorize

Authorization:
ADMIN
MANAGER
SELLER
CUSTOMER

Authentication Flow:

Login
→ JWT issued
→ Client sends Bearer token
→ JwtAuthenticationFilter
→ UserDetails loaded
→ SecurityContext populated

---

# Soft Delete Rules

Soft delete tables:

users
brands
categories
products

Use:

deleted_at TIMESTAMP

Deleted records must not appear in normal queries.

Repositories must provide active queries.

---

# Pagination Rules

List endpoints must support:

Pageable pageable

Default:

* page
* size
* sort

Return:

Page<ResponseDto>

---

# Transaction Rules

Use:

@Transactional

Read operations:
@Transactional(readOnly = true)

Write operations:
@Transactional

Do not open transactions in controllers.

---

# Module Dependencies

auth
↓
user
↓
category
↓
brand
↓
product
↓
inventory
↓
wishlist
cart
review
↓
order
↓
recommendation
analytics
chatbot

---

# Cross Module Communication Rules

Allowed:

Service → Service

Forbidden:

Controller → Repository
Service → Repository of another module
Controller → Service of another module directly

Example:

CartService
→ ProductService
→ InventoryService

NOT:

CartService
→ ProductRepository
→ InventoryRepository

---

# Ownership Rules

Product

* owned by Seller

Inventory

* owned by Product

Cart

* owned by Customer

Wishlist

* owned by Customer

Order

* owned by Customer

Review

* owned by Customer and Product

Recommendation

* depends on Product, Order and DSS modules

---

# Coding Standards

Use constructor injection only.

Use Lombok:
@RequiredArgsConstructor
@Getter
@Setter
@Builder

No field injection.

No circular dependencies.

Prefer interfaces:

Service
ServiceImpl

Logging:
@Slf4j

Use meaningful exception messages.

---

# Development Workflow

1. Database finalized
2. Write module specification
3. Review specification
4. Generate architecture design
5. Implement module
6. Review implementation
7. Integration testing
8. API testing
9. Documentation update

Never implement before specification is approved.
