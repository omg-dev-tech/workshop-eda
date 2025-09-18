package com.workshop.order.tracing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

// KafkaConfig.java
@Configuration
public class KafkaConfig {

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory) {

    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory);

    // 1) 레코드 인터셉터: 리스너 호출 "직전"에 헤더 읽기
    factory.setRecordInterceptor((record, consumer) -> {
      var headers = record.headers();

      String xInstanaT = getString(headers, "X_INSTANA_T");
      String xInstanaS = getString(headers, "X_INSTANA_S");
      String xInstanaLS = getString(headers, "X_INSTANA_L_S");
      String traceparent = getString(headers, "traceparent");
      String tracestate  = getString(headers, "tracestate");

      // 로그로 남기기 (원하면 MDC에 넣어 전체 로그에 태깅)
      if (xInstanaT != null || traceparent != null) {
        org.slf4j.MDC.put("traceId.instana", xInstanaT);
        org.slf4j.MDC.put("traceparent", traceparent);
      }
      org.slf4j.LoggerFactory.getLogger("KafkaTrace")
          .info("[KafkaTrace] topic={} partition={} offset={} X_INSTANA_T={} X_INSTANA_S={} X_INSTANA_L_S={} traceparent={} tracestate={}",
                record.topic(), record.partition(), record.offset(),
                nvl(xInstanaT), nvl(xInstanaS), nvl(xInstanaLS), nvl(traceparent), nvl(tracestate));

      return record; // null 반환 시 리스너 호출 안 함
    });

    return factory;
  }

  private static String getString(org.apache.kafka.common.header.Headers headers, String key) {
    var h = headers.lastHeader(key);
    return (h == null) ? null : new String(h.value(), java.nio.charset.StandardCharsets.UTF_8);
  }

  private static String nvl(String s) { return (s == null ? "-" : s); }
}
