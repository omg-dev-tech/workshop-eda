package com.workshop.order.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class PaymentClient {

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
    // ✅ 절대 URL을 안전하게 구성 (중복/누락 슬래시 방지)
    String url = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl)
               + "/payments/authorize";
    try {
      var spec = rest.post()
          .uri(url);

      var requestSpec = spec
          .contentType(MediaType.APPLICATION_JSON)
          .header("Accept", "application/json");

      if (forceFail) {
        // 서버가 인식하는 강제 실패 트리거(둘 중 하나면 됨)
        requestSpec.header("X-Force-Payment", "fail");
        // 또는 쿼리파라미터로 강제 실패를 쓰고 싶다면:
        // .uri(baseUrl + "/payments/authorize?force=fail")
      }

      // 여기서 4xx/5xx 발생 시 기본적으로 RestClientResponseException 발생
      return requestSpec
          .body(req)
          .retrieve()
          // onStatus로 401을 명시적 예외화(선택적이지만 가독성↑)
          .onStatus(HttpStatus.UNAUTHORIZED::equals, (r, res) -> {
            // 에러 모드면 예외 던져 Instana Error Call로
            if (errorMode) {
              throw new RestClientResponseException(
                  "Payment unauthorized",
                  res.getStatusCode().value(),
                  "",
                  res.getHeaders(),
                  res.getBody() != null ? res.getBody().readAllBytes() : null,
                  null
              );
            }
          })
          .body(PaymentResponse.class);

    } catch (RestClientResponseException ex) {
      // 에러 모드면 그대로 전파 → Instana에서 Error Call로 집계
      if (errorMode) throw ex;

      // 비즈니스 모드: 401 등을 DECLINED로 변환
      String reason = ex.getResponseBodyAsString();
      if (reason == null || reason.isBlank()) {
        reason = ex.getStatusText();
      }
      return new PaymentResponse(
          "DECLINED",
          null,
          reason,
          System.currentTimeMillis()
      );
    } catch (Exception e) {
      // 네트워크 등 기타 예외도 에러 모드면 전파
      if (errorMode) throw e;
      return new PaymentResponse("DECLINED", null, e.getMessage(), System.currentTimeMillis());
    }
  }

  // 기존 시그니처도 유지하고 싶다면 오버로드
  public PaymentResponse authorize(PaymentRequest req) {
    return authorize(req, false);
  }
}
