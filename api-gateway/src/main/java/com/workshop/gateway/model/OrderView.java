package com.workshop.gateway.model;

import java.time.OffsetDateTime;

// order-service의 GET /orders/{id}가 반환할 최소 필드 가정
public record OrderView(
    String id,
    String customerId,
    long amount,
    String currency,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}