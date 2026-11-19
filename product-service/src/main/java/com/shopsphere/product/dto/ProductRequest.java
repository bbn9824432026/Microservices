package com.shopsphere.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * What the client SENDS when creating or updating a product.
 * A record is an immutable data holder. The annotations are checked when the controller
 * parameter is marked @Valid; a violation makes Spring return HTTP 400 automatically.
 */
public record ProductRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0")
        BigDecimal price,

        @Min(value = 0, message = "stock must not be negative")
        int stock
) {
}
