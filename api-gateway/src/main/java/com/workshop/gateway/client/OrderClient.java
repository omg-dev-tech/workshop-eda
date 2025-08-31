package com.workshop.gateway.client;

import com.workshop.gateway.model.OrderCreateRequest;
import com.workshop.gateway.model.OrderCreateResponse;
import com.workshop.gateway.model.OrderView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OrderClient {
  private final RestClient orderRestClient;

  public OrderCreateResponse create(OrderCreateRequest req, String forcePaymentHeader) {
    var builder = orderRestClient.post()
        .uri("/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .body(req);
    if (forcePaymentHeader != null && !forcePaymentHeader.isBlank()) {
      builder = builder.header("X-Force-Payment", forcePaymentHeader);
    }
    return builder.retrieve().body(OrderCreateResponse.class);
  }

  public OrderView get(String orderId) {
    return orderRestClient.get()
        .uri("/orders/{id}", orderId)
        .retrieve()
        .body(OrderView.class);
  }
}
