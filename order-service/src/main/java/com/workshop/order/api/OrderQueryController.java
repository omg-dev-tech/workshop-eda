package com.workshop.order.api;

import com.workshop.order.domain.OrderEntity;
import com.workshop.order.domain.OrderRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderQueryController {
  private final OrderRepository repo;
  public OrderQueryController(OrderRepository repo) { this.repo = repo; }

  @GetMapping("/{id}")
  public OrderEntity get(@PathVariable String id) {
    return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("order not found"));
  }
}
