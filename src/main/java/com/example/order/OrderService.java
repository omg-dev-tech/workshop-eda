package com.example.order;

import org.springframework.stereotype.Service;

@Service
public class OrderService {
    

    public OrderDto order () {
        return new OrderDto();
    }
}
