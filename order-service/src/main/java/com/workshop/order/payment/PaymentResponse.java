package com.workshop.order.payment;

public record PaymentResponse(
    String status,     // AUTHORIZED | DECLINED
    String authId,
    String reason,
    long   eventTimeMs
) {}
