# Product Module Specification

## Purpose

Manage products owned by sellers.

Product owns:

* products
* product_images
* product_attributes

Product does not own inventory.

Inventory is a separate module.

---

## Database Tables

products
product_images
product_attributes

Database schema is immutable.

---

## Dependencies

Depends on:

* security
* auth
* user
* category
* brand

Referenced by:

* inventory
* wishlist
* cart
* review
* order
* recommendation

---

## Responsibilities

* Create product
* Update product
* Soft delete product
* Get product detail
* List products
* Search products
* Manage product images
* Manage product attributes

---

## Ownership

One product belongs to:

* one seller
* one category
* one brand

One product may have:

* many images
* many attributes

---

## Business Rules

Seller can manage only their own products.

Admin can manage all products.

Deleted products must not appear in normal queries.

Category must exist.

Brand must exist.

Seller must exist.

Product module must not access InventoryRepository.

Cross-module communication must occur via services only.

---

## API Endpoints

POST /api/v1/products

PUT /api/v1/products/{id}

DELETE /api/v1/products/{id}

GET /api/v1/products/{id}

GET /api/v1/products

GET /api/v1/products/search

---

## Request DTOs

CreateProductRequest

UpdateProductRequest

AddProductImageRequest

UpdateProductAttributeRequest

---

## Response DTOs

ProductResponse

ProductDetailResponse

ProductImageResponse

ProductAttributeResponse

---

## Validation Rules

Product name

* required

Category

* must exist

Brand

* must exist

Seller

* must exist

Image URLs

* valid format

---

## Security Rules

ADMIN

* full access

SELLER

* manage own products

MANAGER

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

Read
@Transactional(readOnly = true)

---

## Search and Filtering

Support:

keyword

categoryId

brandId

sellerId

sorting

pagination

Return:
Page<ProductResponse>

---

## Edge Cases

Seller updates another seller's product.

Category deleted.

Brand deleted.

Duplicate image.

Deleted product requested.

Invalid product id.

---

## Implementation Notes

Use:

* DTOs
* MapStruct
* Pageable
* soft delete
* ApiResponse<T>
* Service -> Service communication only

Do not query repositories across modules.

Controllers must remain thin.
