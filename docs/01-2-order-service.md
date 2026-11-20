# Lesson 1.2: order-service (its own database)

## Goal
Build a second microservice, `order-service`, with **its own database** (`order_db`) and its own port (8082).
It does **not** talk to product-service yet. That comes in Lesson 1.4.

## The main lesson: database per service
```
product-service (8081) -> product_db   (user: product_user)
order-service   (8082) -> order_db     (user: order_user)
```
Rules:
1. A service reads and writes **only its own** database.
2. No foreign keys between databases.
3. To get another service's data, you call its **API**.

### Why does `Order` have `Long productId` and not `Product product`?
- In a monolith: `@ManyToOne Product product` plus a foreign key, and the database checks it.
- In microservices: the product table is in another database. JPA cannot join it, and MySQL cannot enforce a foreign key.
- So we store only the **id** (a reference) and ask product-service when we need details.

**Cost:** nobody checks that the product exists. We fix this in Lesson 1.4 with a REST call.
That is the price of independence, and why distributed systems need extra patterns later.

## What is new compared to Lesson 1.1
| Idea | Where |
|---|---|
| Enum stored as text | `@Enumerated(EnumType.STRING)` on `status` |
| Business rule inside the entity | `Order.cancel()` |
| State change instead of delete | `POST /api/orders/{id}/cancel` |
| 409 Conflict | cancelling an already cancelled order |
| Query filters | `?customerName=` and `?status=` |
| Derived queries | `findByCustomerName`, `findByStatus` |
| Table name `orders` | `ORDER` is a reserved SQL word |

## The API
| Method | URL | Success | Failure |
|---|---|---|---|
| POST | `/api/orders` | 201 + `Location` | 400 invalid body |
| GET | `/api/orders` | 200 + list | 400 bad `status` value |
| GET | `/api/orders?customerName=Asha` | 200 filtered | |
| GET | `/api/orders?status=CANCELLED` | 200 filtered | |
| GET | `/api/orders/{id}` | 200 | 404 |
| POST | `/api/orders/{id}/cancel` | 200 status CANCELLED | 404, 409 if already cancelled |

## Status codes to remember
- **400 Bad Request**: the request itself is wrong (bad JSON, failed validation).
- **404 Not Found**: the id does not exist.
- **409 Conflict**: the request is fine, but the **current state** does not allow it.

## Run it
```bash
docker compose up -d                       # MySQL (wait for "healthy")
mvn -pl order-service spring-boot:run      # order-service on 8082
```
Run product-service in another terminal if you like. Both run side by side.
```bash
curl -i http://localhost:8082/actuator/health
```

## Try these (success AND failure)

### Success
```bash
# Create -> 201
curl -i -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Asha","productId":1,"quantity":2}'

# Get, list, filter -> 200
curl -i http://localhost:8082/api/orders/1
curl -i http://localhost:8082/api/orders
curl -i "http://localhost:8082/api/orders?customerName=Asha"

# Cancel -> 200, status becomes CANCELLED
curl -i -X POST http://localhost:8082/api/orders/1/cancel

# Filter by status -> shows the cancelled order
curl -i "http://localhost:8082/api/orders?status=CANCELLED"
```

### Failure
| Try | Request | Expected |
|---|---|---|
| Blank customer | body `{"customerName":"","productId":1,"quantity":1}` | 400 |
| Quantity 0 | body `{"customerName":"A","productId":1,"quantity":0}` | 400 |
| Missing productId | body `{"customerName":"A","quantity":1}` | 400 |
| productId 0 | body `{"customerName":"A","productId":0,"quantity":1}` | 400 |
| Bad JSON | body `{"customerName":` | 400 |
| Unknown order | `GET /api/orders/999` | 404 |
| Cancel unknown | `POST /api/orders/999/cancel` | 404 |
| **Cancel twice** | `POST /api/orders/1/cancel` two times | 200 then **409** |
| Bad status filter | `GET /api/orders?status=ABC` | 400 |
| Database down | `docker compose stop mysql`, then `GET /api/orders` | 500 |

### Observe the missing check (on purpose)
```bash
curl -i -X POST http://localhost:8082/api/orders -H "Content-Type: application/json" \
  -d '{"customerName":"Ghost","productId":99999,"quantity":1}'
```
It returns **201** even though product 99999 does not exist. Lesson 1.4 fixes this.

## Look inside the database
```bash
docker exec -it shopsphere-mysql mysql -uorder_user -porder_pass order_db \
  -e "DESCRIBE orders; SELECT * FROM orders;"
```
Observe: no foreign key on `product_id`, and `status` holds the text `CREATED` / `CANCELLED`.

### Prove the isolation
```bash
docker exec -it shopsphere-mysql mysql -uorder_user -porder_pass -e "SELECT * FROM product_db.products;"
```
Expected: `ERROR 1142 ... SELECT command denied` (or 1044 for `USE product_db`).

## Common mistakes
- **Port 8082 in use**: `ss -ltnp | grep 8082`.
- **Access denied for order_user**: the MySQL volume was created before `init.sql` had `order_db`. Run `docker compose down -v` then `up -d`.
- **Wrong port in the URL**: orders are on 8082, products on 8081.

## Recap card
- One service = one database = one DB user.
- Reference other services' data by **id only**, never by JPA relation.
- 400 bad request, 404 missing, 409 wrong state.
- Cancel is a **state change** (POST), not a delete.
- Product existence is not checked yet. Lesson 1.4 adds that with `RestClient`.
