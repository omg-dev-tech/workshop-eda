package com.workshop.gateway.model;

import jakarta.validation.constraints.*;
import java.util.List;

public record OrderCreateRequest(
    @NotBlank String customerId,
    @NotNull @Positive Long amount,
    @NotBlank String currency,
    @NotNull @Size(min=1) List<Item> items
) {
  public record Item(@NotBlank String sku, @NotNull @Positive Integer qty) {}
}
