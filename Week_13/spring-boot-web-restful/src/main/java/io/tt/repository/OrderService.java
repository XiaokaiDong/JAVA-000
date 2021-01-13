package io.tt.repository;

import io.tt.model.Order;

public interface OrderService {
    void sequencing(Order order);
    void match(Order order);
}
