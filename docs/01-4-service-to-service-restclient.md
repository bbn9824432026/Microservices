# Lesson 1.4: Service-to-Service Calls with RestClient

## Goal
`order-service` must check that a product exists and has enough stock **before** saving an order.
It cannot read `product_db` (Lesson 1.3), so it asks **product-service's API** over HTTP.

```
Client --POST /api/orders--> order-service --GET /api/products/{id}--> product-service
                                  |                                         |
                              order_db                                  product_db
```

## What changed
| File (order-service) | Job |
|---|---|
| `config/RestClientConfig` | Builds the `RestClient`: base URL + connect/read timeouts from `application.yml` |
| `client/ProductClient` | The only class that knows how to call product-service |
| `client/ProductDto` | order-service's own copy of the product JSON shape |
| `client/ProductServiceUnavailableException` | "product-service is down/slow" -> HTTP 503 |
| `service/OrderService.create` | Calls the client, applies the rules, then saves |
| `application.yml` | `product-service.url`, `connect-timeout-ms`, `read-timeout-ms` |

## The create-order flow
1. Ask product-service for the product.
2. Product not found -> **422** (our body points at something that does not exist).
3. `stock < quantity` -> **409** (valid request, but state does not allow it).
4. product-service down / slow / 5xx -> **503**.
5. Otherwise save the order -> **201**.

## Key ideas (simple words)

### 1. RestClient
Spring's modern, fluent HTTP client (replaces the old `RestTemplate`).
```java
restClient.get().uri("/api/products/{id}", id).retrieve().body(ProductDto.class);
```
`retrieve()` throws an exception for 4xx/5xx answers, so we catch them and decide what they mean.

### 2. Not found is not a failure
- **404 from product-service** = a normal answer: "no such product". We return `Optional.empty()`.
- **Connection refused / timeout / 5xx** = the service has a problem. We throw `ProductServiceUnavailableException`.

Always separate "the answer is no" from "I could not get an answer".

### 3. Timeouts are mandatory
Without a timeout, a slow product-service makes order-service threads wait forever, and then **order-service dies too**.
This is a *cascading failure*.
- **Connect timeout (1s)**: time allowed to open the connection.
- **Read timeout (2s)**: time allowed to wait for the reply.

### 4. Do not hold a DB transaction during a network call
`create()` uses `@Transactional(propagation = NOT_SUPPORTED)`. A transaction keeps a database connection busy.
A slow remote call would block that connection and exhaust the pool. So: call first, save after.

### 5. Own DTO, no shared code
`ProductDto` is defined inside order-service. Services share the **JSON contract**, never Java classes.
If you shared a library, you could not deploy or change services independently.

### 6. One client class
All HTTP details are in `ProductClient`. Lessons 3 and 4 (discovery, retry, circuit breaker) change only this class.

### 7. Hardcoded URL (for now)
`http://localhost:8081` works on one machine. In Docker/Kubernetes the address changes. Lesson 3 (Eureka) fixes that.

## Known limits (on purpose, fixed later)
- We only **check** stock. We do not reduce it. Two orders at the same time can both pass the check (**race condition**). Lesson 7 (Saga) solves this.
- Errors are still Spring's default format. Lesson 1.5 adds one clean error format.
- No retry, no circuit breaker. Lesson 4.

## Run it
```bash
docker compose up -d
mvn -pl product-service spring-boot:run     # terminal 1 (8081)
mvn -pl order-service spring-boot:run       # terminal 2 (8082)
```

## Try these (success AND failure)

Prepare a product (stock 10):
```bash
curl -i -X POST http://localhost:8081/api/products -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":55000,"stock":10}'
```
Use the returned id below (assumed `1`).

| # | Scenario | Command | Expected |
|---|---|---|---|
| 1 | Success | `POST /api/orders` `{"customerName":"Asha","productId":1,"quantity":2}` | **201** |
| 2 | Exactly all stock | same with `"quantity":10` | **201** |
| 3 | Not enough stock | same with `"quantity":11` | **409** "Not enough stock..." |
| 4 | Product does not exist | `"productId":99999` | **422** "Product 99999 does not exist" |
| 5 | Invalid body | `"quantity":0` | **400** (fails before any call to product-service) |
| 6 | **product-service down** | stop terminal 1 (Ctrl+C), place an order | **503** quickly |
| 7 | Service back | start product-service again, retry | **201** |
| 8 | **Slow service** | see below | **503** after about 2s |

Example call:
```bash
curl -i -X POST http://localhost:8082/api/orders -H "Content-Type: application/json" \
  -d '{"customerName":"Asha","productId":1,"quantity":2}'
```

### Simulate a slow product-service (scenario 8)
Freeze the product-service process so it accepts the connection but never answers:
```bash
ss -ltnp | grep 8081            # note the PID
kill -STOP <PID>                # freeze it
curl -i -X POST http://localhost:8082/api/orders -H "Content-Type: application/json" \
  -d '{"customerName":"Asha","productId":1,"quantity":1}'   # returns 503 after ~2s, not forever
kill -CONT <PID>                # unfreeze it
```
Now set `read-timeout-ms: 60000` in `order-service/src/main/resources/application.yml`, restart, repeat: the call hangs
for up to a minute. That is why timeouts matter.

### Observe the Hibernate SQL
In the order-service console, a failed call (cases 3, 4, 6) prints **no** `insert` statement: nothing was saved.

## Common mistakes
- **Always 503**: product-service is not running or the URL/port in `application.yml` is wrong.
- **404 on the product call from your terminal**: create the product first (ids are auto-generated).
- **Old orders exist** from Lesson 1.2 for products that do not exist. They are not re-checked; only new orders are.

## Interview questions this answers
- **How do services communicate synchronously?** REST over HTTP (or gRPC).
- **What if the called service is slow?** Timeouts, then retry/circuit breaker (Lesson 4).
- **Why not share DTO classes in a common jar?** It couples deployments; share the contract, not the code.
- **Why is the stock check unsafe?** Check-then-act race; needs a Saga or a reservation (Lesson 7).

## Recap card
- Ask another service through its **API**, never its database.
- 404 from the other service = business answer. Timeout/5xx = failure.
- **Always set connect and read timeouts.**
- No DB transaction around a remote call.
- Own DTO per service; one client class per remote service.
- Next: Lesson 1.5, global exception handling, validation and OpenAPI docs.
