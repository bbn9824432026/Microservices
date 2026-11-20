package com.shopsphere.order.dto;

import com.shopsphere.order.model.Order;
import com.shopsphere.order.model.OrderStatus;

import java.time.Instant;

/** JSON returned to the client. */
public record OrderResponse(Long id, String customerName, Long productId, int quantity,
                            OrderStatus status, Instant createdAt) {

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerName(), o.getProductId(),
                o.getQuantity(), o.getStatus(), o.getCreatedAt());
    }
}
