# AI Development Workflow - SEDSP Backend

# AI Tool Roles

## ChatGPT

Use ChatGPT for:

* System architecture
* Database design
* API design
* Business flow analysis
* Refactoring strategy
* Complex service logic
* Security review
* Code review
* Prompt engineering
* Technical documentation
* ERD review
* Transaction design
* Cursor prompt generation

ChatGPT should act as:

* Senior Solution Architect
* Senior Backend Reviewer
* Technical Lead

---

## Cursor

Use Cursor for:

* CRUD implementation
* Boilerplate generation
* DTO creation
* Repository creation
* Controller generation
* Mapper generation
* Refactoring inside project context
* Fast implementation tasks

Cursor should act as:

* Junior-to-mid Backend Engineer

---

# Recommended Workflow

## Step 1 — Ask ChatGPT First

Before implementing a feature:

Ask ChatGPT to:

* Design architecture
* Define module structure
* Define entities
* Define DTOs
* Define API endpoints
* Define service responsibilities
* Define security rules
* Define transaction boundaries
* Define validation rules
* Review edge cases
* Generate Cursor implementation prompts

---

## Step 2 — Review Design

Manually verify:

* Database relationships
* Scalability
* Business rules
* Edge cases
* Security concerns

---

## Step 3 — Implement Using Cursor

Provide:

* Feature requirements
* Existing architecture rules
* Relevant documentation

Then use ChatGPT again for code review before merging.

---

# Cursor Rules

Always enforce:

* Thin controllers
* Business logic inside services
* DTO validation
* Constructor injection
* Standardized API responses
* No duplicated logic

---

# AI Safety Rules

Never trust AI-generated code immediately.

Always review:

* Security logic
* Transactions
* Authentication
* Authorization
* Database queries
* Performance-sensitive logic

---

# AI Review Checklist

Before commit:

* Does code follow modular architecture?
* Is business logic inside service?
* Is DTO validation implemented?
* Is transaction handling correct?
* Is security implemented correctly?
* Are repository queries optimized?
* Is duplicated logic avoided?
* Are APIs consistent with project standards?
