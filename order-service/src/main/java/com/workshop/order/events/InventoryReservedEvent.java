package com.workshop.order.events;

import java.util.List;

public record InventoryReservedEvent(
    String eventId,
    String eventType,
    String orderId,
    List<Item> reservations,
    long eventTimeMs
) {
  public record Item(String sku, Integer qty) {}
}
