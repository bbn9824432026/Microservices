package com.shopsphere.order.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * product-service could not be reached, was too slow, or answered with a 5xx error.
 * @ResponseStatus makes Spring return HTTP 503 (instead of a generic 500) when this escapes a controller.
 * Lesson 1.5 replaces this with a global exception handler.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
