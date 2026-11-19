package com.shopsphere.product.service;

import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.model.Product;
import com.shopsphere.product.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business logic lives here, not in the controller.
 * Each public method is one transaction (class-level @Transactional);
 * read-only methods are marked so Hibernate can skip dirty-checking.
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository repository;

    // Constructor injection: Spring passes the repository in
    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return repository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getOrThrow(id));
    }

    public ProductResponse create(ProductRequest request) {
        Product saved = repository.save(new Product(request.name(), request.price(), request.stock()));
        return ProductResponse.from(saved);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getOrThrow(id);
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        // No save() call needed: the entity is managed, so Hibernate writes the UPDATE on commit
        return ProductResponse.from(product);
    }

    public void delete(Long id) {
        // Check first so deleting a missing id gives a clear 404 instead of silently succeeding
        getOrThrow(id);
        repository.deleteById(id);
    }

    private Product getOrThrow(Long id) {
        // Temporary 404 handling. Lesson 1.5 replaces this with a global exception handler.
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id " + id));
    }
}
