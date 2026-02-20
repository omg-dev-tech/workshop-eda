package com.workshop.analytics.events;

import java.util.List;

public record OrderCreatedEvent(
    String eventId,
    String eventType,
    String orderId,
    String customerId,
    Long amount,
    String currency,
    List<Item> items,
    long eventTime
) {
  public record Item(String sku, Integer qty) {}
}

// Made with Bob
