package com.shopsphere.order.exception;

/** The order refers to a product that product-service says does not exist -> HTTP 422. */
public class ProductDoesNotExistException extends RuntimeException {

    public ProductDoesNotExistException(Long productId) {
        super("Product " + productId + " does not exist");
    }
}
