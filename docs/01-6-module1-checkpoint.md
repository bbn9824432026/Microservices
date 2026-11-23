# Lesson 1.6: Module 1 Checkpoint (end-to-end)

## Goal
Prove that everything from Lessons 1.1 to 1.5 works **together**, with an automated script and a Postman collection.

## What you built in Module 1
```
Client
  |  POST /api/orders
  v
order-service (8082) --RestClient (1s connect, 2s read timeout)--> product-service (8081)
  |                                                                      |
order_db  (order_user)                                            product_db (product_user)
```
| Lesson | Idea |
|---|---|
| 1.1 | product-service: layers, DTOs, validation |
| 1.2 | order-service: own DB, `productId` as a plain number |
| 1.3 | Database per service, enforced by MySQL users |
| 1.4 | Service-to-service call, timeouts, no transaction around a remote call |
| 1.5 | One error format (Problem Details), Swagger |

## New in this lesson
| File | Job |
|---|---|
| `scripts/e2e-module1.sh` | Runs about 30 checks (success and failure) against the running services |
| `postman/shopsphere-module1.postman_collection.json` | Same flow in Postman, with tests on every request |

Why automate? Manual testing is slow and you forget cases. A script you can re-run after every change is the beginning of
**regression testing**. In Module 12 we add contract tests and load tests on top of this idea.

## Run it
```bash
docker compose up -d
mvn -pl product-service spring-boot:run     # terminal 1
mvn -pl order-service spring-boot:run       # terminal 2
./scripts/e2e-module1.sh                    # terminal 3
```
Expected: every line `[OK  ]` and `Result: N passed, 0 failed`.

### What the script does
| Step | Checks |
|---|---|
| 0 | Both services healthy (200) |
| 1 | Create product -> 201, get -> 200 |
| 2 | Order 2 items -> 201, get -> 200, filter -> 200, order **exactly all stock** -> 201 |
| 3 | Above stock -> **409**, unknown product -> **422**, invalid body -> **400**, bad JSON -> 400, missing -> 404, bad enum -> 400 |
| 4 | Cancel -> 200, cancel again -> **409**, cancel unknown -> 404, filter CANCELLED -> 200 |
| 5 | Invalid product -> 400, missing -> 404, update -> 200, delete -> 204, delete again -> 404, order for deleted product -> **422** |

The script exits with code 1 if any check fails, so a CI pipeline can use it later (Module 11).

## Postman
1. Postman -> **Import** -> choose `postman/shopsphere-module1.postman_collection.json`.
2. Right-click the collection -> **Run collection**, run folders 1 and 2 in order.
3. Variables `productId` and `orderId` are filled in automatically by test scripts.
4. Folder 3 is manual: stop product-service, send the request, expect **503** in under 4 seconds.

## The manual failure drills (do these once)
These cannot be scripted easily because they need you to stop things.

| # | Drill | What to do | Expected | What it teaches |
|---|---|---|---|---|
| 1 | Dependency down | Ctrl+C product-service, `POST /api/orders` | **503** fast | Timeout + mapped error |
| 2 | Recovery | Start product-service again, retry | **201** | System self-heals |
| 3 | Slow dependency | `kill -STOP <product PID>`, `POST /api/orders` | **503** after ~2s | Read timeout |
| 4 | Own DB down | `docker compose stop mysql`, `GET /api/orders` | **503** "Database unavailable" | DB failure handling |
| 5 | Isolation | `./scripts/verify-db-isolation.sh` | all `OK` | Database per service |

After drill 3 run `kill -CONT <PID>`. After drill 4 run `docker compose start mysql`.

## Known weaknesses (the reason for the next modules)
Write these down. Each one is a later lesson.

| Weakness you can now observe | Fixed in |
|---|---|
| Stock is checked but **never reduced**; two parallel orders can oversell | Module 7 (Saga) |
| URL `http://localhost:8081` is hardcoded | Module 3 (Eureka, Gateway, Config) |
| Every order depends on product-service being up right now; no retry, no circuit breaker | Module 4 (Resilience4j) |
| Anyone can call anything, no login | Module 5 (Keycloak) |
| You start each service by hand | Module 2 (Docker Compose) |
| No visibility across the two services when something fails | Module 9 (tracing) |

## Module 1 self-test (answer without looking)
1. Why does `Order` store `productId` instead of a `Product` object?
2. What does MySQL do if `order_user` reads `product_db`?
3. 404 vs 409 vs 422: when is each used?
4. Why must a remote call have a timeout?
5. Why does `OrderService.create` run without a transaction?
6. Which two lines of defense stop a bad order (validation vs business rule)?
7. Why does the catch-all handler hide the exception message?
8. Which weakness above would break your system first under real traffic?

## Recap card
- Module 1 = two independent services, each with its own database, talking over HTTP.
- One script checks success and failure paths. Re-run it after every change.
- Failures you must be able to explain: dependency down (503), slow dependency (503 after timeout), database down (503).
- Next: Module 2, Lesson 2.1: a Dockerfile for each service (multi-stage build).
