package com.workshop.order.tracing;

import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class KafkaHeaderUtils {
  private KafkaHeaderUtils() {}

  /** org.apache.kafka.common.header.Headers → Map (키는 원본 + 소문자 중복 저장) */
  public static Map<String, String> toMap(Headers headers) {
    Map<String, String> map = new HashMap<>();
    if (headers == null) return map;
    headers.forEach(h -> {
      if (h == null || h.key() == null) return;
      String k = h.key();
      String v = h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8);
      map.put(k, v);
      map.put(k.toLowerCase(), v);
    });
    return map;
  }

  /** Map용 OTel Getter (원키 → 소문자 키 둘 다 시도) */
  public static final TextMapGetter<Map<String,String>> MAP_GETTER = new TextMapGetter<>() {
    @Override public Iterable<String> keys(Map<String, String> carrier) { return carrier.keySet(); }
    @Override public String get(Map<String, String> carrier, String key) {
      if (carrier == null) return null;
      String v = carrier.get(key);
      return v != null ? v : carrier.get(key.toLowerCase());
    }
  };

  /** Instana 헤더를 OTel hex 규격으로 맞춰보기 (best-effort) */
  public static String normalizeTo32Hex(String raw) {
    if (raw == null) return null;
    String s = raw.trim().replaceAll("[^0-9a-fA-F]", "");
    if (s.length() == 32) return s.toLowerCase();
    if (s.length() < 32) return String.format("%032x", new java.math.BigInteger(s, 16));
    return s.substring(s.length() - 32).toLowerCase();
  }
  public static String normalizeTo16Hex(String raw) {
    if (raw == null) return null;
    String s = raw.trim().replaceAll("[^0-9a-fA-F]", "");
    if (s.length() == 16) return s.toLowerCase();
    if (s.length() < 16) return String.format("%016x", new java.math.BigInteger(s, 16));
    return s.substring(s.length() - 16).toLowerCase();
  }
}
