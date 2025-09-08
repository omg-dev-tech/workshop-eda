package com.workshop.order.tracing;

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

@Aspect
public class ServiceNodeAspect {

  // ★ 무인자 생성자 필수 (AspectJ가 aspectOf()에서 사용)
  public ServiceNodeAspect() {}

  private static final String VSSC_KEY = "vsvc";

  // lazy factory (환경변수에서 엔드포인트 읽음)
  private volatile VirtualOtelFactory _factory;
  private VirtualOtelFactory factory() {
    var f = _factory;
    if (f == null) {
      synchronized (this) {
        if (_factory == null) {
          String ep = System.getenv().getOrDefault(
              "OTEL_EXPORTER_OTLP_ENDPOINT", "http://instana-agent.instana-agent:4317");
          _factory = new VirtualOtelFactory(ep);
        }
        f = _factory;
      }
    }
    return f;
  }

  @Around("@annotation(node)")
  public Object around(ProceedingJoinPoint pjp, ServiceNode node) throws Throwable {
    // from 결정: baggage.vsvc 있으면 사용, 없으면 기본값
    var baggage = Baggage.fromContext(Context.current());
    String from = Optional.ofNullable(baggage.getEntryValue(VSSC_KEY)).orElse("vs.entry");
    String to   = node.value();

    // FROM(Exit) 스팬
    var fromKind = node.mode()==ServiceNode.Mode.PRODUCER_CONSUMER ? SpanKind.PRODUCER : SpanKind.CLIENT;
    var fromSpan = factory().tracer(from).spanBuilder(node.value()+" [FROM]").setSpanKind(fromKind).startSpan();
    try (var f = fromSpan.makeCurrent()) {

      // TO(Entry) 스팬
      var toKind = node.mode()==ServiceNode.Mode.PRODUCER_CONSUMER ? SpanKind.CONSUMER : SpanKind.SERVER;
      var toSpan = factory().tracer(to).spanBuilder(node.value()+" [TO]").setSpanKind(toKind).startSpan();

      // 이후 단계의 from을 'to'로 넘겨주기 (선택)
      var newBag = Baggage.current().toBuilder().put(VSSC_KEY, to).build();

      try (var s = toSpan.makeCurrent(); var b = newBag.makeCurrent()) {
        return pjp.proceed();
      } catch (Throwable t) {
        toSpan.recordException(t);
        toSpan.setStatus(StatusCode.ERROR);
        throw t;
      } finally {
        toSpan.end();
      }
    } finally {
      fromSpan.end();
    }
  }
}
