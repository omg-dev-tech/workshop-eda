package com.workshop.order.api.dto;

import java.util.List;

public record CreateOrderReq(
    String customerId,
    Long amount,
    String currency,
    List<Item> items,
    Boolean forcePaymentFail
) {
  public record Item(String sku, Integer qty) {}
}
