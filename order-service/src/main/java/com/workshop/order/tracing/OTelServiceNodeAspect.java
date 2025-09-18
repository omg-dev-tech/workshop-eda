package com.workshop.order.tracing;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

@Slf4j
@Aspect
public class OTelServiceNodeAspect {

  private static volatile VirtualOtelFactory vFactory;
  public static void setFactory(VirtualOtelFactory f) { vFactory = f; }

  private static volatile Tracer appTracer;

  private static Tracer app() {
    if (appTracer == null) {
      synchronized (OTelServiceNodeAspect.class) {
        if (appTracer == null) {
          appTracer = GlobalOpenTelemetry.get().getTracer("svcnode-tracer-app");
        }
      }
    }
    return appTracer;
  }

  @Around("execution(@com.workshop.order.tracing.ServiceNode * *(..)) && @annotation(node)")
  public Object around(ProceedingJoinPoint pjp, ServiceNode node) throws Throwable {
    final String nodeName = node.value();
    final String method   = pjp.getSignature().getName();
    final var mode        = node.mode();

    // ★ 현재 체인의 부모(없으면 Context.current())
    Context parent = ServiceChainContext.getOrCurrent();
    Span parentSpan = Span.fromContext(parent);

    // 1) CLIENT (앱 Tracer) — 부모는 직전 SERVER
    SpanKind clientKind = (mode == ServiceNode.Mode.PRODUCER_CONSUMER)
        ? SpanKind.PRODUCER : SpanKind.CLIENT;

    Span client = app().spanBuilder("call " + nodeName)
        .setSpanKind(clientKind)
        .setParent(parent)
        .setAttribute("rpc.system", "internal")
        .setAttribute("rpc.service", nodeName)
        .setAttribute("rpc.method", method)
        .setAttribute("peer.service", nodeName)
        .startSpan();

    log.info("[CLIENT] {} traceId={} spanId={} parentSpanId={}",
        nodeName,
        client.getSpanContext().getTraceId(),
        client.getSpanContext().getSpanId(),
        parentSpan.getSpanContext().isValid()
            ? parentSpan.getSpanContext().getSpanId()
            : "0000000000000000"
    );

    // 2) SERVER (가상 서비스 Tracer) — 부모는 "지금(=CLIENT가 current인) 컨텍스트"
    Tracer nodeTracer = (vFactory != null) ? vFactory.tracer(nodeName) : app();
    SpanKind serverKind = (mode == ServiceNode.Mode.PRODUCER_CONSUMER)
        ? SpanKind.CONSUMER : SpanKind.SERVER;

    Span server;
    Context serverCtx;
    try (Scope cScope = client.makeCurrent()) {
      server = nodeTracer.spanBuilder(nodeName)
          .setSpanKind(serverKind)
          .setParent(Context.current())   // ← CLIENT 아래에 SERVER를 직접 연결
          .setAttribute("rpc.system", "internal")
          .setAttribute("rpc.service", nodeName)
          .setAttribute("rpc.method", method)
          .startSpan();

      log.info("[SERVER] {} traceId={} spanId={} parentSpanId={}",
          nodeName,
          server.getSpanContext().getTraceId(),
          server.getSpanContext().getSpanId(),
          client.getSpanContext().getSpanId()
      );

      try (Scope sScope = server.makeCurrent()) {
        // ★ 다음 hop은 이 SERVER 밑으로 붙는다
        serverCtx = Context.current();
        ServiceChainContext.set(serverCtx);

        return pjp.proceed();
      } finally {
        server.end();
      }
    } finally {
      client.end();
      // SERVER가 끝났으니, 체인 부모는 더 이상 유효 컨텍스트가 아님.
      // 다음 hop에서 다시 set 할 것이므로 깔끔히 지워둠 (부모 잔류 방지)
      // ServiceChainContext.clear();
    }
  }
}
