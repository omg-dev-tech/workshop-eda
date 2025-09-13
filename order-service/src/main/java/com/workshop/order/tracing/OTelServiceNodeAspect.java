package com.workshop.order.tracing;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

@Aspect
public class OTelServiceNodeAspect {

  private final Tracer tracer;
  private final TextMapPropagator propagator;

  // ★ AspectJ가 호출할 기본 생성자
  public OTelServiceNodeAspect() {
    OpenTelemetry otel = io.opentelemetry.api.GlobalOpenTelemetry.get(); // 전역에서 가져오기
    this.tracer = otel.getTracer("svcnode-tracer");
    this.propagator = otel.getPropagators().getTextMapPropagator();
  }

  private static final TextMapSetter<Map<String, String>> SETTER =
      (carrier, key, value) -> carrier.put(key, value);
  private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
    @Override public Iterable<String> keys(Map<String, String> c) { return c.keySet(); }
    @Override public String get(Map<String, String> c, String k) { return c.get(k); }
  };

  @Around("@annotation(node)")
  public Object around(ProceedingJoinPoint pjp, ServiceNode node) throws Throwable {
    final String nodeName = node.value();          // 예: "taskB"
    final String method   = pjp.getSignature().getName();

    Context parent = Context.current();

    // 1) CLIENT 스팬: "이전 단계가 다음 노드로 호출했다"를 모델링
    Span client = tracer.spanBuilder(nodeName)
        .setSpanKind(SpanKind.CLIENT)
        .setParent(parent)
        .setAttribute("rpc.system", "internal")
        .setAttribute("rpc.method", method)
        // Instana 서비스 맵핑용: 원격 서비스 이름
        .setAttribute("peer.service", nodeName)
        .startSpan();

    Map<String,String> carrier = new HashMap<>();
    try (Scope ignored = client.makeCurrent()) {
      // 컨텍스트를 carrier에 주입
      propagator.inject(Context.current(), carrier, SETTER);
    } finally {
      client.end(); // CLIENT는 즉시 종료(경계의 '보내는 쪽')
    }

    // 2) SERVER 스팬: "해당 노드가 수신/시작점"으로 보이게
    Context extracted = propagator.extract(parent, carrier, GETTER);
    Span server = tracer.spanBuilder(nodeName)
        .setSpanKind(SpanKind.SERVER)
        .setParent(extracted)
        .setAttribute("rpc.system", "internal")
        .setAttribute("rpc.method", method)
        // Instana per-call 서비스명(수신측 서비스 이름)
        .setAttribute("service", nodeName)
        .startSpan();

    try (Scope ignored = server.makeCurrent()) {
      Object ret = pjp.proceed();          // ★ 실제 메소드 실행
      return ret;
    } catch (Throwable t) {
      server.recordException(t);
      server.setStatus(StatusCode.ERROR, t.toString());
      throw t;
    } finally {
      server.end();
    }
  }
}
