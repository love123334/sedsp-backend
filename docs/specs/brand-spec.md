# Brand Module Specification

## Purpose

Manage product brands used by products.

Brand is a master-data module and must not depend on Product, Inventory, Cart, Order, Wishlist, Review, Recommendation or Analytics modules.

---

## Database Tables

brands

Database schema is immutable.

---

## Dependencies

Depends on:

* security
* auth
* user

May be referenced by:

* product

Must not directly depend on:

* product
* inventory
* cart
* order
* wishlist
* review
* recommendation
* analytics

---

## Responsibilities

* Create brand
* Update brand
* Soft delete brand
* Get brand detail
* List brands
* Search brands

---

## Business Rules

Brand name must be unique.

Brand slug must be unique.

Deleted brands must not appear in normal queries.

Brand deletion uses deleted_at.

Brand module must not query ProductRepository.

Brand module must not call ProductService.

---

## API Endpoints

POST /api/v1/brands

PUT /api/v1/brands/{id}

DELETE /api/v1/brands/{id}

GET /api/v1/brands/{id}

GET /api/v1/brands

---

## Request DTOs

CreateBrandRequest

* name
* slug

UpdateBrandRequest

* name
* slug

---

## Response DTO

BrandResponse

* id
* name
* slug
* createdAt
* deletedAt

---

## Validation Rules

name

* required
* max length follows database schema

slug

* required
* max length follows database schema

---

## Security Rules

ADMIN

* create
* update
* delete

MANAGER

* read only

SELLER

* read only

CUSTOMER

* read only

---

## Transaction Rules

Create
@Transactional

Update
@Transactional

Delete
@Transactional

Read operations
@Transactional(readOnly = true)

---

## Search

Support:

* keyword
* pageable
* sorting

Return:
Page<BrandResponse>

---

## Edge Cases

Create duplicate name.

Create duplicate slug.

Update to duplicate name.

Update to duplicate slug.

Read deleted brand.

Delete non-existing brand.

---

## Implementation Notes

Follow Category module conventions.

Use:

* ApiResponse<T>
* MapStruct
* constructor injection
* logging
* GlobalExceptionHandler
* soft delete
* Pageable

Do not expose entities.
Do not put business logic inside entities.
