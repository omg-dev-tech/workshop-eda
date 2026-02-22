package com.workshop.order.api;

import com.workshop.order.api.dto.CreateOrderReq;
import com.workshop.order.api.dto.CreateOrderRes;
import com.workshop.order.domain.OrderEntity;
import com.workshop.order.domain.OrderRepository;
import com.workshop.order.service.OrderService;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderService service;
  private final OrderRepository repo;

  public OrderController(OrderService service, OrderRepository repo) {
    this.service = service;
    this.repo = repo;
  }

  // 주문 생성
  @PostMapping
  public CreateOrderRes create(@RequestBody CreateOrderReq req) {
    OrderEntity saved = service.create(req);
    return new CreateOrderRes(saved.getId().toString(), saved.getStatus().name());
  }

  // 주문 목록 조회
  @GetMapping
  public List<OrderEntity> getAll() {
    return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  // 주문 상세 조회
  @GetMapping("/{id}")
  public OrderEntity get(@PathVariable String id) {
    UUID uuid = UUID.fromString(id);
    return repo.findById(uuid).orElseThrow(() -> new IllegalArgumentException("order not found"));
  }

  // 주문 재처리 (INVENTORY_REJECTED 상태만 가능)
  @PostMapping("/{id}/retry")
  public OrderEntity retry(@PathVariable String id) {
    UUID uuid = UUID.fromString(id);
    return service.retry(uuid);
  }

  @GetMapping("/healthz")
  public String health() { return "order-service ok"; }
}
