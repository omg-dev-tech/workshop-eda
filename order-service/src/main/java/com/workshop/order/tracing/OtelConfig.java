package com.workshop.order.tracing;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.*;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

@Configuration
public class OtelConfig {
    @Bean
    public OpenTelemetrySdk openTelemetry() {
        var exporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(System.getenv().getOrDefault(
                "OTEL_EXPORTER_OTLP_ENDPOINT",
                "http://instana-agent.instana-agent:4317"))
            .build();

        var provider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
            .setResource(Resource.getDefault().merge(Resource.create(
                Attributes.of(SERVICE_NAME, "order-process"))))
            .build();

        // ✅ 글로벌은 여기서 "단 한 번" 등록
        return OpenTelemetrySdk.builder()
            .setTracerProvider(provider)
            .buildAndRegisterGlobal();
    }

    // ✅ Aspect에 주입할 Tracer 빈
    @Bean
    public Tracer bpEdgeTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("bp-edge");
    }
}
