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
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

@Aspect
public class OTelServiceNodeAspect {

  /** --- 정적 브리지/지연 초기화 영역 --- */
  private static volatile VirtualOtelFactory vFactory;
  public static void setFactory(VirtualOtelFactory f) { vFactory = f; }

  private static volatile Tracer appTracer;                 // 현재 앱의 Tracer (Global)
  private static volatile TextMapPropagator propagator;

  private static Tracer getAppTracer() {
    Tracer t = appTracer;
    if (t == null) {
      synchronized (OTelServiceNodeAspect.class) {
        if (appTracer == null) {
          OpenTelemetry otel = GlobalOpenTelemetry.get();
          appTracer = otel.getTracer("svcnode-tracer-app");
          propagator = otel.getPropagators().getTextMapPropagator();
        }
        t = appTracer;
      }
    }
    return t;
  }

  private static TextMapPropagator getPropagator() {
    // getAppTracer() 호출 시 함께 초기화됨
    if (propagator == null) {
      getAppTracer();
    }
    return propagator;
  }

  /** 생성자 정의 X (기본 생성자 유지). Bean 등록 금지(CTW 단독). */

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

    final Tracer app = getAppTracer();
    final TextMapPropagator prop = getPropagator();

    Context parent = Context.current();

    // 1) 송신측 CLIENT/PRODUCER 스팬 (현재 앱의 service.name)
    SpanKind clientKind = (mode == ServiceNode.Mode.PRODUCER_CONSUMER)
        ? SpanKind.PRODUCER : SpanKind.CLIENT;

    var clientBuilder = app.spanBuilder(nodeName)
        .setSpanKind(clientKind)
        .setParent(parent)
        .setAttribute("rpc.system", "internal")
        .setAttribute("rpc.method", method)
        .setAttribute("peer.service", nodeName);     // 의존성 맵 연결 힌트

    if (mode == ServiceNode.Mode.PRODUCER_CONSUMER) {
      clientBuilder
        .setAttribute("messaging.system", "internal")
        .setAttribute("messaging.destination.name", nodeName)
        .setAttribute("messaging.operation", "publish");
    }

    Span client = clientBuilder.startSpan();

    Map<String,String> carrier = new HashMap<>();
    try (Scope ignored = client.makeCurrent()) {
      prop.inject(Context.current(), carrier, SETTER);
    } finally {
      client.end(); // 경계의 보낸쪽
    }

    // 2) 수신측 SERVER/CONSUMER 스팬 (nodeName을 service.name으로 가지는 Tracer)
    Tracer nodeTracer = null;
    VirtualOtelFactory factory = vFactory;
    if (factory != null) {
      nodeTracer = factory.tracer(nodeName);
    } else {
      // 브리지 미설정 시에도 죽지 않도록 fallback (동일 앱 Tracer 사용)
      nodeTracer = app;
    }

    SpanKind serverKind = (mode == ServiceNode.Mode.PRODUCER_CONSUMER)
        ? SpanKind.CONSUMER : SpanKind.SERVER;

    Context extracted = prop.extract(parent, carrier, GETTER);
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
