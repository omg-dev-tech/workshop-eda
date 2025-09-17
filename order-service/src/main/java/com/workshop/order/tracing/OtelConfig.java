package com.workshop.order.tracing;

import org.springframework.context.annotation.Configuration;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@Configuration
public class OtelConfig {

  @Bean
  public VirtualOtelFactory virtualOtelFactory(
      @Value("${OTEL_EXPORTER_OTLP_ENDPOINT}") String endpoint,
      @Value("${INSTANA_API_KEY:}") Optional<String> apiKey
  ) {
    var builder = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint);
    apiKey.filter(k -> !k.isBlank())
         .ifPresent(k -> builder.addHeader("x-instana-key", k));
    return new VirtualOtelFactory(builder.build());
  }

  @Bean
  public OTelServiceNodeAspect oTelServiceNodeAspect(VirtualOtelFactory factory) {
    return new OTelServiceNodeAspect(factory);
  }
}
