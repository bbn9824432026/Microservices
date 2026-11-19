package com.shopsphere.product.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * The database row. @Entity maps this class to the "products" table in product_db.
 * This class never leaves the service: the API uses DTOs (see the dto package),
 * so changing the table later does not break API consumers.
 */
@Entity
@Table(name = "products")
public class Product {

    // AUTO_INCREMENT primary key; MySQL generates the value
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // BigDecimal for money: double/float give rounding errors (0.1 + 0.2 != 0.3)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // How many items are available. Order-service will use this in later lessons.
    @Column(nullable = false)
    private int stock;

    // JPA requires a no-arg constructor
    protected Product() {
    }

    public Product(String name, BigDecimal price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }

    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
}
