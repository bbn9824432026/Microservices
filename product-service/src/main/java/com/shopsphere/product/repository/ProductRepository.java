package com.shopsphere.product.repository;

import com.shopsphere.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data generates the implementation at startup.
 * Free methods: save, findById, findAll, deleteById, existsById, count ...
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
