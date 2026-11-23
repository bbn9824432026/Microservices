package com.shopsphere.order.exception;

/** Requested quantity is more than the available stock -> HTTP 409. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Not enough stock for product " + productId
                + ": requested " + requested + ", available " + available);
    }
}
