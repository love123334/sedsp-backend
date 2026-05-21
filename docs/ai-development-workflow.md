# AI Development Workflow - SEDSP Backend

# AI Tool Roles

## Claude Pro

Use Claude Pro for:
- System architecture
- Database design
- API design
- Business flow analysis
- Refactoring strategy
- Complex service logic
- Security review
- Code review
- Prompt engineering
- Technical documentation

Claude should act as:
- Senior Solution Architect
- Senior Backend Reviewer

---

## Cursor

Use Cursor for:
- CRUD implementation
- Boilerplate generation
- DTO creation
- Repository creation
- Controller generation
- Mapper generation
- Refactoring inside project context
- Fast implementation tasks

Cursor should act as:
- Junior-to-mid backend engineer

---

# Recommended Workflow

## Step 1 — Ask Claude First

Before implementing a feature:

Ask Claude to:
- Design architecture
- Define module structure
- Define entities
- Define DTOs
- Define API endpoints
- Define service responsibilities
- Define security rules
- Define transaction boundaries

---

## Step 2 — Review Design

Manually verify:
- Database relationships
- Scalability
- Business rules
- Edge cases
- Security concerns

---

## Step 3 — Implement Using Cursor

In Cursor:

Provide:
- Feature requirements
- Existing architecture rules
- Relevant documentation

Prompt example:

```text
Create Product module following:
- docs/project-architecture.md
- docs/ai-development-workflow.md

Requirements:
- Product entity
- CRUD APIs
- Pagination
- Validation
- DTOs
- Service layer
- Repository layer
- Flyway migration

Use clean architecture and RESTful API design.
```

---

# Cursor Rules

## Always enforce:
- Thin controllers
- Business logic inside services
- DTO validation
- Constructor injection
- Standardized API responses
- No duplicated logic

---

# AI Safety Rules

Never trust AI-generated code immediately.

Always review:
- Security logic
- Transactions
- Authentication
- Authorization
- Database queries
- Performance-sensitive logic

---

# Backend Development Order

## Phase 1 — Foundation
- Auth
- JWT
- User
- Role
- Security
- Exception handling

## Phase 2 — Core Ecommerce
- Product
- Category
- Inventory
- Cart
- Order

## Phase 3 — Business Features
- Analytics
- Reporting
- Dashboard

## Phase 4 — AI/DSS
- Recommendation
- Prediction
- Chatbot
- What-if analysis

---

# Prompting Rules

## Good Prompt

```text
Create Order module following docs/project-architecture.md.

Requirements:
- Order entity
- OrderItem entity
- Create order API
- Transaction handling
- Inventory validation
- DTO validation
- RESTful design
```

---

## Bad Prompt

```text
Build ecommerce backend
```

---

# AI Review Checklist

Before commit:
- Does code follow modular architecture?
- Is business logic inside service?
- Is DTO validation implemented?
- Is transaction handling correct?
- Is security implemented correctly?
- Are repository queries optimized?
- Is duplicated logic avoided?