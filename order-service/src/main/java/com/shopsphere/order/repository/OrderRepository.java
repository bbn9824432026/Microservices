package com.shopsphere.order.repository;

import com.shopsphere.order.model.Order;
import com.shopsphere.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Derived query: Spring builds "select ... where customer_name = ?" from the method name
    List<Order> findByCustomerName(String customerName);

    List<Order> findByStatus(OrderStatus status);
}
