package com.shopsphere.order.exception;

/** Order id does not exist -> HTTP 404. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("Order not found with id " + id);
    }
}
