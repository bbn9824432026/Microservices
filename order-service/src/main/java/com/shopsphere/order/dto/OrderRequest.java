package com.shopsphere.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** JSON the client sends to place an order. Violations of these rules return HTTP 400. */
public record OrderRequest(

        @NotBlank(message = "customerName must not be blank")
        @Size(max = 100, message = "customerName must be at most 100 characters")
        String customerName,

        @NotNull(message = "productId is required")
        @Positive(message = "productId must be greater than 0")
        Long productId,

        @Min(value = 1, message = "quantity must be at least 1")
        int quantity
) {
}
