package com.shopsphere.order.client;

import java.math.BigDecimal;

/**
 * order-service's OWN copy of "what a product looks like in product-service's API".
 * We do not import product-service's classes: services share an API contract (JSON), not code.
 * Extra JSON fields sent by product-service are ignored; only the fields we need are listed.
 */
public record ProductDto(Long id, String name, BigDecimal price, int stock) {
}
