package com.workshop.gateway.model;

import java.time.OffsetDateTime;

public record FulfillmentView(
    Long id,
    String orderId,
    String status,
    long eventTimeMs,
    String shippingId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}

// Made with Bob
