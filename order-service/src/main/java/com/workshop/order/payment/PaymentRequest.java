package com.workshop.order.payment;

import java.util.List;

public record PaymentRequest(
    String orderId,
    Long amount,
    String currency,
    List<Item> items
) {
  public record Item(String sku, Integer qty) {}
}
