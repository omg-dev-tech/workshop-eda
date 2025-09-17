package com.workshop.order.tracing;

import java.lang.management.ManagementFactory;
import java.util.UUID;
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
  private static final AttributeKey<String> SERVICE_NAME       = AttributeKey.stringKey("service.name");
  private static final AttributeKey<String> SERVICE_INSTANCEID = AttributeKey.stringKey("service.instance.id");
  private static final AttributeKey<String> DEPLOY_ENV         = AttributeKey.stringKey("deployment.environment");

  private final ConcurrentHashMap<String, Tracer> cache = new ConcurrentHashMap<>();
  private final SpanExporter exporter;
  private final String environment;

  public VirtualOtelFactory(OtlpGrpcSpanExporter exporter) {
    this(exporter, System.getenv().getOrDefault("OTEL_DEPLOYMENT_ENVIRONMENT", "dev"));
  }
  public VirtualOtelFactory(SpanExporter exporter, String environment) {
    this.exporter = exporter;
    this.environment = environment;
  }

  public Tracer tracer(String virtualServiceName) {
    return cache.computeIfAbsent(virtualServiceName, svc -> {
      // 각 가상 서비스마다 고유 인스턴스 부여
      String pid = ManagementFactory.getRuntimeMXBean().getName(); // "pid@host"
      String instanceId = svc + ":" + pid + ":" + UUID.randomUUID();

      Resource vsvcRes = Resource.getDefault().merge(
          Resource.create(Attributes.of(
              SERVICE_NAME,       svc,
              SERVICE_INSTANCEID, instanceId,
              DEPLOY_ENV,         environment
          ))
      );

      SdkTracerProvider provider = SdkTracerProvider.builder()
          .setResource(vsvcRes)
          .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
          .build();

      return OpenTelemetrySdk.builder()
          .setTracerProvider(provider)
          .build()
          .getTracer("vsvc");
    });
  }
}
