package com.workshop.inventory.events;

import java.util.List;

public record InventoryRejectedEvent(
    String eventId,
    String eventType,
    String orderId,
    String reason,
    List<Item> requested,
    long eventTime
) {
  public record Item(String sku, Integer qty) {}
}
