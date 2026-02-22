package com.workshop.order.api;

import com.workshop.order.domain.OrderEntity;
import com.workshop.order.domain.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderQueryController {
  private final OrderRepository repo;
  public OrderQueryController(OrderRepository repo) { this.repo = repo; }

  @GetMapping
  public List<OrderEntity> getAll() {
    return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  @GetMapping("/{id}")
  public OrderEntity get(@PathVariable String id) {
    UUID uuid = UUID.fromString(id);
    return repo.findById(uuid).orElseThrow(() -> new IllegalArgumentException("order not found"));
  }
}
