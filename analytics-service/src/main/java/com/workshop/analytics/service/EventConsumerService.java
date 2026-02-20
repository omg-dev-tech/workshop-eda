package com.workshop.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.analytics.domain.EventLogEntity;
import com.workshop.analytics.domain.EventLogRepository;
import com.workshop.analytics.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumerService {

    private final EventLogRepository eventLogRepository;
    private final MetricsCollectorService metricsCollector;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.event.ns:com.workshop}.created", groupId = "analytics-service")
    @Transactional
    public void handleOrderCreated(String message) {
        try {
            log.info("Received order created event: {}", message);
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            
            // 이벤트 로그 저장
            saveEventLog(event.eventId(), "order.created", event.orderId(), message, event.eventTime());
            
            // 메트릭 수집
            metricsCollector.recordOrderCreated(event);
            
        } catch (Exception e) {
            log.error("Failed to process order created event", e);
        }
    }

    @KafkaListener(topics = "${app.event.ns:com.workshop}.inventory_reserved", groupId = "analytics-service")
    @Transactional
    public void handleInventoryReserved(String message) {
        try {
            log.info("Received inventory reserved event: {}", message);
            InventoryReservedEvent event = objectMapper.readValue(message, InventoryReservedEvent.class);
            
            saveEventLog(event.eventId(), "inventory.reserved", event.orderId(), message, event.eventTimeMs());
            metricsCollector.recordInventoryReserved(event);
            
        } catch (Exception e) {
            log.error("Failed to process inventory reserved event", e);
        }
    }

    @KafkaListener(topics = "${app.event.ns:com.workshop}.inventory_rejected", groupId = "analytics-service")
    @Transactional
    public void handleInventoryRejected(String message) {
        try {
            log.info("Received inventory rejected event: {}", message);
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            
            String eventId = (String) event.get("eventId");
            String orderId = (String) event.get("orderId");
            Long timestamp = ((Number) event.get("eventTimeMs")).longValue();
            
            saveEventLog(eventId, "inventory.rejected", orderId, message, timestamp);
            metricsCollector.recordInventoryRejected(orderId);
            
        } catch (Exception e) {
            log.error("Failed to process inventory rejected event", e);
        }
    }

    @KafkaListener(topics = "${app.event.ns:com.workshop}.payment_authorized", groupId = "analytics-service")
    @Transactional
    public void handlePaymentAuthorized(String message) {
        try {
            log.info("Received payment authorized event: {}", message);
            PaymentAuthorizedEvent event = objectMapper.readValue(message, PaymentAuthorizedEvent.class);
            
            saveEventLog(event.eventId(), "payment.authorized", event.orderId(), message, event.eventTimeMs());
            metricsCollector.recordPaymentAuthorized(event);
            
        } catch (Exception e) {
            log.error("Failed to process payment authorized event", e);
        }
    }

    @KafkaListener(topics = "${app.event.ns:com.workshop}.payment_failed", groupId = "analytics-service")
    @Transactional
    public void handlePaymentFailed(String message) {
        try {
            log.info("Received payment failed event: {}", message);
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            
            String eventId = (String) event.get("eventId");
            String orderId = (String) event.get("orderId");
            Long timestamp = ((Number) event.get("eventTimeMs")).longValue();
            
            saveEventLog(eventId, "payment.failed", orderId, message, timestamp);
            metricsCollector.recordPaymentFailed(orderId);
            
        } catch (Exception e) {
            log.error("Failed to process payment failed event", e);
        }
    }

    @KafkaListener(topics = "${app.event.ns:com.workshop}.fulfillment_scheduled", groupId = "analytics-service")
    @Transactional
    public void handleFulfillmentScheduled(String message) {
        try {
            log.info("Received fulfillment scheduled event: {}", message);
            FulfillmentScheduledEvent event = objectMapper.readValue(message, FulfillmentScheduledEvent.class);
            
            saveEventLog(event.eventId(), "fulfillment.scheduled", event.orderId(), message, event.eventTimeMs());
            metricsCollector.recordFulfillmentScheduled(event);
            
        } catch (Exception e) {
            log.error("Failed to process fulfillment scheduled event", e);
        }
    }

    private void saveEventLog(String eventId, String eventType, String aggregateId, String message, Long timestamp) {
        try {
            // 중복 체크
            if (eventLogRepository.findByEventId(eventId).isPresent()) {
                log.debug("Event already processed: {}", eventId);
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            
            EventLogEntity entity = EventLogEntity.builder()
                .eventId(eventId != null ? eventId : UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(payload)
                .timestamp(timestamp != null ? timestamp : System.currentTimeMillis())
                .build();
            
            eventLogRepository.save(entity);
            log.debug("Saved event log: {}", eventId);
            
        } catch (Exception e) {
            log.error("Failed to save event log", e);
        }
    }
}

// Made with Bob
