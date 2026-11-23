# ShopSphere: Learn Microservices by Building

A mini e-commerce system built lesson by lesson with **Java 17, Spring Boot, Maven and MySQL**.
Every lesson has a notes file in `docs/` and is committed separately.

## Lessons
| # | Lesson | Notes |
|---|---|---|
| 0 | Setup and architecture | [docs/00-setup-and-architecture.md](docs/00-setup-and-architecture.md) |
| 1.1 | product-service (Spring Boot + JPA + MySQL) | [docs/01-1-product-service.md](docs/01-1-product-service.md) |
| 1.2 | order-service (own database) | [docs/01-2-order-service.md](docs/01-2-order-service.md) |
| 1.3 | Database per service (rule + proof) | [docs/01-3-database-per-service.md](docs/01-3-database-per-service.md) |
| 1.4 | Service-to-service calls with RestClient | [docs/01-4-service-to-service-restclient.md](docs/01-4-service-to-service-restclient.md) |
| 1.5 | Global exception handling, validation, OpenAPI | [docs/01-5-exception-handling-and-openapi.md](docs/01-5-exception-handling-and-openapi.md) |

(More lessons are added as we go.)

## Commands
Frequently used Docker, Maven, MySQL and curl commands: [commands/README.md](commands/README.md)

## Quick start
Requires JDK 25 (install steps in the commands file).
```bash
docker compose up -d     # starts MySQL on localhost:3307
mvn clean package        # builds all services
```
