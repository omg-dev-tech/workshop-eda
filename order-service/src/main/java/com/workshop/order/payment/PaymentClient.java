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

  private static final String PAYMENT_AUTHORIZE_ENDPOINT = "/payments/authorize";
  private static final String FORCE_PAYMENT_HEADER = "X-Force-Payment";
  private static final String FORCE_PAYMENT_VALUE = "fail";
  private static final String DECLINED_STATUS = "DECLINED";
  private static final String ACCEPT_HEADER = "Accept";
  private static final String APPLICATION_JSON = "application/json";

  private final RestClient rest = RestClient.create();

  @Value("${payment.base-url}")
  private String baseUrl;

  // feature flag: true면 401을 예외로 던져 Instana Error Call 유도
  @Value("${payment.error-mode:false}")
  private boolean errorMode;

  /**
   * 실패를 강제로 유도하고 싶으면 forceFail=true 로 호출
   *  - 서버는 X-Force-Payment: fail 또는 ?force=fail 을 인지해 실패(401) 응답
   *  - errorMode=true면 예외를 그대로 던져 Instana에 에러로 기록
   *  - errorMode=false면 기존처럼 DECLINED 매핑
   */
  public PaymentResponse authorize(PaymentRequest req, boolean forceFail) {
    String url = buildPaymentUrl(PAYMENT_AUTHORIZE_ENDPOINT);
    
    try {
      var requestSpec = buildRequestSpec(url, forceFail);
      
      return requestSpec
          .body(req)
          .retrieve()
          .body(PaymentResponse.class);
          
    } catch (RestClientResponseException ex) {
      handleErrorModeException(ex);
      return createDeclinedResponse(ex);
      
    } catch (Exception e) {
      handleErrorModeException(e);
      return createDeclinedResponse(e.getMessage());
    }
  }

  // 기존 시그니처도 유지하고 싶다면 오버로드
  public PaymentResponse authorize(PaymentRequest req) {
    return authorize(req, false);
  }

  /**
   * baseUrl과 endpoint를 안전하게 결합하여 완전한 URL 생성
   * 중복/누락 슬래시 방지
   */
  private String buildPaymentUrl(String endpoint) {
    String normalizedBase = baseUrl.endsWith("/")
        ? baseUrl.substring(0, baseUrl.length() - 1)
        : baseUrl;
    return normalizedBase + endpoint;
  }

  /**
   * REST 요청 스펙 빌더 생성
   * 공통 헤더 설정 및 forceFail 플래그 처리
   */
  private RestClient.RequestBodySpec buildRequestSpec(String url, boolean forceFail) {
    var spec = rest.post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .header(ACCEPT_HEADER, APPLICATION_JSON);
    
    if (forceFail) {
      spec.header(FORCE_PAYMENT_HEADER, FORCE_PAYMENT_VALUE);
    }
    
    return spec;
  }

  /**
   * DECLINED 상태의 PaymentResponse 생성
   */
  private PaymentResponse createDeclinedResponse(String reason) {
    String finalReason = (reason != null && !reason.isBlank())
        ? reason
        : "Unknown error";
    return new PaymentResponse(
        DECLINED_STATUS,
        null,
        finalReason,
        System.currentTimeMillis()
    );
  }

  /**
   * RestClientResponseException으로부터 DECLINED 응답 생성
   */
  private PaymentResponse createDeclinedResponse(RestClientResponseException ex) {
    String reason = ex.getResponseBodyAsString();
    if (reason == null || reason.isBlank()) {
      reason = ex.getStatusText();
    }
    return createDeclinedResponse(reason);
  }

  /**
   * errorMode가 활성화된 경우 예외를 전파
   * 비활성화된 경우 호출자가 DECLINED 응답을 처리
   */
  private void handleErrorModeException(Exception ex) {
    if (errorMode) {
      if (ex instanceof RuntimeException) {
        throw (RuntimeException) ex;
      }
      throw new RuntimeException("Payment processing failed", ex);
    }
  }
}
