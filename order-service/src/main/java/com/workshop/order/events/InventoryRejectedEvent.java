package com.workshop.order.events;

import java.util.List;

public record InventoryRejectedEvent(
    String eventId,
    String eventType,
    String orderId,
    String reason,
    List<Item> items,
    long eventTimeMs
) {
  public record Item(String sku, Integer qty) {}
}
