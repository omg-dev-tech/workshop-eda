package com.workshop.inventory.events;

import java.util.List;

public record PaymentFailedEvent(
    String eventId,
    String eventType,
    String orderId,
    String reason,
    List<Item> items,
    long eventTimeMs
) {
  public record Item(String sku, Integer qty) {}
}

// Made with Bob
