package io.tt.controller;

import io.tt.model.Order;
import io.tt.repository.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping(value = "order")
    public void create(Order order){
        orderService.take(order);
    }
}
