# Category Module Specification

## Purpose

Manage product categories.

Categories support hierarchical structure through parent_id.

This module is responsible for:

- Create category
- Update category
- Delete category (soft delete)
- View category detail
- View category tree
- Search categories

---

# Database

Table: categories

Columns:

- id
- name
- slug
- parent_id
- created_at
- deleted_at

Relationships:

Category
→ Parent Category (ManyToOne)

Category
→ Child Categories (OneToMany)

---

# Endpoints

## POST /api/v1/categories

Create category.

Admin only.

Request:

{
"name": "Electronics",
"slug": "electronics",
"parentId": null
}

Response:

CategoryResponse

---

## PUT /api/v1/categories/{id}

Update category.

Admin only.

---

## DELETE /api/v1/categories/{id}

Soft delete category.

Admin only.

Rules:

Cannot delete category if active products exist.

---

## GET /api/v1/categories/{id}

Get category detail.

Public.

---

## GET /api/v1/categories

Get categories.

Public.

Pagination supported.

Query Params:

page
size
sort
keyword

---

## GET /api/v1/categories/tree

Get hierarchical category tree.

Public.

Example:

Electronics
├─ Laptop
├─ Phone
└─ Tablet

---

# Package Structure

modules/category/

controller/
- CategoryController

service/
- CategoryService
- CategoryServiceImpl

repository/
- CategoryRepository

entity/
- Category

dto/request/
- CreateCategoryRequest
- UpdateCategoryRequest

dto/response/
- CategoryResponse
- CategoryTreeResponse

mapper/
- CategoryMapper

---

# Validation Rules

name

- @NotBlank
- @Size(max = 150)

slug

- @NotBlank
- @Size(max = 150)

parentId

- nullable

---

# Business Rules

Category name must be unique.

Category slug must be unique.

Parent category must exist.

Cannot assign category as its own parent.

Prevent circular hierarchy.

Soft delete only.

deleted_at != null means deleted.

Deleted categories should not appear in public APIs.

---

# Security Rules

POST /categories

ROLE_ADMIN

PUT /categories/{id}

ROLE_ADMIN

DELETE /categories/{id}

ROLE_ADMIN

GET APIs

permitAll

---

# Pagination

GET /categories

Support:

?page=
&size=
&sort=

---

# Exceptions

Category not found

→ ResourceNotFoundException

Duplicate category name

→ BusinessException

Duplicate slug

→ BusinessException

Invalid parent category

→ BusinessException

Circular hierarchy

→ BusinessException
