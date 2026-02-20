package com.workshop.order.api;

import com.workshop.order.api.dto.CreateOrderReq;
import com.workshop.order.api.dto.CreateOrderRes;
import com.workshop.order.domain.OrderEntity;
import com.workshop.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  public CreateOrderRes create(@RequestBody CreateOrderReq req) {
    OrderEntity saved = service.create(req);
    return new CreateOrderRes(saved.getId().toString(), saved.getStatus().name());
  }

  @GetMapping("/healthz")
  public String health() { return "order-service ok"; }
}
