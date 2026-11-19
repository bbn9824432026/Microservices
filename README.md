# ShopSphere: Learn Microservices by Building

A mini e-commerce system built lesson by lesson with **Java 17, Spring Boot, Maven and MySQL**.
Every lesson has a notes file in `docs/` and is committed separately.

## Lessons
| # | Lesson | Notes |
|---|---|---|
| 0 | Setup and architecture | [docs/00-setup-and-architecture.md](docs/00-setup-and-architecture.md) |
| 1.1 | product-service (Spring Boot + JPA + MySQL) | [docs/01-1-product-service.md](docs/01-1-product-service.md) |

(More lessons are added as we go.)

## Commands
Frequently used Docker, Maven, MySQL and curl commands: [commands/README.md](commands/README.md)

## Quick start
Requires JDK 25 (install steps in the commands file).
```bash
docker compose up -d     # starts MySQL on localhost:3307
mvn clean package        # builds all services
```
