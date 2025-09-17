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

  private final VirtualOtelFactory vFactory;
  private final Tracer appTracer;                 // 현재 앱의 서비스 Tracer (Global)
  private final TextMapPropagator propagator;

  // Spring 주입용
  public OTelServiceNodeAspect(VirtualOtelFactory vFactory) {
    this.vFactory = vFactory;
    OpenTelemetry otel = io.opentelemetry.api.GlobalOpenTelemetry.get();
    this.appTracer = otel.getTracer("svcnode-tracer-app");
    this.propagator = otel.getPropagators().getTextMapPropagator();
  }

  // AspectJ 기본 생성자가 필요한 환경이라면, 정적 홀더/빈 조회 등으로 vFactory를 채워주세요.

  private static final TextMapSetter<Map<String, String>> SETTER =
      (carrier, key, value) -> carrier.put(key, value);
  private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
    @Override public Iterable<String> keys(Map<String, String> c) { return c.keySet(); }
    @Override public String get(Map<String, String> c, String k) { return c.get(k); }
  };

  @Around("@annotation(node)")
  public Object around(ProceedingJoinPoint pjp, ServiceNode node) throws Throwable {
    final String nodeName = node.value();           // 가상 서비스명 (예: "vs.taskB")
    final String method   = pjp.getSignature().getName();
    final ServiceNode.Mode mode = node.mode();

    Context parent = Context.current();

    // 1) 송신측 CLIENT/PRODUCER 스팬 (현재 앱의 service.name)
    SpanKind clientKind = (mode == ServiceNode.Mode.PRODUCER_CONSUMER)
        ? SpanKind.PRODUCER : SpanKind.CLIENT;

    var clientBuilder = appTracer.spanBuilder(nodeName)
        .setSpanKind(clientKind)
        .setParent(parent)
        .setAttribute("rpc.system", "internal")
        .setAttribute("rpc.method", method)
        .setAttribute("peer.service", nodeName);     // 의존성 맵 연결 힌트

    // 메시징 모델일 경우 표준 속성 추가
    if (mode == ServiceNode.Mode.PRODUCER_CONSUMER) {
      clientBuilder
        .setAttribute("messaging.system", "internal")
        .setAttribute("messaging.destination.name", nodeName)
        .setAttribute("messaging.operation", "publish");
    }

    Span client = clientBuilder.startSpan();

    Map<String,String> carrier = new HashMap<>();
    try (Scope ignored = client.makeCurrent()) {
      propagator.inject(Context.current(), carrier, SETTER);
    } finally {
      client.end(); // 경계의 보낸쪽
    }

    // 2) 수신측 SERVER/CONSUMER 스팬 (nodeName을 service.name으로 가지는 Tracer)
    Tracer nodeTracer = vFactory.tracer(nodeName);

    SpanKind serverKind = (mode == ServiceNode.Mode.PRODUCER_CONSUMER)
        ? SpanKind.CONSUMER : SpanKind.SERVER;

    Context extracted = propagator.extract(parent, carrier, GETTER);
    var serverBuilder = nodeTracer.spanBuilder(nodeName)
        .setSpanKind(serverKind)
        .setParent(extracted)
        .setAttribute("rpc.system", "internal")
        .setAttribute("rpc.method", method);

    if (mode == ServiceNode.Mode.PRODUCER_CONSUMER) {
      serverBuilder
        .setAttribute("messaging.system", "internal")
        .setAttribute("messaging.destination.name", nodeName)
        .setAttribute("messaging.operation", "process");
    }

    Span server = serverBuilder.startSpan();

    try (Scope ignored = server.makeCurrent()) {
      return pjp.proceed();
    } catch (Throwable t) {
      server.recordException(t);
      server.setStatus(StatusCode.ERROR, t.toString());
      throw t;
    } finally {
      server.end();
    }
  }
}
