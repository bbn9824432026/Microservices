#!/usr/bin/env bash
# Lesson 1.3: proves "database per service" is enforced by MySQL itself.
# Usage: ./scripts/verify-db-isolation.sh   (MySQL container must be running)
# Each check runs a query as one service's user and reports whether MySQL allowed it.

CONTAINER=shopsphere-mysql

# run_as <user> <password> <sql> <expectation: ALLOWED|DENIED> <description>
run_as() {
  local user=$1 pass=$2 sql=$3 expect=$4 desc=$5
  if docker exec "$CONTAINER" mysql -u"$user" -p"$pass" -e "$sql" >/dev/null 2>&1; then
    result=ALLOWED
  else
    result=DENIED
  fi
  if [ "$result" = "$expect" ]; then status="OK  "; else status="FAIL"; fi
  echo "[$status] $desc -> $result (expected $expect)"
}

echo "--- Each service on its OWN database: must be allowed ---"
run_as product_user product_pass "SELECT 1 FROM product_db.products LIMIT 1;" ALLOWED "product_user reads product_db.products"
run_as order_user   order_pass   "SELECT 1 FROM order_db.orders LIMIT 1;"     ALLOWED "order_user reads order_db.orders"

echo "--- A service on ANOTHER service's database: must be denied ---"
run_as order_user   order_pass   "SELECT * FROM product_db.products;"         DENIED  "order_user reads product_db.products"
run_as product_user product_pass "SELECT * FROM order_db.orders;"             DENIED  "product_user reads order_db.orders"
run_as order_user   order_pass   "USE product_db;"                            DENIED  "order_user opens product_db"

echo
echo "Note: the two ALLOWED checks need the tables to exist, so start each service once first."
