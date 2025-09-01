package com.workshop.order.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class PaymentClient {

  private final RestClient rest = RestClient.create();

  @Value("${payment.base-url}")
  private String baseUrl;

  public PaymentResponse authorize(PaymentRequest req) {
    try {
      return rest.post()
          .uri(baseUrl + "/payments/authorize")
          .contentType(MediaType.APPLICATION_JSON)
          .body(req)
          .retrieve()
          .body(PaymentResponse.class);

    } catch (RestClientResponseException ex) {
      // 실패(401 등)를 PaymentResponse(DECLINED)로 변환
      String reason = ex.getResponseBodyAsString();
      if (reason == null || reason.isBlank()) {
        reason = ex.getStatusText(); // e.g., "Unauthorized"
      }
      return new PaymentResponse(
          "DECLINED",
          null,
          reason,
          System.currentTimeMillis()
      );
    }
  }
}
