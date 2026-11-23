package com.shopsphere.order.client;

/**
 * product-service could not be reached, was too slow, or answered with a 5xx error.
 * The GlobalExceptionHandler turns it into HTTP 503.
 */
public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
