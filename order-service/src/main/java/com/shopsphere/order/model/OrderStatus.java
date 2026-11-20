package com.shopsphere.order.model;

/** Life cycle of an order. Lesson 1.2 only uses CREATED and CANCELLED; later lessons add more (PAID, FAILED...). */
public enum OrderStatus {
    CREATED,
    CANCELLED
}
