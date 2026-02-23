package com.workshop.gateway.client;

import com.workshop.gateway.model.OrderCreateRequest;
import com.workshop.gateway.model.OrderCreateResponse;
import com.workshop.gateway.model.OrderView;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

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

  public Map<String, Object> getAll(int page, int size) {
    return orderRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/orders")
            .queryParam("page", page)
            .queryParam("size", size)
            .build())
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  public OrderView get(String orderId) {
    return orderRestClient.get()
        .uri("/orders/{id}", orderId)
        .retrieve()
        .body(OrderView.class);
  }

  public void retryOrder(String orderId) {
    orderRestClient.post()
        .uri("/orders/{id}/retry", orderId)
        .retrieve()
        .toBodilessEntity();
  }
}
