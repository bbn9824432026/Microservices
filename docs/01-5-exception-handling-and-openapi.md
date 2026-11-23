# Lesson 1.5: Global Exception Handling, Validation and OpenAPI

## Goal
Every error from every service should look the **same** and be **useful**. Also, document the API automatically.

## Before vs after
Before (Lesson 1.1-1.4): errors came from Spring's default format and from `ResponseStatusException`s thrown inside services.
After: services throw **business exceptions**, and one class converts them to JSON.

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/products",
  "errors": { "name": "name must not be blank", "price": "price must be greater than 0" }
}
```
This is the standard **Problem Details** format (RFC 9457). Clients can parse every error the same way.

## What changed
| File | Job |
|---|---|
| `exception/GlobalExceptionHandler` (both services) | `@RestControllerAdvice`: maps exceptions to HTTP status + JSON |
| `ProductNotFoundException` (product) | -> 404 |
| `OrderNotFoundException` (order) | -> 404 |
| `InvalidOrderStateException` (order) | cancel twice -> 409 |
| `InsufficientStockException` (order) | not enough stock -> 409 |
| `ProductDoesNotExistException` (order) | unknown product -> 422 |
| `ProductServiceUnavailableException` (order) | dependency down -> 503 |
| `config/OpenApiConfig` (both) | Swagger page title/description |
| Controllers | `@Tag`, `@Operation`, `@ApiResponse` = Swagger descriptions only |
| `pom.xml` (both) | `springdoc-openapi-starter-webmvc-ui` 3.1.1 (the 3.1.x line targets Spring Boot 4.1) |

## Key ideas (simple words)

### 1. Business exceptions, not HTTP in the service
The service says **what went wrong** (`ProductNotFoundException`). The handler decides **how to show it** (404).
The service no longer knows about HTTP. That keeps business code clean and testable.

### 2. `@RestControllerAdvice`
One class that watches all controllers. When any controller or service throws, Spring looks here for a matching
`@ExceptionHandler`.

### 3. Extend `ResponseEntityExceptionHandler`
It already handles Spring's own errors in Problem Details format:
| Problem | Result |
|---|---|
| Malformed JSON | 400 |
| Wrong type, e.g. `/api/products/abc` | 400 |
| Wrong HTTP method | 405 |
| Wrong `Content-Type` | 415 |
| Unknown URL | 404 |
We only override the validation case to add the `errors` map (which field is wrong and why).

### 4. Status codes: who is at fault?
| Code | Meaning | Example |
|---|---|---|
| 400 | Request is malformed or invalid | blank name |
| 404 | Id does not exist | `GET /api/products/999` |
| 409 | Valid, but clashes with current state | cancel twice, not enough stock |
| 422 | Valid JSON, but refers to something missing | order for unknown product |
| 500 | Our bug | unexpected exception |
| 503 | Temporary: dependency/DB down | product-service stopped |

**4xx = the client should change the request. 5xx = the server has a problem, retrying may help.**

### 5. Never leak internals
The catch-all handler logs the real exception (with stack trace) on the server but returns only
"Something went wrong". Stack traces and SQL in responses help attackers.

### 6. Database down -> 503, not 500
`DataAccessException` means the database failed. It is usually temporary, so 503 tells clients "retry later".

### 7. Validation vs business rules
- **Validation** (`@NotBlank`, `@Positive`, `@Min`): checks the **shape** of the input. Needs no database. -> 400.
- **Business rules** (product exists, enough stock): need data and logic. -> 404 / 409 / 422.

### 8. OpenAPI and Swagger UI
`springdoc` reads your controllers and DTOs and generates a machine-readable API description (OpenAPI JSON)
plus an interactive page (Swagger UI). Other teams, Postman and code generators can use it. It stays in sync with the code.

## Run it
```bash
docker compose up -d
mvn -pl product-service spring-boot:run     # 8081
mvn -pl order-service spring-boot:run       # 8082
```
- Swagger UI: http://localhost:8081/swagger-ui.html and http://localhost:8082/swagger-ui.html
- Raw OpenAPI JSON: http://localhost:8081/v3/api-docs and http://localhost:8082/v3/api-docs

Open Swagger UI, expand an endpoint, press **Try it out** and **Execute**. You can test every call from the browser.

## Try these (success AND failure)
All errors should now be JSON with `type`, `title`, `status`, `detail`, `instance`.

| # | Scenario | Request | Expected |
|---|---|---|---|
| 1 | Validation, many fields | `POST :8081/api/products` `{"name":"","price":-5,"stock":-1}` | 400 + `errors` with **3 fields** |
| 2 | Missing field | `POST :8081/api/products` `{"name":"A","stock":1}` | 400, `errors.price` |
| 3 | Malformed JSON | `POST :8081/api/products` body `{"name":` | 400 Problem Details |
| 4 | Wrong id type | `GET :8081/api/products/abc` | 400 |
| 5 | Not found | `GET :8081/api/products/999` | 404 title "Product not found" |
| 6 | Wrong method | `PATCH :8081/api/products/1` | 405 |
| 7 | Wrong content type | POST with `Content-Type: text/plain` | 415 |
| 8 | Order validation | `POST :8082/api/orders` `{"customerName":"","productId":0,"quantity":0}` | 400 + 3 errors |
| 9 | Order not found | `GET :8082/api/orders/999` | 404 |
| 10 | Cancel twice | `POST :8082/api/orders/1/cancel` two times | 200, then 409 |
| 11 | Not enough stock | order with `quantity` above stock | 409 |
| 12 | Unknown product | order with `productId` 99999 | 422 |
| 13 | product-service stopped | place an order | 503 "product-service is not available" |
| 14 | Bad enum filter | `GET :8082/api/orders?status=ABC` | 400 |
| 15 | **Database down** | `docker compose stop mysql`, then `GET :8081/api/products` | **503** "Database unavailable" (was 500 before) |
| 16 | Database back | `docker compose start mysql`, retry | works (may take a few seconds as the pool reconnects) |

Example:
```bash
curl -i -X POST http://localhost:8081/api/products -H "Content-Type: application/json" \
  -d '{"name":"","price":-5,"stock":-1}'
```
Look at the server console on scenario 15: the full stack trace is **logged**, but the client only sees the friendly message.

## Common mistakes
- **No `errors` map in the response**: the handler must override `handleMethodArgumentNotValid`; check the class extends `ResponseEntityExceptionHandler`.
- **Swagger UI shows 404**: use `/swagger-ui.html` (it redirects) and confirm the springdoc dependency is in that service's `pom.xml`.
- **Catching `Exception` too early**: the catch-all must stay a safety net; add specific handlers for known cases.
- **Returning 200 with an error message in the body**: always use the proper status code.

## Interview questions this answers
- **How do you handle exceptions globally in Spring?** `@RestControllerAdvice` + `@ExceptionHandler`.
- **What is Problem Details?** The standard JSON error format (RFC 9457), built into Spring (`ProblemDetail`).
- **400 vs 404 vs 409 vs 422?** See the table above.
- **Why not return stack traces?** Security: it leaks internals.
- **How do you document a REST API?** OpenAPI with springdoc/Swagger UI.

## Recap card
- Service throws business exceptions. One `@RestControllerAdvice` maps them to HTTP.
- Same error format (Problem Details) in every service.
- Validation errors list each wrong field.
- 4xx = client fix needed. 5xx = server problem. Database down = 503.
- Log details on the server, hide them from the client.
- Swagger UI + `/v3/api-docs` come from springdoc for free.
- Next: Lesson 1.6 checkpoint, place an order end to end (Postman collection).
