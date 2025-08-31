package com.workshop.payment.model;

public record PaymentResponse(
    String status,        // AUTHORIZED | DECLINED
    String authId,        // 성공 시 pay-*
    String reason,        // 실패 사유(선택)
    long   eventTimeMs
) {}
