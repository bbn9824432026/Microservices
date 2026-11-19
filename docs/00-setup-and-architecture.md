# Lesson 0: Setup and Architecture

## What is a microservice? (simple words)
Instead of one big application (a **monolith**), you build many **small applications**.
Each one does one job, has its own database, and can be deployed on its own.

| | Monolith | Microservices |
|---|---|---|
| Deploy | Everything together | Each service alone |
| Database | One shared | One per service |
| A bug/crash | Can take down everything | Usually stays in one service |
| Complexity | In the code | In the network between services |

**Remember:** microservices do not remove complexity. They **move it from the code to the network**.
Network calls can be slow, fail, or time out. Most of this course is about handling that.

## What we are building: ShopSphere

A mini e-commerce system. We grow it lesson by lesson.

```
            Client (Postman)
                  |
   +--------------+--------------+
   |                             |
product-service            order-service
   |                             |
 product_db                  order_db        <- two separate databases
   \_____________  MySQL  _____________/
```

Later lessons add: Docker, Gateway, Eureka, Resilience4j, Keycloak, Kafka, Saga,
payment/notification services, observability and Kubernetes.

## Rules we follow
1. **One service = one database.** A service never reads another service's tables.
2. **Services talk through APIs (HTTP) or events (Kafka)**, never through the database.
3. **No shared code that holds business logic** between services.

## Tools used
| Tool | Version | Purpose |
|---|---|---|
| JDK | 17 | Java runtime (Spring Boot 3.3 supports 17+) |
| Maven | 3.9 | Build |
| Spring Boot | 3.3.5 | Framework |
| MySQL | 8.4 (Docker) | Database |
| Docker | any recent | Runs MySQL |
| Postman or curl | any | Call the APIs |

## Project layout
```
Microservices/
├── pom.xml                 <- parent POM (shared versions, lists the modules)
├── docker-compose.yml      <- MySQL for all services
├── infra/mysql/init.sql    <- creates one DB + one user per service
├── docs/                   <- one .md file per lesson (these notes)
├── product-service/        <- added in Lesson 1.1
└── order-service/          <- added in Lesson 1.2
```

## Why a parent POM?
It gives every service the **same Spring Boot version** and lets you build all services with one
command from the root: `mvn clean package`.

## Why separate DB users?
`init.sql` creates `product_user` (only for `product_db`) and `order_user` (only for `order_db`).
If order-service tried to read `product_db`, **MySQL would refuse**.
The rule is enforced by the database, not only by good intentions.

## Hands-on: start MySQL
```bash
docker compose up -d
docker compose ps            # wait until STATUS says "healthy"
```
Connect to check (host port **3307**, because 3306 may be used by a local MySQL):
```bash
docker exec -it shopsphere-mysql mysql -uroot -proot -e "SHOW DATABASES;"
```
You should see `product_db` and `order_db`.

### Try the failure case
Prove that databases are isolated:
```bash
docker exec -it shopsphere-mysql mysql -uorder_user -porder_pass -e "USE product_db;"
```
Expected: `ERROR 1044 ... Access denied for user 'order_user'`.

## Recap card
- Microservice = small app, own DB, deployed alone.
- Complexity moves from code to network.
- Database per service, enforced with separate DB users.
- MySQL runs in Docker on host port 3307.
