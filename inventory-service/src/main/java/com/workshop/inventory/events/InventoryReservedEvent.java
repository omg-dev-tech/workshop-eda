package com.workshop.inventory.events;

import java.util.List;

public record InventoryReservedEvent(
    String eventId,
    String eventType,
    String orderId,
    List<Item> reservations,
    long eventTime
) {
  public record Item(String sku, Integer qty) {}
}
