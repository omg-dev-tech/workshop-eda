package com.workshop.order.tracing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.Span.Type;
import com.instana.sdk.support.SpanSupport;

@Aspect
public class ServiceNodeAspect {

  /** Throwable 을 던질 수 있는 함수형 인터페이스 */
  @FunctionalInterface
  interface SpanBody {
    Object call() throws Throwable;
  }

  // 방법 1) @annotation 바인딩 + execution 융합 (가장 안전)
  @Around("execution(@com.workshop.order.tracing.ServiceNode * *(..)) && @annotation(node)")
  public Object aroundServiceNode(ProceedingJoinPoint pjp, ServiceNode node) throws Throwable {
    final String name = node.value();
    // proceed() 는 Throwable 을 던지므로 커스텀 인터페이스로 전달
    return exitThenEntry(name, node.mode(), () -> pjp.proceed());
  }

  /** EXIT → ENTRY 를 같은 이름으로 연달아 생성 */
  private Object exitThenEntry(String name, ServiceNode.Mode mode, SpanBody body) throws Throwable {
    return exitEnvelope(name, mode, () -> entryEnvelope(name, mode, body));
  }

  /** EXIT 스팬 래퍼: throws Throwable 로 맞춤 */
  @Span(type = Type.EXIT, value = "sdk.exit")
  private Object exitEnvelope(String name, ServiceNode.Mode mode, SpanBody next) throws Throwable {
    // 동일 네이밍
    SpanSupport.annotate(Type.EXIT, "sdk.exit", "service",   name);
    SpanSupport.annotate(Type.EXIT, "sdk.exit", "endpoint",  name);
    SpanSupport.annotate(Type.EXIT, "sdk.exit", "call.name", name);
    SpanSupport.annotate(Type.EXIT, "sdk.exit", "span.kind",
        mode == ServiceNode.Mode.PRODUCER_CONSUMER ? "producer" : "client");

    // 원격 상관관계 (EXIT → ENTRY)
    SpanSupport.inheritNext(
        SpanSupport.currentTraceId(Type.EXIT),
        SpanSupport.currentSpanId(Type.EXIT)
    );

    try {
      return next.call();
    } catch (Throwable t) {
      SpanSupport.annotate(Type.EXIT, "sdk.exit", "error", "true");
      if (t.getMessage() != null) {
        SpanSupport.annotate(Type.EXIT, "sdk.exit", "message", t.getMessage());
      }
      throw t;
    }
  }

  /** ENTRY 스팬 래퍼: throws Throwable 로 맞춤 */
  @Span(type = Type.ENTRY, value = "sdk.entry")
  private Object entryEnvelope(String name, ServiceNode.Mode mode, SpanBody body) throws Throwable {
    // 동일 네이밍
    SpanSupport.annotate(Type.ENTRY, "sdk.entry", "service",   name);
    SpanSupport.annotate(Type.ENTRY, "sdk.entry", "endpoint",  name);
    SpanSupport.annotate(Type.ENTRY, "sdk.entry", "call.name", name);
    SpanSupport.annotate(Type.ENTRY, "sdk.entry", "span.kind",
        mode == ServiceNode.Mode.PRODUCER_CONSUMER ? "consumer" : "server");

    try {
      return body.call();
    } catch (Throwable t) {
      SpanSupport.annotate(Type.ENTRY, "sdk.entry", "error", "true");
      if (t.getMessage() != null) {
        SpanSupport.annotate(Type.ENTRY, "sdk.entry", "message", t.getMessage());
      }
      throw t;
    }
  }
}
