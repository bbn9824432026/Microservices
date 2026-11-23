package com.shopsphere.order.exception;

/** The order's current status does not allow the action (e.g. cancelling twice) -> HTTP 409. */
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
