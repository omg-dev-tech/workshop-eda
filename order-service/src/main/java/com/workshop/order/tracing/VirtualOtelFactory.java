package com.workshop.order.tracing;

import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class VirtualOtelFactory {
  private final ConcurrentHashMap<String, Tracer> cache = new ConcurrentHashMap<>();
  private final SpanExporter exporter;

  public VirtualOtelFactory(String endpoint) {
    this.exporter = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build();
  }

  public Tracer tracer(String serviceName) {
    return cache.computeIfAbsent(serviceName, svc -> {
      var res = Resource.getDefault().merge(
          Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), svc)));
      var provider = SdkTracerProvider.builder()
          .setResource(res)
          .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
          .build();
      return OpenTelemetrySdk.builder().setTracerProvider(provider).build().getTracer("vsvc");
    });
  }
}
