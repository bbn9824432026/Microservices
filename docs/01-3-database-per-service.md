# Lesson 1.3: Database per Service (why no shared database)

## Goal
Understand the most important microservices rule, and **prove** it with MySQL.

## The rule (simple words)
> Every service owns its data. Nobody else touches its tables. Others must ask through its API.

```
BAD  (shared database)                 GOOD (database per service)

product-svc   order-svc                product-svc        order-svc
      \         /                            |                 |
     one big database                  product_db          order_db
```

## Why not share one database?
| Problem with a shared DB | What happens |
|---|---|
| **Hidden coupling** | Change a column in `products` and `order-service` breaks, with no warning. |
| **No independent deploys** | A schema change forces every service to deploy together. You rebuilt a monolith. |
| **No independent scaling** | One heavy service slows down everyone's queries. |
| **One technology** | You cannot give one service Redis or MongoDB if all share MySQL. |
| **Unclear ownership** | Who is allowed to change this table? Nobody knows. |
| **Shared failure** | One bad query or lock hurts every service. |

**Remember:** the database is a service's **private detail**. The API is its **public promise**.

## What we gave up (the honest part)
In a monolith you get these for free. With separate databases you do not:

| Lost feature | Replacement (later lessons) |
|---|---|
| SQL `JOIN` across tables | Call the other API (Lesson 1.4), or keep a local copy of needed data (CQRS, Lesson 8) |
| Foreign key checks | The service validates through an API call |
| One `@Transactional` over both | Saga and Outbox patterns (Lesson 7) |
| Easy reports across data | Read models, events |

This is the **price** of independence. Every later pattern in this course exists because of this one rule.

## How we enforce it in ShopSphere
`infra/mysql/init.sql` creates one database and one user per service:

| Service | Database | User | Can access |
|---|---|---|---|
| product-service | `product_db` | `product_user` | only `product_db` |
| order-service | `order_db` | `order_user` | only `order_db` |

Even if a developer writes bad code, **MySQL refuses** it. Enforcement by the system beats good intentions.

Note: both databases run on **one MySQL server** here, for simplicity. In production each service often gets its
own server or cluster. The rule is about **ownership and access**, not the number of servers.

## Rules checklist
1. One service = one database (or schema) = one DB user.
2. No cross-database `JOIN`, no cross-database foreign key.
3. Store another service's data as an **id only** (like `Order.productId`).
4. Need its data? Call its API, or listen to its events.
5. Never "just read their table, it's faster".

## Hands-on: prove the isolation
Start MySQL, then start each service once so the tables exist:
```bash
docker compose up -d
mvn -pl product-service spring-boot:run     # wait for startup, then Ctrl+C
mvn -pl order-service spring-boot:run       # wait for startup, then Ctrl+C
```
Run the script:
```bash
chmod +x scripts/verify-db-isolation.sh
./scripts/verify-db-isolation.sh
```
Expected output:
```
--- Each service on its OWN database: must be allowed ---
[OK  ] product_user reads product_db.products -> ALLOWED (expected ALLOWED)
[OK  ] order_user reads order_db.orders -> ALLOWED (expected ALLOWED)
--- A service on ANOTHER service's database: must be denied ---
[OK  ] order_user reads product_db.products -> DENIED (expected DENIED)
[OK  ] product_user reads order_db.orders -> DENIED (expected DENIED)
[OK  ] order_user opens product_db -> DENIED (expected DENIED)
```

### Break the rule on purpose (failure scenario)
Give `order_user` access to `product_db`, run the script again, and see the `FAIL` lines:
```bash
docker exec -it shopsphere-mysql mysql -uroot -proot -e "GRANT SELECT ON product_db.* TO 'order_user'@'%';"
./scripts/verify-db-isolation.sh
```
Now `order-service` could read product tables directly. Nothing stops it. That is how coupling creeps in.

Undo it:
```bash
docker exec -it shopsphere-mysql mysql -uroot -proot -e "REVOKE SELECT ON product_db.* FROM 'order_user'@'%';"
./scripts/verify-db-isolation.sh
```

### Manual checks (same idea, one by one)
```bash
docker exec -it shopsphere-mysql mysql -uorder_user -porder_pass -e "SELECT * FROM product_db.products;"
# ERROR 1142 (42000): SELECT command denied to user 'order_user'...
docker exec -it shopsphere-mysql mysql -uorder_user -porder_pass -e "SHOW DATABASES;"
# order_user only sees order_db (and information_schema)
```

## Interview questions this answers
- **Why database per service?** Loose coupling, independent deploys and scaling, clear ownership.
- **How do you join data across services?** API composition or a local read model (CQRS), never a cross-DB join.
- **How do you keep data consistent?** Eventual consistency with Saga and Outbox.
- **Is a shared database ever OK?** Only as a short step while migrating a monolith. It is a known anti-pattern.

## Recap card
- Own your data, expose it only through the API.
- Shared DB = hidden coupling = a distributed monolith.
- We lose joins, foreign keys and shared transactions. Later lessons replace them.
- ShopSphere enforces the rule with separate MySQL users.
- Next: Lesson 1.4, `order-service` calls `product-service` with `RestClient`.
