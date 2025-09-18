package com.workshop.order.tracing;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.messaging.Message;
import org.springframework.kafka.support.KafkaHeaders;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;

import java.util.Map;

@Slf4j
@Aspect
public class ServiceChainRootAspect {

  /** 경계 어드바이스 재진입 가드 (동일 스레드에서 중복 적용 방지) */
  private static final ThreadLocal<Boolean> ENTERED = ThreadLocal.withInitial(() -> false);

  /** 모든 @KafkaListener 메서드 실행 시점에 적용 (비즈니스 코드 수정 불필요) */
  @Around("execution(@org.springframework.kafka.annotation.KafkaListener * *(..))")
  public Object kafkaRoot(ProceedingJoinPoint pjp) throws Throwable {
    if (ENTERED.get()) return pjp.proceed();
    ENTERED.set(true);
    try {
      // 새 요청: 체인 초기화
      ServiceChainContext.clear();

      // 1) 리스너 인수들에서 Kafka Headers를 찾아낸다
      Headers headers = extractKafkaHeaders(pjp.getArgs());

      if (headers != null) {
        Map<String,String> map = KafkaHeaderUtils.toMap(headers);

        // 2) 표준 propagator로 먼저 extract (traceparent/b3 등)
        var propagator = GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator();
        Context extracted = propagator.extract(Context.current(), map, KafkaHeaderUtils.MAP_GETTER);
        Span extractedSpan = Span.fromContext(extracted);

        if (extractedSpan.getSpanContext().isValid()) {
          ServiceChainContext.set(extracted);
          log.debug("[KafkaRoot] extracted via propagator: traceId={} spanId={}",
              extractedSpan.getSpanContext().getTraceId(),
              extractedSpan.getSpanContext().getSpanId());
        } else {
          // 3) fallback: Instana 헤더(x-instana-t/x-instana-s)로 SpanContext 수동 구성 시도
          String t = firstNonNull(map.get("X-Instana-T"), map.get("x-instana-t"));
          String s = firstNonNull(map.get("X-Instana-S"), map.get("x-instana-s"));
          if (t != null && s != null) {
            try {
              String traceId = KafkaHeaderUtils.normalizeTo32Hex(t);
              String spanId  = KafkaHeaderUtils.normalizeTo16Hex(s);
              if (traceId != null && spanId != null) {
                SpanContext sc = SpanContext.createFromRemoteParent(
                    traceId, spanId, TraceFlags.getDefault(), TraceState.getDefault());
                Context ctx = Context.root().with(Span.wrap(sc));
                ServiceChainContext.set(ctx);
                log.debug("[KafkaRoot] constructed from Instana headers: traceId={} spanId={}", traceId, spanId);
              } else {
                log.debug("[KafkaRoot] Instana headers present but not hex; skip");
              }
            } catch (Throwable e) {
              log.warn("[KafkaRoot] failed to build SpanContext from Instana headers", e);
            }
          } else {
            log.debug("[KafkaRoot] no trace headers found; chain starts empty");
          }
        }
      } else {
        log.debug("[KafkaRoot] no Headers argument found; chain starts empty");
      }

      // 4) 비즈니스 수행 (OTelServiceNodeAspect가 ServiceChainContext를 부모로 사용)
      return pjp.proceed();

    } finally {
      // 요청 종료: 체인 정리
      ServiceChainContext.clear();
      ENTERED.set(false);
    }
  }

  // ───────── helpers ─────────

  private static Headers extractKafkaHeaders(Object[] args) {
    if (args == null) return null;
    for (Object a : args) {
      if (a == null) continue;

      // (1) ConsumerRecord<?,?>
      if (a instanceof ConsumerRecord<?,?> cr) {
        return cr.headers();
      }

      // (2) Spring Message<?>: native headers에 Kafka Headers가 있음
      if (a instanceof Message<?> msg) {
        Object nativeHeaders = msg.getHeaders().get(KafkaHeaders.NATIVE_HEADERS);
        if (nativeHeaders instanceof Headers h) return h;
      }

      // (3) 인자 자체가 Headers
      if (a instanceof Headers h) {
        return h;
      }

      // (4) Spring Messaging header map (Map<String, Object>)에 들어있는 경우
      if (a instanceof Map<?,?> m) {
        Object nh = m.get(KafkaHeaders.NATIVE_HEADERS);
        if (nh instanceof Headers h) return h;
      }
    }
    return null;
  }

  private static String firstNonNull(String a, String b) {
    return (a != null) ? a : b;
  }
}
