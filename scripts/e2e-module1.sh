#!/usr/bin/env bash
# Lesson 1.6 checkpoint: runs the whole Module 1 flow against the RUNNING services
# and checks every HTTP status code. Prints OK / FAIL per step and a summary.
#
# Needs: MySQL, product-service (8081) and order-service (8082) running. No jq needed.
# Usage: ./scripts/e2e-module1.sh

PRODUCT=http://localhost:8081
ORDER=http://localhost:8082
JSON="Content-Type: application/json"
PASS=0
FAIL=0
BODY=""

# call <METHOD> <URL> [json body] -> sets $BODY (response body) and echoes the HTTP status code
call() {
  local method=$1 url=$2 data=$3 out
  if [ -n "$data" ]; then
    out=$(curl -s -w '\n%{http_code}' -X "$method" -H "$JSON" -d "$data" "$url")
  else
    out=$(curl -s -w '\n%{http_code}' -X "$method" "$url")
  fi
  BODY=$(echo "$out" | sed '$d')   # everything except the last line
  echo "$out" | tail -n1           # last line = status code
}

# check <description> <expected status> <actual status>
check() {
  if [ "$2" = "$3" ]; then
    echo "[OK  ] $1 -> $3"; PASS=$((PASS + 1))
  else
    echo "[FAIL] $1 -> got $3, expected $2"; echo "       body: $BODY"; FAIL=$((FAIL + 1))
  fi
}

# extract the first "id":<number> from the last response body
get_id() { echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }

echo "== Step 0: both services must be up =="
check "product-service health" 200 "$(call GET $PRODUCT/actuator/health)"
check "order-service health"   200 "$(call GET $ORDER/actuator/health)"

echo "== Step 1: create a product with stock 10 =="
check "create product" 201 "$(call POST $PRODUCT/api/products '{"name":"E2E Laptop","price":55000.50,"stock":10}')"
PID=$(get_id)
echo "       product id = $PID"
check "get product" 200 "$(call GET $PRODUCT/api/products/$PID)"

echo "== Step 2: place orders (success) =="
check "order 2 items" 201 "$(call POST $ORDER/api/orders "{\"customerName\":\"E2E User\",\"productId\":$PID,\"quantity\":2}")"
OID=$(get_id)
echo "       order id = $OID"
check "get order"  200 "$(call GET $ORDER/api/orders/$OID)"
check "list orders by customer" 200 "$(call GET "$ORDER/api/orders?customerName=E2E%20User")"
check "order exactly all stock (10)" 201 "$(call POST $ORDER/api/orders "{\"customerName\":\"E2E User\",\"productId\":$PID,\"quantity\":10}")"

echo "== Step 3: order failures =="
check "quantity above stock"    409 "$(call POST $ORDER/api/orders "{\"customerName\":\"E2E User\",\"productId\":$PID,\"quantity\":11}")"
check "unknown product"         422 "$(call POST $ORDER/api/orders '{"customerName":"E2E User","productId":99999999,"quantity":1}')"
check "invalid body (3 errors)" 400 "$(call POST $ORDER/api/orders '{"customerName":"","productId":0,"quantity":0}')"
echo "       $BODY"
check "malformed JSON"          400 "$(call POST $ORDER/api/orders '{"customerName":')"
check "order not found"         404 "$(call GET $ORDER/api/orders/99999999)"
check "bad status filter"       400 "$(call GET "$ORDER/api/orders?status=ABC")"

echo "== Step 4: cancel =="
check "cancel order"            200 "$(call POST $ORDER/api/orders/$OID/cancel)"
check "cancel again (conflict)" 409 "$(call POST $ORDER/api/orders/$OID/cancel)"
check "cancel unknown order"    404 "$(call POST $ORDER/api/orders/99999999/cancel)"
check "filter CANCELLED"        200 "$(call GET "$ORDER/api/orders?status=CANCELLED")"

echo "== Step 5: product failures and cleanup =="
check "invalid product"         400 "$(call POST $PRODUCT/api/products '{"name":"","price":-1,"stock":-1}')"
check "product not found"       404 "$(call GET $PRODUCT/api/products/99999999)"
check "update product"          200 "$(call PUT $PRODUCT/api/products/$PID '{"name":"E2E Laptop v2","price":60000,"stock":5}')"
check "delete product"          204 "$(call DELETE $PRODUCT/api/products/$PID)"
check "delete product again"    404 "$(call DELETE $PRODUCT/api/products/$PID)"
check "order for deleted product" 422 "$(call POST $ORDER/api/orders "{\"customerName\":\"E2E User\",\"productId\":$PID,\"quantity\":1}")"

echo
echo "Result: $PASS passed, $FAIL failed"
echo "Not covered here (do it by hand, see the lesson notes): stop product-service and expect 503 on POST /api/orders."
[ "$FAIL" -eq 0 ]
