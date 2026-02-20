package com.workshop.analytics.events;

public record FulfillmentScheduledEvent(
    String eventId,
    String eventType,
    String orderId,
    String shippingId,
    long eventTimeMs
) {}

// Made with Bob
