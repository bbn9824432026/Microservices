-- Database-per-service: each service gets its OWN database and its OWN user.
-- A service's user has no rights on other services' databases, so the rule
-- "no shared database" is enforced by MySQL itself, not just by convention.

CREATE DATABASE IF NOT EXISTS product_db;
CREATE DATABASE IF NOT EXISTS order_db;

CREATE USER IF NOT EXISTS 'product_user'@'%' IDENTIFIED BY 'product_pass';
CREATE USER IF NOT EXISTS 'order_user'@'%'   IDENTIFIED BY 'order_pass';

GRANT ALL PRIVILEGES ON product_db.* TO 'product_user'@'%';
GRANT ALL PRIVILEGES ON order_db.*   TO 'order_user'@'%';

FLUSH PRIVILEGES;
