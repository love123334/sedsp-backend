# API Response Standard

All APIs must return a standardized response format.

## Success Response

```json
{
  "success": true,
  "message": "Request successful",
  "data": {}
}
```

## Error Response

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email is invalid"
    }
  ]
}
```

---

## Generic Response Wrapper

Example:

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
```

---

## HTTP Status Guidelines

200 OK

* Get successful
* Update successful

201 Created

* Resource created

204 No Content

* Delete successful

400 Bad Request

* Validation failed

401 Unauthorized

* Authentication failed

403 Forbidden

* Access denied

404 Not Found

* Resource not found

409 Conflict

* Duplicate resource

500 Internal Server Error

* Unexpected server error

```
```
