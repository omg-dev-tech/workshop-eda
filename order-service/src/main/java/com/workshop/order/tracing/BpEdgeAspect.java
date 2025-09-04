package com.workshop.order.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BpEdgeAspect {
  private final Tracer tracer = GlobalOpenTelemetry.getTracer("bp-edge");
  private static final ThreadLocal<Span> LAST_SERVER = new ThreadLocal<>();

  @Around("@annotation(edge)")
  public Object aroundEdge(ProceedingJoinPoint pjp, BpEdge edge) throws Throwable {
    Span parent = LAST_SERVER.get();

    SpanBuilder clientBuilder = tracer.spanBuilder(edge.name() + " [CLIENT]")
        .setSpanKind(SpanKind.CLIENT);
    if (parent != null) clientBuilder.setParent(Context.current().with(parent));
    Span client = clientBuilder.startSpan();
    client.setAttribute("service", edge.from());
    client.setAttribute("bp.step.name", edge.name());
    client.setAttribute("bp.step.index", edge.index());

    try (Scope c = client.makeCurrent()) {
      Span server = tracer.spanBuilder(edge.name() + " [SERVER]")
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
