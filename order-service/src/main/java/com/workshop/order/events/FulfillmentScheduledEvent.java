package com.workshop.order.events;

import java.util.List;

public record FulfillmentScheduledEvent(
    String eventId,
    String eventType,
    String orderId,
    String shippingId,
    List<Item> items,
    long eventTimeMs
) {
  public record Item(String sku, Integer qty) {}
}
