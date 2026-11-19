package com.shopsphere.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of product-service.
 * @SpringBootApplication turns on auto-configuration and scans this package
 * (com.shopsphere.product and all sub-packages) for controllers, services and entities.
 */
@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
