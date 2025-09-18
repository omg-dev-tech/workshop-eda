package com.workshop.order.tracing;

import io.opentelemetry.context.Context;

public final class ServiceChainContext {
  private static final ThreadLocal<Context> PARENT = new ThreadLocal<>();
  private ServiceChainContext() {}

  public static Context getOrCurrent() {
    Context ctx = PARENT.get();
    return ctx != null ? ctx : Context.current();
  }
  public static void set(Context ctx) { PARENT.set(ctx); }
  public static void clear() { PARENT.remove(); }
}
