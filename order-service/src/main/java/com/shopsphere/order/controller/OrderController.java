package com.shopsphere.order.controller;

import com.shopsphere.order.dto.OrderRequest;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.model.OrderStatus;
import com.shopsphere.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** HTTP layer for orders. Base path: /api/orders. Annotations from io.swagger only add Swagger descriptions. */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Place, view and cancel orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    // GET /api/orders, /api/orders?customerName=Asha, /api/orders?status=CANCELLED
    // An invalid status value (e.g. ?status=ABC) gives 400 automatically.
    @Operation(summary = "List orders, optionally filtered by customerName or status")
    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) String customerName,
                                    @RequestParam(required = false) OrderStatus status) {
        return service.findAll(customerName, status);
    }

    // GET /api/orders/1 -> 200, or 404
    @Operation(summary = "Get one order by id")
    @ApiResponse(responseCode = "404", description = "Order does not exist")
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    // POST /api/orders -> 201 + Location header, 400 if invalid
    @Operation(summary = "Place an order (checks the product and stock in product-service)")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Not enough stock")
    @ApiResponse(responseCode = "422", description = "Product does not exist")
    @ApiResponse(responseCode = "503", description = "product-service unavailable")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    // POST /api/orders/1/cancel -> 200, 404 if missing, 409 if already cancelled.
    // POST (not DELETE): we keep the order and change its state; nothing is removed.
    @Operation(summary = "Cancel an order")
    @ApiResponse(responseCode = "404", description = "Order does not exist")
    @ApiResponse(responseCode = "409", description = "Order is already cancelled")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
