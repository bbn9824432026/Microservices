package com.shopsphere.product.exception;

/** Thrown when a product id does not exist. The GlobalExceptionHandler turns it into HTTP 404. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found with id " + id);
    }
}
