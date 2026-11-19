# Lesson 1.1: product-service (Spring Boot + JPA + MySQL)

## Goal
Build our first microservice: a small REST API that manages products and stores them in **its own** database (`product_db`).

## What you will learn
- How a microservice is structured (layers)
- Why we use DTOs instead of exposing the entity
- Bean validation and HTTP status codes
- The first rule of microservices: **a service owns its data**

## The layers (remember: C-S-R-E)
```
HTTP request
   |
Controller  -> reads the request, returns the response (HTTP only, no logic)
   |
Service     -> business rules, transactions
   |
Repository  -> talks to the database (Spring Data JPA)
   |
Entity      -> a row in the "products" table
```
| Class | Package | Job |
|---|---|---|
| `ProductController` | controller | URLs, status codes |
| `ProductService` | service | logic, transactions, 404 checks |
| `ProductRepository` | repository | `findAll`, `save`, `deleteById`... (generated) |
| `Product` | model | table `products` |
| `ProductRequest` | dto | JSON coming **in** + validation rules |
| `ProductResponse` | dto | JSON going **out** |

## Why DTOs? (simple words)
The entity is your **database shape**. The DTO is your **API shape**. Keep them separate so that:
1. You can change a column without breaking every client.
2. You never leak internal fields by accident.
3. Validation rules sit on the input object, not on the table.

In microservices this matters more: other services depend on your API, so the API must stay stable.

## The API
| Method | URL | Success | Failure |
|---|---|---|---|
| GET | `/api/products` | 200 + list | |
| GET | `/api/products/{id}` | 200 | 404 not found |
| POST | `/api/products` | 201 + `Location` header | 400 invalid body |
| PUT | `/api/products/{id}` | 200 | 400 invalid, 404 not found |
| DELETE | `/api/products/{id}` | 204 | 404 not found |

## Key ideas explained
- **`@Transactional`**: one service method = one database transaction. If an exception is thrown, changes roll back.
- **Update without `save()`**: inside a transaction the entity is "managed". Hibernate notices the changed fields and writes the `UPDATE` at commit (dirty checking).
- **`BigDecimal` for money**: `double` gives rounding errors.
- **`ddl-auto: update`**: Hibernate creates the table for you. Good for learning, not for production (use Flyway/Liquibase).
- **`open-in-view: false`**: stops lazy loading from leaking into the web layer. A safer default.
- **`show-sql: true`**: SQL is printed in the console so you can see what each call does.
- **Port 8081**: product-service. order-service will get 8082.
- **Boot 4 naming**: the web starter is `spring-boot-starter-webmvc`.

## Run it
1. Start MySQL (Docker Desktop must be running):
   ```bash
   docker compose up -d
   docker compose ps          # wait for "healthy"
   ```
2. Start the service (JDK 25 required):
   ```bash
   mvn -pl product-service spring-boot:run
   ```
3. Check: `curl -i http://localhost:8081/actuator/health` -> `{"status":"UP"}`

## Try these (success AND failure)

### Success scenarios
```bash
# Create -> 201 Created, look at the Location header
curl -i -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":55000.50,"stock":10}'

# List -> 200
curl -i http://localhost:8081/api/products

# Get one -> 200
curl -i http://localhost:8081/api/products/1

# Update -> 200 (watch the UPDATE SQL in the console)
curl -i -X PUT http://localhost:8081/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop Pro","price":65000,"stock":7}'

# Delete -> 204
curl -i -X DELETE http://localhost:8081/api/products/1
```

### Failure scenarios
| Try | Command body / URL | Expected |
|---|---|---|
| Blank name | `{"name":"","price":10,"stock":1}` | 400 |
| Negative price | `{"name":"A","price":-5,"stock":1}` | 400 |
| Missing price | `{"name":"A","stock":1}` | 400 |
| Negative stock | `{"name":"A","price":5,"stock":-1}` | 400 |
| Bad JSON | `{"name":` | 400 |
| Unknown id | `GET /api/products/999` | 404 |
| Delete twice | `DELETE /api/products/1` again | 404 |
| Letters as id | `GET /api/products/abc` | 400 |
| Database down | `docker compose stop mysql`, then `GET /api/products` | 500 (the service cannot reach its data) |
| Database back | `docker compose start mysql`, retry | works again |

Example failing call:
```bash
curl -i -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"","price":-5,"stock":-1}'
```

Note: the 400 and 500 bodies are Spring's default format. **Lesson 1.5** replaces them with one clean error format.

## Look inside the database
```bash
docker exec -it shopsphere-mysql mysql -uproduct_user -pproduct_pass product_db \
  -e "DESCRIBE products; SELECT * FROM products;"
```
Observe: Hibernate created the `products` table with `price DECIMAL(12,2)` and an `AUTO_INCREMENT` id.

## Common mistakes
- **Connection refused / startup fails**: MySQL is not running, or you used port 3306 instead of **3307**.
- **Access denied**: the volume was created before `init.sql` existed. Run `docker compose down -v` then `up -d` to re-create it.
- **Port 8081 in use**: another process holds it (`ss -ltnp | grep 8081`).
- **`release version 25 not supported`**: Maven is running on an older JDK. Check `mvn -v`.

## Recap card
- Layers: Controller -> Service -> Repository -> Entity.
- DTO = API shape, Entity = DB shape.
- `@Valid` + validation annotations -> automatic HTTP 400.
- 201 for create, 204 for delete, 404 for missing.
- product-service uses only `product_db` (database per service).
- Next: Lesson 1.2, `order-service` with its own database.
