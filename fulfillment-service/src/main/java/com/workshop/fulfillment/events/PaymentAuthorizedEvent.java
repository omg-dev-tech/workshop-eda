package com.workshop.fulfillment.events;

import java.util.List;

public record PaymentAuthorizedEvent(
    String eventId,
    String eventType,
    String orderId,
    String authId,
    List<Item> items,
    long eventTimeMs
) {
  public record Item(String sku, Integer qty) {}
}
