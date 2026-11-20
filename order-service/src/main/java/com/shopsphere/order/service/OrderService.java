package com.shopsphere.order.service;

import com.shopsphere.order.dto.OrderRequest;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.model.Order;
import com.shopsphere.order.model.OrderStatus;
import com.shopsphere.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business logic for orders. One public method = one transaction.
 *
 * NOTE: we do NOT check that the product exists yet. That needs a call to product-service,
 * which is added in Lesson 1.4. For now any positive productId is accepted.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    /** Optional filters: ?customerName=Asha or ?status=CANCELLED. */
    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(String customerName, OrderStatus status) {
        List<Order> orders;
        if (customerName != null) {
            orders = repository.findByCustomerName(customerName);
        } else if (status != null) {
            orders = repository.findByStatus(status);
        } else {
            orders = repository.findAll();
        }
        return orders.stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrThrow(id));
    }

    public OrderResponse create(OrderRequest request) {
        Order saved = repository.save(new Order(request.customerName(), request.productId(), request.quantity()));
        return OrderResponse.from(saved);
    }

    public OrderResponse cancel(Long id) {
        Order order = getOrThrow(id);
        try {
            order.cancel(); // the entity enforces the rule; Hibernate saves the change on commit
        } catch (IllegalStateException e) {
            // 409 Conflict: the request is valid but clashes with the current state of the order
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return OrderResponse.from(order);
    }

    private Order getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with id " + id));
    }
}
