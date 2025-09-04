package com.workshop.order.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;

import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import io.opentelemetry.api.trace.*;

@Aspect
public class BpEdgeAspect {
  // lazy 초기화용
  private volatile Tracer _tracer;

  private Tracer tracer() {
    Tracer t = _tracer;
    if (t == null) {
      synchronized (this) {
        if (_tracer == null) {
          _tracer = GlobalOpenTelemetry.get().getTracer("bp-edge");
        }
        t = _tracer;
      }
    }
    return t;
  }
  private static final ThreadLocal<Span> LAST_SERVER = new ThreadLocal<>();

  @Around("@annotation(edge)")
  public Object aroundEdge(ProceedingJoinPoint pjp, BpEdge edge) throws Throwable {
    Span parent = LAST_SERVER.get();

    SpanBuilder clientBuilder = tracer().spanBuilder(edge.name() + " [CLIENT]")
        .setSpanKind(SpanKind.CLIENT);
    if (parent != null) clientBuilder.setParent(Context.current().with(parent));
    Span client = clientBuilder.startSpan();
    client.setAttribute("service", edge.from());
    client.setAttribute("bp.step.name", edge.name());
    client.setAttribute("bp.step.index", edge.index());

    try (Scope c = client.makeCurrent()) {
      Span server = tracer().spanBuilder(edge.name() + " [SERVER]")
          .setSpanKind(SpanKind.SERVER)
          .startSpan();
      server.setAttribute("service", edge.to());
      server.setAttribute("bp.step.name", edge.name());
      server.setAttribute("bp.step.index", edge.index());

      LAST_SERVER.set(server);
      try (Scope s = server.makeCurrent()) {
        return pjp.proceed();
      } catch (Throwable t) {
        server.recordException(t);
        server.setStatus(StatusCode.ERROR);
        throw t;
      } finally {
        server.end();
      }
    } finally {
      client.end();
    }
  }
}
