package com.shopsphere.order.controller;

import com.shopsphere.order.dto.OrderRequest;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.model.OrderStatus;
import com.shopsphere.order.service.OrderService;
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

/** HTTP layer for orders. Base path: /api/orders */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    // GET /api/orders, /api/orders?customerName=Asha, /api/orders?status=CANCELLED
    // An invalid status value (e.g. ?status=ABC) gives 400 automatically.
    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) String customerName,
                                    @RequestParam(required = false) OrderStatus status) {
        return service.findAll(customerName, status);
    }

    // GET /api/orders/1 -> 200, or 404
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    // POST /api/orders -> 201 + Location header, 400 if invalid
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    // POST /api/orders/1/cancel -> 200, 404 if missing, 409 if already cancelled.
    // POST (not DELETE): we keep the order and change its state; nothing is removed.
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
