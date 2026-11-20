package com.shopsphere.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A row in order_db.orders. (The table is called "orders" because ORDER is a reserved SQL word.)
 *
 * KEY MICROSERVICES IDEA: productId is a plain number, NOT a @ManyToOne to Product.
 * Product lives in another service's database, so there is no foreign key and no JPA relation.
 * Order-service only stores the id and asks product-service about it through its API (Lesson 1.4).
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String customerName;

    // Reference to a product in ANOTHER service. No foreign key possible across databases.
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    // Store the enum as text (CREATED) not as a number, so reordering the enum cannot corrupt data
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Order() {
    }

    public Order(String customerName, Long productId, int quantity) {
        this.customerName = customerName;
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderStatus.CREATED;
        this.createdAt = Instant.now();
    }

    /**
     * Business rule lives in the entity: only a CREATED order can be cancelled.
     * Throws IllegalStateException otherwise; the service turns that into HTTP 409.
     */
    public void cancel() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Order " + id + " cannot be cancelled, current status is " + status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
