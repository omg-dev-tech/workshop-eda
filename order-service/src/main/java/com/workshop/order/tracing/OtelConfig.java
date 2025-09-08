package com.workshop.order.tracing;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

@Configuration
public class OtelConfig {
    
    // @Bean
    // VirtualOtelFactory virtualOtelFactory() {
    // String ep = System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT",
    //             "http://instana-agent.instana-agent:4317");
    // return new VirtualOtelFactory(ep);
    // }
}
