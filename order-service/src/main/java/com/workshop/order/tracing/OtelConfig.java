package com.workshop.order.tracing;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import io.opentelemetry.api.common.Attributes;
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
            // Instana 에이전트 OTLP gRPC (클러스터라면 Service DNS 사용)
            .setEndpoint(System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT",
                        "http://instana-agent.instana-agent:4317"))
            .build();

        var provider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
            .setResource(Resource.getDefault().merge(Resource.create(
                Attributes.of(SERVICE_NAME, "order-process"))))
            .build();

        return OpenTelemetrySdk.builder().setTracerProvider(provider).buildAndRegisterGlobal();
    }
}
