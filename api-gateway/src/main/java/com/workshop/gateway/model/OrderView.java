package com.workshop.gateway.model;

// order-service의 GET /orders/{id}가 반환할 최소 필드 가정
public record OrderView(
    String id,
    String customerId,
    long amount,
    String currency,
    String status
) {}