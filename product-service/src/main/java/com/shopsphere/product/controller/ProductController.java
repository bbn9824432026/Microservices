package com.shopsphere.product.controller;

import com.shopsphere.product.dto.ProductRequest;
import com.shopsphere.product.dto.ProductResponse;
import com.shopsphere.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * HTTP layer only: read the request, call the service, return the response.
 * Base path: /api/products
 * @Tag / @Operation / @ApiResponse only add descriptions to the generated Swagger page; they do not change behaviour.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Create, read, update and delete products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // GET /api/products -> 200 + list
    @Operation(summary = "List all products")
    @GetMapping
    public List<ProductResponse> list() {
        return service.findAll();
    }

    // GET /api/products/1 -> 200 + product, or 404 if missing
    @Operation(summary = "Get one product by id")
    @ApiResponse(responseCode = "404", description = "Product does not exist")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    // POST /api/products -> 201 Created + Location header. @Valid triggers the checks in ProductRequest (400 on failure)
    @Operation(summary = "Create a product")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    // PUT /api/products/1 -> 200 + updated product, 404 if missing, 400 if invalid
    @Operation(summary = "Update a product")
    @ApiResponse(responseCode = "404", description = "Product does not exist")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    // DELETE /api/products/1 -> 204 No Content, or 404 if missing
    @Operation(summary = "Delete a product")
    @ApiResponse(responseCode = "404", description = "Product does not exist")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
