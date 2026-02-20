package com.workshop.analytics.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BusinessMetrics {

    private final Meter meter;

    // Counters
    private final LongCounter orderCreatedCounter;
    private final LongCounter orderCompletedCounter;
    private final LongCounter orderFailedCounter;
    private final LongCounter inventoryReservedCounter;
    private final LongCounter inventoryRejectedCounter;
    private final LongCounter paymentAuthorizedCounter;
    private final LongCounter paymentFailedCounter;
    private final LongCounter fulfillmentScheduledCounter;

    // Histograms
    private final LongHistogram orderAmountHistogram;
    private final LongHistogram orderProcessingDuration;

    public BusinessMetrics(OpenTelemetry openTelemetry) {
        this.meter = openTelemetry.getMeter("analytics-business-metrics");

        // Initialize counters
        this.orderCreatedCounter = meter
            .counterBuilder("order.created.total")
            .setDescription("Total number of orders created")
            .setUnit("orders")
            .build();

        this.orderCompletedCounter = meter
            .counterBuilder("order.completed.total")
            .setDescription("Total number of orders completed")
            .setUnit("orders")
            .build();

        this.orderFailedCounter = meter
            .counterBuilder("order.failed.total")
            .setDescription("Total number of orders failed")
            .setUnit("orders")
            .build();

        this.inventoryReservedCounter = meter
            .counterBuilder("inventory.reserved.total")
            .setDescription("Total number of inventory reservations")
            .setUnit("reservations")
            .build();

        this.inventoryRejectedCounter = meter
            .counterBuilder("inventory.rejected.total")
            .setDescription("Total number of inventory rejections")
            .setUnit("rejections")
            .build();

        this.paymentAuthorizedCounter = meter
            .counterBuilder("payment.authorized.total")
            .setDescription("Total number of authorized payments")
            .setUnit("payments")
            .build();

        this.paymentFailedCounter = meter
            .counterBuilder("payment.failed.total")
            .setDescription("Total number of failed payments")
            .setUnit("payments")
            .build();

        this.fulfillmentScheduledCounter = meter
            .counterBuilder("fulfillment.scheduled.total")
            .setDescription("Total number of scheduled fulfillments")
            .setUnit("fulfillments")
            .build();

        // Initialize histograms
        this.orderAmountHistogram = meter
            .histogramBuilder("order.amount")
            .setDescription("Distribution of order amounts")
            .setUnit("currency")
            .ofLongs()
            .build();

        this.orderProcessingDuration = meter
            .histogramBuilder("order.processing.duration")
            .setDescription("Order processing duration")
            .setUnit("ms")
            .ofLongs()
            .build();

        log.info("Business metrics initialized");
    }

    public void recordOrderCreated(String customerId, long amount, String currency) {
        orderCreatedCounter.add(1, Attributes.of(
            AttributeKey.stringKey("customer.id"), customerId,
            AttributeKey.stringKey("currency"), currency
        ));
        orderAmountHistogram.record(amount, Attributes.of(
            AttributeKey.stringKey("currency"), currency
        ));
    }

    public void recordOrderCompleted(String orderId) {
        orderCompletedCounter.add(1, Attributes.of(
            AttributeKey.stringKey("order.id"), orderId
        ));
    }

    public void recordOrderFailed(String orderId, String reason) {
        orderFailedCounter.add(1, Attributes.of(
            AttributeKey.stringKey("order.id"), orderId,
            AttributeKey.stringKey("failure.reason"), reason
        ));
    }

    public void recordInventoryReserved(String orderId, int itemCount) {
        inventoryReservedCounter.add(1, Attributes.of(
            AttributeKey.stringKey("order.id"), orderId,
            AttributeKey.longKey("item.count"), (long) itemCount
        ));
    }

    public void recordInventoryRejected(String orderId) {
        inventoryRejectedCounter.add(1, Attributes.of(
            AttributeKey.stringKey("order.id"), orderId
        ));
    }

    public void recordPaymentAuthorized(String orderId, String authId) {
        paymentAuthorizedCounter.add(1, Attributes.of(
            AttributeKey.stringKey("order.id"), orderId,
            AttributeKey.stringKey("auth.id"), authId
        ));
    }

    public void recordPaymentFailed(String orderId, String reason) {
        paymentFailedCounter.add(1, Attributes.of(
            AttributeKey.stringKey("order.id"), orderId,
            AttributeKey.stringKey("failure.reason"), reason
        ));
    }

    public void recordFulfillmentScheduled(String orderId, String shippingId) {
        fulfillmentScheduledCounter.add(1, Attributes.of(
            AttributeKey.stringKey("order.id"), orderId,
            AttributeKey.stringKey("shipping.id"), shippingId
        ));
    }

    public void recordOrderProcessingDuration(String orderId, long durationMs) {
        orderProcessingDuration.record(durationMs, Attributes.of(
            AttributeKey.stringKey("order.id"), orderId
        ));
    }
}

// Made with Bob
