package com.workshop.order.tracing;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.lang.Nullable;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class KafkaTracePropExtractInterceptor<K,V> implements RecordInterceptor<K,V> {

  // ---- TextMapGetter: Headers 전체 key 목록 만들기 (iterator 사용)
  private static final TextMapGetter<Headers> GETTER = new TextMapGetter<>() {
    @Override public Iterable<String> keys(Headers carrier) {
      if (carrier == null) return List.of();
      List<String> keys = new ArrayList<>();
      for (Header h : carrier) {            // ← iterable 지원
        keys.add(h.key());
      }
      return keys;
    }
    @Override public String get(Headers carrier, String key) {
      if (carrier == null) return null;
      Header h = carrier.lastHeader(key);
      return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
  };

  // ---- Spring Kafka 3.x 필수 구현: intercept(record, consumer)
  @Override
  public ConsumerRecord<K,V> intercept(ConsumerRecord<K,V> record, Consumer<K,V> consumer) {
    process(record.headers());
    return record;
  }

  // ---- (호환용) 구버전 시그니처도 구현해두면 좋음
  public ConsumerRecord<K,V> intercept(ConsumerRecord<K,V> record) {
    process(record.headers());
    return record;
  }

  // ---- Spring Kafka 3.x 필수 구현: afterRecord(record, consumer)
  @Override
  public void afterRecord(ConsumerRecord<K,V> record, @Nullable Consumer<K,V> consumer) {
    ServiceChainContext.clear();
  }

  // ---- (호환용) 구버전 afterRecord
  public void afterRecord(ConsumerRecord<K,V> record, @Nullable Exception ex) {
    ServiceChainContext.clear();
  }

  // ===== 내부 로직 =====
  private void process(Headers headers) {
    String t  = get(headers, "X_INSTANA_T");      // 32-hex traceId
    String s  = get(headers, "X_INSTANA_S");      // 16-hex parent spanId
    String ls = get(headers, "X_INSTANA_L_S");    // "1" → sampled

    // Instana 헤더 → W3C traceparent 문자열
    String traceparent = toTraceparent(t, s, "1".equals(ls));
    if (traceparent != null) {
      // carrier에 traceparent를 올려두고 extractor로 Context 생성
      headers.remove("traceparent");
      headers.add("traceparent", traceparent.getBytes(StandardCharsets.UTF_8));

      Context parentCtx = W3CTraceContextPropagator.getInstance()
                          .extract(Context.current(), headers, GETTER);

      // ★ 기존 로직 유지: Aspect가 ServiceChainContext.getOrCurrent()를 부모로 씀
      ServiceChainContext.set(parentCtx);
    } else {
      ServiceChainContext.clear();
    }
  }

  private static String get(Headers headers, String key) {
    Header h = headers.lastHeader(key);
    return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
  }

  // Instana X_INSTANA_* → W3C traceparent("00-{traceId}-{spanId}-{flags}")
  private static String toTraceparent(String traceId, String parentSpanId, boolean sampled) {
    if (!isHex(traceId, 32) || !isHex(parentSpanId, 16) ||
        isAllZero(traceId) || isAllZero(parentSpanId)) {
      return null;
    }
    String flags = sampled ? "01" : "00";
    return "00-" + traceId.toLowerCase() + "-" + parentSpanId.toLowerCase() + "-" + flags;
  }

  private static boolean isHex(String v, int len) {
    if (v == null || v.length() != len) return false;
    for (int i = 0; i < len; i++) {
      char c = v.charAt(i);
      if (!((c>='0'&&c<='9')||(c>='a'&&c<='f')||(c>='A'&&c<='F'))) return false;
    }
    return true;
  }

  private static boolean isAllZero(String v) {
    for (int i = 0; i < v.length(); i++) if (v.charAt(i) != '0') return false;
    return true;
  }
}
