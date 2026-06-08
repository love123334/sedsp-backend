# Security Module Specification

## Purpose

Provide authentication and authorization infrastructure for the entire system.

This module is responsible for:

* JWT generation
* JWT validation
* Authentication
* Authorization
* SecurityContext population
* Access control
* Security exception handling

Business authentication logic belongs to Auth Module.

---

# Authentication Strategy

Authentication Type:

* JWT Bearer Token

Session Policy:

* Stateless

JWT Algorithm:

* HS256

Refresh Token:

* Not supported in V1

Redis Token Revocation:

* Not supported in V1

---

# Login Strategy

Login identifier:

* Email only

Database:

users.email

Authentication flow:

Email + Password
→ AuthenticationManager
→ UserDetailsService
→ BCrypt verification
→ JWT generation
→ Return access token

---

# Authorization Strategy

Roles:

* CUSTOMER
* SELLER
* MANAGER
* ADMIN

JWT contains:

{
"sub": userId,
"role": roleName
}

Spring Security authorities:

ROLE_CUSTOMER
ROLE_SELLER
ROLE_MANAGER
ROLE_ADMIN

---

# Security Package Structure

security/

config/

* SecurityConfig

jwt/

* JwtProvider
* JwtAuthenticationFilter

user/

* UserDetailsImpl
* CustomUserDetailsService

handler/

* RestAuthenticationEntryPoint
* RestAccessDeniedHandler

---

# JwtProvider Responsibilities

Generate Access Token

Validate Token

Extract:

* userId
* role

JWT Claims:

sub
role
iat
exp

---

# JwtAuthenticationFilter Responsibilities

Run once per request.

Read:

Authorization: Bearer <token>

Validate token.

Extract user information.

Create Authentication object.

Store into SecurityContextHolder.

Skip if token missing.

---

# UserDetailsImpl

Represents authenticated user.

Fields:

* id
* email
* role

Do not expose password.

Authorities generated from role.

---

# CustomUserDetailsService

Load user by email.

Query:

UserRepository.findByEmail()

Throw UsernameNotFoundException when user does not exist.

---

# SecurityConfig

Permit:

POST /api/v1/auth/login

Swagger:

/swagger-ui/**
/v3/api-docs/**

Require authentication for all other endpoints.

Disable:

* CSRF
* Session

Enable:

* JWT Filter

Password Encoder:

BCryptPasswordEncoder

AuthenticationManager Bean

---

# AuthenticationEntryPoint

Handle:

401 Unauthorized

Response:

ApiResponse

success = false

---

# AccessDeniedHandler

Handle:

403 Forbidden

Response:

ApiResponse

success = false

---

# Security Rules

Never expose password.

Never trust client role.

Role comes from database.

All protected endpoints require valid JWT.

---

# Future Enhancements

V2:

* Refresh Token
* Redis Revocation
* Token Blacklist
* Account Locking
* Multi Device Session Control
