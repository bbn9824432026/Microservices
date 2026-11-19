package com.shopsphere.product.dto;

import com.shopsphere.product.model.Product;

import java.math.BigDecimal;

/**
 * What the client RECEIVES. Separate from the entity so we control exactly what is exposed.
 */
public record ProductResponse(Long id, String name, BigDecimal price, int stock) {

    // Converts an entity to the response shape
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
    }
}
