package com.workshop.order.tracing;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;

@Configuration
public class OtelConfig {

  @Bean
  public VirtualOtelFactory virtualOtelFactory(
      @Value("${OTEL_EXPORTER_OTLP_ENDPOINT}") String endpoint,
      @Value("${INSTANA_API_KEY:}") Optional<String> apiKey
  ) {
    var builder = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint.length() == 0 ? "http://127.0.0.1:9999" : endpoint);
    apiKey.filter(k -> !k.isBlank()).ifPresent(k -> builder.addHeader("x-instana-key", k));
    return new VirtualOtelFactory(builder.build());
  }

  /** 앱 시작 시 Aspect로 브리지 주입 */
  @Bean
  public ApplicationRunner initAspectBridge(VirtualOtelFactory factory) {
    return args -> OTelServiceNodeAspect.setFactory(factory);
  }
}
