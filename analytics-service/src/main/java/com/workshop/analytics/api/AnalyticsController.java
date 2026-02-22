package com.workshop.analytics.api;

import com.workshop.analytics.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final EventLogRepository eventLogRepository;
    private final OrderMetricsRepository orderMetricsRepository;
    private final ProductMetricsRepository productMetricsRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "analytics-service");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventLogEntity>> getEvents(
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String aggregateId
    ) {
        if (eventType != null) {
            return ResponseEntity.ok(eventLogRepository.findByEventType(eventType));
        } else if (aggregateId != null) {
            return ResponseEntity.ok(eventLogRepository.findByAggregateId(aggregateId));
        }
        return ResponseEntity.ok(eventLogRepository.findAll());
    }

    @GetMapping("/events/count")
    public ResponseEntity<Map<String, Long>> getEventCount(
        @RequestParam(required = false) String eventType
    ) {
        Map<String, Long> response = new HashMap<>();
        if (eventType != null) {
            response.put("count", eventLogRepository.countByEventType(eventType));
        } else {
            response.put("count", eventLogRepository.count());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/metrics")
    public ResponseEntity<List<OrderMetricsEntity>> getOrderMetrics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(orderMetricsRepository.findByDate(date));
    }

    @GetMapping("/orders/metrics/range")
    public ResponseEntity<List<OrderMetricsEntity>> getOrderMetricsRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(orderMetricsRepository.findByDateRange(startDate, endDate));
    }

    @GetMapping("/orders/summary")
    public ResponseEntity<Map<String, Object>> getOrderSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("Fetching order summary for date: {}", date);
        
        // 해당 날짜의 모든 메트릭 조회 (디버깅용)
        List<OrderMetricsEntity> metrics = orderMetricsRepository.findByDate(date);
        log.info("Found {} order metrics entries for date {}", metrics.size(), date);
        
        Long totalOrders = orderMetricsRepository.getTotalOrdersByDate(date);
        Long totalAmount = orderMetricsRepository.getTotalAmountByDate(date);
        
        log.info("Query results - totalOrders: {}, totalAmount: {}", totalOrders, totalAmount);
        
        Map<String, Object> response = new HashMap<>();
        response.put("date", date);
        response.put("totalOrders", totalOrders != null ? totalOrders : 0);
        response.put("totalAmount", totalAmount != null ? totalAmount : 0);
        response.put("metricsCount", metrics.size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/metrics")
    public ResponseEntity<List<ProductMetricsEntity>> getProductMetrics(
        @RequestParam(required = false) String sku,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (sku != null && date != null) {
            return ResponseEntity.ok(
                productMetricsRepository.findBySkuAndDate(sku, date)
                    .map(List::of)
                    .orElse(List.of())
            );
        } else if (sku != null) {
            return ResponseEntity.ok(productMetricsRepository.findBySku(sku));
        } else if (date != null) {
            return ResponseEntity.ok(productMetricsRepository.findByDate(date));
        }
        return ResponseEntity.ok(productMetricsRepository.findAll());
    }

    @GetMapping("/products/top-selling")
    public ResponseEntity<List<ProductMetricsEntity>> getTopSellingProducts(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<ProductMetricsEntity> products = productMetricsRepository
            .findTopSellingProducts(startDate, endDate);
        
        return ResponseEntity.ok(
            products.stream()
                .limit(limit)
                .toList()
        );
    }

    @GetMapping("/products/{sku}/total-sold")
    public ResponseEntity<Map<String, Object>> getProductTotalSold(
        @PathVariable String sku,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long totalSold = productMetricsRepository.getTotalSoldBySku(sku, startDate, endDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sku", sku);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("totalSold", totalSold != null ? totalSold : 0);
        
        return ResponseEntity.ok(response);
    }
}

// Made with Bob
