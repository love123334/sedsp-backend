# Auth Module Specification

## Purpose

Provide authentication endpoints for the system.

Responsibilities:

- Login using email and password
- Return JWT access token
- Return current authenticated user information

Authentication is delegated to Security Module.

---

# Endpoints

## POST /api/v1/auth/login

Authenticate user.

Request:

```json
{
  "email": "user@example.com",
  "password": "password"
}
```

Response:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "...",
    "expiresInSeconds": 86400,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "username": "user01",
      "role": "CUSTOMER"
    }
  }
}
```

---

## GET /api/v1/auth/me

Return authenticated user.

Authorization:

Bearer Token required.

Response:

```json
{
  "success": true,
  "message": "Current user retrieved successfully",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "username": "user01",
    "fullName": "John Doe",
    "phone": "0123456789",
    "role": "CUSTOMER"
  }
}
```

---

# Package Structure

modules/auth/

controller/
- AuthController

service/
- AuthService
- AuthServiceImpl

dto/request/
- LoginRequest

dto/response/
- LoginResponse
- CurrentUserSummary
- MeResponse

mapper/
- AuthMapper

---

# Login Flow

Email + Password

→ AuthenticationManager

→ CustomUserDetailsService

→ BCrypt verification

→ JwtProvider.generateToken()

→ Return LoginResponse

---

# Current User Flow

JWT

→ JwtAuthenticationFilter

→ SecurityContextHolder

→ AuthService.getCurrentUser()

→ UserRepository.findById()

→ MeResponse

---

# Validation Rules

LoginRequest

email

- @NotBlank
- @Email

password

- @NotBlank
- @Size(min = 8, max = 72)

---

# Security Rules

POST /login

- permitAll

GET /me

- authenticated

Never expose password.

Never expose internal security information.

---

# Exceptions

Invalid credentials

→ UnauthorizedException

User not found

→ ResourceNotFoundException

Validation failures

→ handled by GlobalExceptionHandler