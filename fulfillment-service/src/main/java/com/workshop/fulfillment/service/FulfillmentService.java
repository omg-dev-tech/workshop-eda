package com.workshop.fulfillment.service;

import com.workshop.fulfillment.domain.FulfillmentEntity;
import com.workshop.fulfillment.domain.FulfillmentRepository;
import com.workshop.fulfillment.events.FulfillmentScheduledEvent;
import com.workshop.fulfillment.events.PaymentAuthorizedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentService {

  private final FulfillmentRepository repo;
  private final KafkaTemplate<String, Object> kafka;

  @Value("${app.event.ns:orders.v1}")
  private String ns;

  @Value("${app.fulfillment.scheduled-topic:${EVENT_NS:orders.v1}.fulfillment_scheduled}")
  private String scheduledTopic;

  @KafkaListener(
    topics = "${app.payment.authorized-topic}",
    groupId = "${spring.kafka.consumer.group-id}",
    properties = {"spring.json.value.default.type=com.workshop.fulfillment.events.PaymentAuthorizedEvent"}
  )
  @Transactional
  public void onPaymentAuthorized(PaymentAuthorizedEvent evt) {
    log.info("📦 [FULFILLMENT] received orderId={} items={}",
        evt.orderId(), evt.items() == null ? 0 : evt.items().size());

    var entity = repo.findByOrderId(evt.orderId())
        .orElseGet(() -> repo.save(FulfillmentEntity.builder()
            .orderId(evt.orderId())
            .status("PENDING")
            .eventTimeMs(evt.eventTimeMs())
            .build()));

    // 간단 스케줄링 시뮬레이션 → 즉시 SCHEDULED
    var shippingId = "shp-" + UUID.randomUUID();
    entity.setStatus("SCHEDULED");
    entity.setShippingId(shippingId);
    repo.save(entity);

    var scheduledItems = toScheduledItems(evt.items());

    var out = new FulfillmentScheduledEvent(
        UUID.randomUUID().toString(),
        ns + ".fulfillment_scheduled",
        evt.orderId(),
        shippingId,
        scheduledItems,
        System.currentTimeMillis()
    );
    kafka.send(scheduledTopic, evt.orderId(), out);
    log.info("✅ published {}", scheduledTopic);
  }

  // InventoryReservedEvent.Item -> FulfillmentScheduledEvent.Item 매핑
  private List<FulfillmentScheduledEvent.Item> toScheduledItems(List<PaymentAuthorizedEvent.Item> in) {
    if (in == null) return List.of();
    return in.stream()
        .map(i -> new FulfillmentScheduledEvent.Item(i.sku(), i.qty()))
        .collect(Collectors.toList());
  }
}
