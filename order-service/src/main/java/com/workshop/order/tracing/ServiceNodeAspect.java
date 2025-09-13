package com.workshop.order.tracing;

import java.util.concurrent.Callable;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.Span.Type;
import com.instana.sdk.support.SpanSupport;

@Aspect
public class ServiceNodeAspect {

    private static final String EXIT_SPAN = "svcnode-exit";
    private static final String ENTRY_SPAN = "svcnode-entry";

    @Around("@annotation(node)")
    public Object aroundServiceNode(ProceedingJoinPoint pjp, ServiceNode node) throws Throwable {
        final String name = node.value();
        return exitThenEntry(name, () -> {
            try {
                return pjp.proceed();
            } catch (Throwable t) {
                // ENTRY 스팬을 오류로 표시
                SpanSupport.annotate(Span.Type.ENTRY, ENTRY_SPAN, "tags.error", "true");
                // (선택) 에러 메시지/코드 등 추가 태깅
                SpanSupport.annotate(Span.Type.ENTRY, ENTRY_SPAN, "tags.error.message", t.toString());
                throw new Exception(t);
            }
        });
    }

    /** EXIT 직후 ENTRY를 여는 래퍼 */
    private Object exitThenEntry(String serviceName, Callable<?> body) throws Exception {
        return openExit(serviceName, () -> openEntry(serviceName, body));
    }

    /** EXIT 스팬 시작: 이전 단계에서 '이 서비스로 나간 호출'로 보이게 함 */
    @Span(type = Span.Type.EXIT, value = EXIT_SPAN)
    private Object openExit(String serviceName, Callable<?> next) throws Exception {
        // UI 표시용 이름 오버라이드: 서비스/콜명을 현재 노드명으로 지정
        SpanSupport.annotate(Span.Type.EXIT, EXIT_SPAN, "service", serviceName);
        SpanSupport.annotate(Span.Type.EXIT, EXIT_SPAN, "call.name", serviceName);
        // (선택) HTTP/RPC로 변환하고 싶다면 semantic tag를 추가
        // SpanSupport.annotate(Span.Type.EXIT, EXIT_SPAN, "tags.rpc.method",
        // "INTERNAL");

        return next.call();
    }

    /** ENTRY 스팬 시작: 이 메소드 자체를 '수신'으로 보이게 함 */
    @Span(type = Span.Type.ENTRY, value = ENTRY_SPAN)
    private Object openEntry(String serviceName, Callable<?> body) throws Exception {
        // UI 표시용: 서비스/엔드포인트명을 현재 노드명으로 지정
        SpanSupport.annotate(Span.Type.ENTRY, ENTRY_SPAN, "service", serviceName);
        SpanSupport.annotate(Span.Type.ENTRY, ENTRY_SPAN, "endpoint", serviceName);
        SpanSupport.annotate(Span.Type.ENTRY, ENTRY_SPAN, "call.name", serviceName);
        return body.call();
    }
}
