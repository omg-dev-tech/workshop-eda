package com.workshop.analytics.service;

import com.workshop.analytics.domain.*;
import com.workshop.analytics.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 메트릭 수집 서비스
 * 모든 시간대 처리는 한국 시간(Asia/Seoul)을 기준으로 합니다.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsCollectorService {

    // 한국 시간대 (UTC+9)
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final OrderMetricsRepository orderMetricsRepository;
    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void recordOrderCreated(OrderCreatedEvent event) {
        try {
            // 한국 시간대로 변환
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.eventTime()),
                KOREA_ZONE
            );
            LocalDate date = dateTime.toLocalDate();
            int hour = dateTime.getHour();
            
            log.debug("Recording order created: orderId={}, eventTime={}, KST date={}, hour={}",
                event.orderId(), event.eventTime(), date, hour);

            // Order Metrics 업데이트
            OrderMetricsEntity metrics = orderMetricsRepository
                .findByDateAndHour(date, hour)
                .orElse(OrderMetricsEntity.builder()
                    .date(date)
                    .hour(hour)
                    .totalOrders(0)
                    .completedOrders(0)
                    .failedOrders(0)
                    .cancelledOrders(0)
                    .totalAmount(0L)
                    .avgProcessingTimeMs(0L)
                    .build());

            metrics.setTotalOrders(metrics.getTotalOrders() + 1);
            metrics.setTotalAmount(metrics.getTotalAmount() + event.amount());
            orderMetricsRepository.save(metrics);

            // Product Metrics 업데이트
            for (OrderCreatedEvent.Item item : event.items()) {
                updateProductMetrics(item.sku(), date, item.qty(), event.amount() / event.items().size());
            }

            log.debug("Recorded order created metrics for order: {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to record order created metrics", e);
        }
    }

    @Transactional
    public void recordInventoryReserved(InventoryReservedEvent event) {
        try {
            log.debug("Recorded inventory reserved metrics for order: {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to record inventory reserved metrics", e);
        }
    }

    @Transactional
    public void recordInventoryRejected(String orderId) {
        try {
            // 한국 시간대 기준 현재 시간
            LocalDateTime dateTime = LocalDateTime.now(KOREA_ZONE);
            LocalDate date = dateTime.toLocalDate();
            int hour = dateTime.getHour();

            OrderMetricsEntity metrics = orderMetricsRepository
                .findByDateAndHour(date, hour)
                .orElse(OrderMetricsEntity.builder()
                    .date(date)
                    .hour(hour)
                    .totalOrders(0)
                    .completedOrders(0)
                    .failedOrders(0)
                    .cancelledOrders(0)
                    .totalAmount(0L)
                    .avgProcessingTimeMs(0L)
                    .build());

            metrics.setFailedOrders(metrics.getFailedOrders() + 1);
            orderMetricsRepository.save(metrics);

            log.debug("Recorded inventory rejected metrics for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to record inventory rejected metrics", e);
        }
    }

    @Transactional
    public void recordPaymentAuthorized(PaymentAuthorizedEvent event) {
        try {
            log.debug("Recorded payment authorized metrics for order: {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to record payment authorized metrics", e);
        }
    }

    @Transactional
    public void recordPaymentFailed(String orderId) {
        try {
            // 한국 시간대 기준 현재 시간
            LocalDateTime dateTime = LocalDateTime.now(KOREA_ZONE);
            LocalDate date = dateTime.toLocalDate();
            int hour = dateTime.getHour();

            OrderMetricsEntity metrics = orderMetricsRepository
                .findByDateAndHour(date, hour)
                .orElse(OrderMetricsEntity.builder()
                    .date(date)
                    .hour(hour)
                    .totalOrders(0)
                    .completedOrders(0)
                    .failedOrders(0)
                    .cancelledOrders(0)
                    .totalAmount(0L)
                    .avgProcessingTimeMs(0L)
                    .build());

            metrics.setFailedOrders(metrics.getFailedOrders() + 1);
            orderMetricsRepository.save(metrics);

            log.debug("Recorded payment failed metrics for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to record payment failed metrics", e);
        }
    }

    @Transactional
    public void recordFulfillmentScheduled(FulfillmentScheduledEvent event) {
        try {
            // 한국 시간대로 변환
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.eventTimeMs()),
                KOREA_ZONE
            );
            LocalDate date = dateTime.toLocalDate();
            int hour = dateTime.getHour();
            
            log.debug("Recording fulfillment scheduled: orderId={}, eventTime={}, KST date={}, hour={}",
                event.orderId(), event.eventTimeMs(), date, hour);

            OrderMetricsEntity metrics = orderMetricsRepository
                .findByDateAndHour(date, hour)
                .orElse(OrderMetricsEntity.builder()
                    .date(date)
                    .hour(hour)
                    .totalOrders(0)
                    .completedOrders(0)
                    .failedOrders(0)
                    .cancelledOrders(0)
                    .totalAmount(0L)
                    .avgProcessingTimeMs(0L)
                    .build());

            metrics.setCompletedOrders(metrics.getCompletedOrders() + 1);
            orderMetricsRepository.save(metrics);

            log.debug("Recorded fulfillment scheduled metrics for order: {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to record fulfillment scheduled metrics", e);
        }
    }

    private void updateProductMetrics(String sku, LocalDate date, int quantity, long revenue) {
        ProductMetricsEntity metrics = productMetricsRepository
            .findBySkuAndDate(sku, date)
            .orElse(ProductMetricsEntity.builder()
                .sku(sku)
                .date(date)
                .totalSold(0)
                .totalRevenue(0L)
                .build());

        metrics.setTotalSold(metrics.getTotalSold() + quantity);
        metrics.setTotalRevenue(metrics.getTotalRevenue() + revenue);
        productMetricsRepository.save(metrics);
    }
}

// Made with Bob
