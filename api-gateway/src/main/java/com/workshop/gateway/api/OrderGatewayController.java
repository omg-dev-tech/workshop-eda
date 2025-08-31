// src/main/java/com/workshop/gateway/api/OrderGatewayController.java
package com.workshop.gateway.api;

import com.workshop.gateway.client.OrderClient;
import com.workshop.gateway.model.OrderCreateRequest;
import com.workshop.gateway.model.OrderCreateResponse;
import com.workshop.gateway.model.OrderView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderGatewayController {

  private final OrderClient orders;

  // 주문 생성(강제 결제 결과를 테스트하려면 X-Force-Payment: success|fail)
  @PostMapping("/orders")
  public OrderCreateResponse create(
      @Valid @RequestBody OrderCreateRequest req,
      @RequestHeader(value = "X-Force-Payment", required = false) String forcePayment
  ) {
    return orders.create(req, forcePayment);
  }

  // 주문 조회(상태 확인)
  @GetMapping("/orders/{id}")
  public OrderView get(@PathVariable String id) {
    return orders.get(id);
  }
}
