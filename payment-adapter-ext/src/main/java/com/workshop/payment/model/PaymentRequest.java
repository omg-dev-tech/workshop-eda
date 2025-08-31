package com.workshop.payment.model;

import java.util.List;
import jakarta.validation.constraints.*;

public record PaymentRequest(
    @NotBlank String orderId,
    @NotNull @Positive Long amount,
    @NotBlank String currency,
    List<Item> items
) {
  public record Item(@NotBlank String sku, @NotNull @Positive Integer qty) {}
}
