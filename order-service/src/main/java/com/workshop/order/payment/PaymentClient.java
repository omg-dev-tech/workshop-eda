package com.workshop.order.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PaymentClient {

  private final RestClient rest = RestClient.create();

  @Value("${payment.base-url}")
  private String baseUrl;

  public PaymentResponse authorize(PaymentRequest req) {
    return rest.post()
        .uri(baseUrl + "/payments/authorize")
        .contentType(MediaType.APPLICATION_JSON)
        .body(req)
        .retrieve()
        .body(PaymentResponse.class);
  }
}
