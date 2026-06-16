# User Module Specification

## Purpose

Manage user profile information.

This module is responsible for:

- View current profile
- Update profile
- Admin view users
- Admin search users

Authentication is handled by Security Module.

---

# Endpoints

## GET /api/v1/users/profile

Get current user profile.

Authentication required.

Response:

{
"success": true,
"message": "Profile retrieved successfully",
"data": {
"id": 1,
"username": "john",
"email": "john@gmail.com",
"fullName": "John Doe",
"phone": "0123456789",
"role": "CUSTOMER",
"status": "ACTIVE"
}
}

---

## PUT /api/v1/users/profile

Update current user profile.

Request:

{
"fullName": "John Doe",
"phone": "0123456789"
}

Response:

Updated profile.

---

## GET /api/v1/users

Admin only.

Pagination required.

Query Params:

page
size
sort

Search:

keyword

Search by:

- username
- email
- fullName

---

## GET /api/v1/users/{id}

Admin only.

View user details.

---

# Package Structure

modules/user/

controller/
- UserController

service/
- UserService
- UserServiceImpl

dto/request/
- UpdateProfileRequest

dto/response/
- UserProfileResponse
- UserSummaryResponse

mapper/
- UserMapper

repository/
- UserRepository

entity/
- User

---

# Validation Rules

fullName

- @Size(max = 150)

phone

- @Size(max = 20)

---

# Security Rules

GET /profile

authenticated

PUT /profile

authenticated

GET /users

ROLE_ADMIN

GET /users/{id}

ROLE_ADMIN

---

# Business Rules

Email cannot be changed in V1.

Username cannot be changed in V1.

Role cannot be changed in V1.

Password update is not part of User Module.

Password management belongs to Auth Module.

---

# Pagination

GET /users

must support:

?page=
&size=
&sort=

Return Page<UserSummaryResponse>

---

# Exceptions

User not found

→ ResourceNotFoundException

Phone already exists

→ BusinessException

Validation failures

→ GlobalExceptionHandler