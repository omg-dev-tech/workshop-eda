package com.workshop.order.tracing;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import io.opentelemetry.api.GlobalOpenTelemetry;
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

  // ── 정적 브리지 ─────────────────────────────────────────────
  private static volatile VirtualOtelFactory vFactory;
  public static void setFactory(VirtualOtelFactory f) { vFactory = f; }

  private static volatile Tracer appTracer;
  private static volatile TextMapPropagator propagator;

  private static Tracer app() {
    if (appTracer == null) {
      synchronized (OTelServiceNodeAspect.class) {
        if (appTracer == null) {
          var otel = GlobalOpenTelemetry.get();
          appTracer = otel.getTracer("svcnode-tracer-app");
          propagator = otel.getPropagators().getTextMapPropagator();
        }
      }
    }
    return appTracer;
  }
  private static TextMapPropagator prop() {
    if (propagator == null) app();
    return propagator;
  }
  // 생성자 정의 금지(기본 생성자 유지). Bean으로 등록하지 않음(CTW 전제).

  private static final TextMapSetter<Map<String,String>> SETTER =
      (carrier, k, v) -> carrier.put(k, v);
  private static final TextMapGetter<Map<String,String>> GETTER = new TextMapGetter<>() {
    @Override public Iterable<String> keys(Map<String,String> c) { return c.keySet(); }
    @Override public String get(Map<String,String> c, String k) { return c.get(k); }
  };

  @Around("@annotation(node)")
  public Object around(ProceedingJoinPoint pjp, ServiceNode node) throws Throwable {
    final String nodeName = node.value(); // vs.taskA / vs.taskB / vs.taskC
    final String method   = pjp.getSignature().getName();
    final var mode        = node.mode();
    final Context parent  = Context.current();

    // 1) CLIENT/PRODUCER: 앱 Tracer (현재 앱의 service.name로 귀속)
    final SpanKind clientKind = (mode == ServiceNode.Mode.PRODUCER_CONSUMER)
        ? SpanKind.PRODUCER : SpanKind.CLIENT;

    var clientBuilder = app().spanBuilder("call " + nodeName)
        .setSpanKind(clientKind)
        .setParent(parent)
        // 표준 힌트 (Instana 엣지 계산 보조)
        .setAttribute("rpc.system",  "internal")
        .setAttribute("rpc.service", nodeName)   // 원격 서비스 식별
        .setAttribute("rpc.method",  method)
        .setAttribute("server.address", nodeName)
        .setAttribute("peer.service", nodeName);

    if (mode == ServiceNode.Mode.PRODUCER_CONSUMER) {
      clientBuilder
          .setAttribute("messaging.system", "internal")
          .setAttribute("messaging.destination.name", nodeName)
          .setAttribute("messaging.operation", "publish");
    }

    Span client = clientBuilder.startSpan();

    Map<String,String> carrier = new HashMap<>();
    try (Scope ignored = client.makeCurrent()) {
      prop().inject(Context.current(), carrier, SETTER);
    } finally {
      client.end();
    }

    // 2) SERVER/CONSUMER: 가상 서비스 Tracer (service.name = nodeName)
    final SpanKind serverKind = (mode == ServiceNode.Mode.PRODUCER_CONSUMER)
        ? SpanKind.CONSUMER : SpanKind.SERVER;

    var extracted  = prop().extract(parent, carrier, GETTER);
    var nodeTracer = (vFactory != null) ? vFactory.tracer(nodeName) : app(); // 안전 Fallback

    var serverBuilder = nodeTracer.spanBuilder(nodeName)
        .setSpanKind(serverKind)
        .setParent(extracted)
        .setAttribute("rpc.system",  "internal")
        .setAttribute("rpc.service", nodeName)
        .setAttribute("rpc.method",  method);

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
