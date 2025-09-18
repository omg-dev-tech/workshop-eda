package com.workshop.order.tracing;

import org.apache.kafka.common.header.Headers;

public final class TraceHeaders {
  public static final String X_INSTANA_T = "X_INSTANA_T";
  public static final String X_INSTANA_S = "X_INSTANA_S";
  public static final String X_INSTANA_L_S = "X_INSTANA_L_S";
  public static final String TRACEPARENT = "traceparent";
  public static final String TRACESTATE  = "tracestate";

  public static String getString(Headers headers, String key) {
    var h = headers.lastHeader(key);
    return (h == null) ? null : new String(h.value(), java.nio.charset.StandardCharsets.UTF_8);
  }
}
