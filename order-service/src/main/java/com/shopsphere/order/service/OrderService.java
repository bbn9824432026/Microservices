package com.shopsphere.order.service;

import com.shopsphere.order.client.ProductClient;
import com.shopsphere.order.client.ProductDto;
import com.shopsphere.order.dto.OrderRequest;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.exception.InsufficientStockException;
import com.shopsphere.order.exception.InvalidOrderStateException;
import com.shopsphere.order.exception.OrderNotFoundException;
import com.shopsphere.order.exception.ProductDoesNotExistException;
import com.shopsphere.order.model.Order;
import com.shopsphere.order.model.OrderStatus;
import com.shopsphere.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for orders. One public method = one transaction.
 *
 * Since Lesson 1.4, create() asks product-service whether the product exists and has enough stock.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final ProductClient productClient;

    public OrderService(OrderRepository repository, ProductClient productClient) {
        this.repository = repository;
        this.productClient = productClient;
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

    /**
     * NOT_SUPPORTED = run without a transaction. We must not hold a database connection open
     * while waiting for a network call to another service (it could take seconds or time out).
     * Order of work: 1) ask product-service  2) only then save (repository.save has its own short transaction).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderResponse create(OrderRequest request) {
        ProductDto product = productClient.getProduct(request.productId())
                .orElseThrow(() -> new ProductDoesNotExistException(request.productId()));

        if (product.stock() < request.quantity()) {
            throw new InsufficientStockException(product.id(), request.quantity(), product.stock());
        }

        // NOTE: we only CHECK stock here, we do not reduce it. Two orders arriving together could both pass
        // this check. Fixing that across two services needs the Saga pattern (Lesson 7).
        Order saved = repository.save(new Order(request.customerName(), request.productId(), request.quantity()));
        return OrderResponse.from(saved);
    }

    public OrderResponse cancel(Long id) {
        Order order = getOrThrow(id);
        try {
            order.cancel(); // the entity enforces the rule; Hibernate saves the change on commit
        } catch (IllegalStateException e) {
            // Translate the entity's plain Java exception into our business exception (-> HTTP 409)
            throw new InvalidOrderStateException(e.getMessage());
        }
        return OrderResponse.from(order);
    }

    private Order getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
