package com.workshop.analytics.api;

import com.workshop.analytics.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final EventCountSummaryRepository eventCountSummaryRepository;
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

    /**
     * 이벤트 카운트 조회 API (성능 개선 버전)
     *
     * 성능 개선 사항:
     * 1. Spring Cache 적용 (60초 TTL)
     * 2. 집계 테이블(event_count_summary) 우선 조회
     * 3. 집계 테이블이 없는 경우 기존 방식으로 fallback
     *
     * 예상 성능 향상:
     * - 캐시 히트 시: ~1ms (기존 대비 100배 이상 빠름)
     * - 집계 테이블 조회 시: ~10ms (기존 대비 10배 이상 빠름)
     * - Fallback 시: 기존과 동일
     */
    @GetMapping("/events/count")
    @Cacheable(value = "eventCount", key = "#eventType != null ? #eventType : 'all'")
    public ResponseEntity<Map<String, Long>> getEventCount(
        @RequestParam(required = false) String eventType
    ) {
        log.debug("Fetching event count for eventType: {}", eventType);
        
        Map<String, Long> response = new HashMap<>();
        Long count;
        
        try {
            if (eventType != null) {
                // 특정 이벤트 타입의 카운트 조회
                count = eventCountSummaryRepository.findByEventType(eventType)
                    .map(EventCountSummaryEntity::getCount)
                    .orElseGet(() -> {
                        // 집계 테이블에 없으면 실시간 조회 (fallback)
                        log.warn("Event count summary not found for type: {}, falling back to real-time count", eventType);
                        return eventLogRepository.countByEventType(eventType);
                    });
            } else {
                // 전체 이벤트 카운트 조회
                count = eventCountSummaryRepository.getTotalCount();
                if (count == null) {
                    // 집계 테이블이 비어있으면 실시간 조회 (fallback)
                    log.warn("Event count summary is empty, falling back to real-time count");
                    count = eventLogRepository.count();
                }
            }
            
            response.put("count", count);
            log.debug("Event count result: {}", count);
            
        } catch (Exception e) {
            // 에러 발생 시 기존 방식으로 fallback
            log.error("Error fetching from event count summary, falling back to real-time count", e);
            if (eventType != null) {
                count = eventLogRepository.countByEventType(eventType);
            } else {
                count = eventLogRepository.count();
            }
            response.put("count", count);
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
