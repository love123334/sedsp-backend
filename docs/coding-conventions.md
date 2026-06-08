# Coding Conventions

## Package Naming

Use lowercase package names.

Examples:

```text
controller
service
repository
entity
dto
mapper
```

---

## Class Naming

Use PascalCase.

Examples:

* ProductController
* ProductService
* ProductRepository

---

## Method Naming

Use camelCase and meaningful names.

Examples:

* createProduct()
* updateProduct()
* getProductById()
* deleteProduct()

Avoid:

* process()
* handle()
* doSomething()

---

## Variable Naming

Use camelCase.

Examples:

```java
productName
totalAmount
availableQuantity
```

---

## Constant Naming

Use UPPER_SNAKE_CASE.

Examples:

```java
JWT_EXPIRATION
DEFAULT_PAGE_SIZE
```

---

## DTO Naming

Request DTO:

```java
CreateProductRequest
UpdateProductRequest
LoginRequest
```

Response DTO:

```java
ProductResponse
LoginResponse
UserResponse
```

---

## Service Naming

Interfaces:

```java
ProductService
OrderService
```

Implementations:

```java
ProductServiceImpl
OrderServiceImpl
```

---

## Controller Naming

```java
ProductController
OrderController
AuthController
```

---

## Repository Naming

```java
ProductRepository
OrderRepository
UserRepository
```

---

## Avoid

* God classes
* Duplicate business logic
* Long methods (>50 lines)
* Hardcoded values
* Direct entity exposure

```
```
