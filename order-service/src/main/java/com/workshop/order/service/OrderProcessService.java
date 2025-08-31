package com.workshop.order.service;

import com.workshop.order.domain.OrderEntity;
import com.workshop.order.domain.OrderRepository;
import com.workshop.order.domain.OrderStatus;
import com.workshop.order.events.*;
import com.workshop.order.payment.PaymentClient;
import com.workshop.order.payment.PaymentRequest;
import com.workshop.order.payment.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessService {

  private final OrderRepository orders;
  private final PaymentClient payment;
  private final KafkaTemplate<String, Object> kafka;

  @Value("${app.event.ns:orders.v1}") private String ns;
  @Value("${app.payment.authorized-topic}") private String paymentAuthorizedTopic;
  @Value("${app.payment.failed-topic}")     private String paymentFailedTopic;

  // 1) 재고 예약됨 → 결제 시도
  @KafkaListener(
      topics = "${app.inventory.reserved-topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      properties = {"spring.json.value.default.type=com.workshop.order.events.InventoryReservedEvent"}
  )
  @Transactional
  public void onInventoryReserved(InventoryReservedEvent evt) {
    log.info("🟩 onInventoryReserved orderId={}", evt.orderId());
    orders.findById(evt.orderId()).ifPresent(o -> {
      o.setStatus(OrderStatus.INVENTORY_RESERVED);
      orders.save(o);
    });

    // 결제 요청 본문 구성 (order 엔티티에서 금액/통화 가져와도 됨)
    var order = orders.findById(evt.orderId()).orElse(null);
    long amount = order != null ? order.getAmount() : 0L;
    String currency = order != null ? order.getCurrency() : "KRW";

    var req = new PaymentRequest(
        evt.orderId(),
        amount,
        currency,
        evt.reservations().stream()
            .map(i -> new PaymentRequest.Item(i.sku(), i.qty()))
            .toList()
    );

    PaymentResponse res = payment.authorize(req);
    if ("AUTHORIZED".equalsIgnoreCase(res.status())) {
      var out = new PaymentAuthorizedEvent(
          UUID.randomUUID().toString(),
          ns + ".payment_authorized",
          evt.orderId(),
          res.authId(),
          evt.reservations().stream().map(i -> new PaymentAuthorizedEvent.Item(i.sku(), i.qty())).toList(),
          System.currentTimeMillis()
      );
      kafka.send(paymentAuthorizedTopic, evt.orderId(), out);
      log.info("✅ published {}", paymentAuthorizedTopic);
    } else {
      var out = new PaymentFailedEvent(
          UUID.randomUUID().toString(),
          ns + ".payment_failed",
          evt.orderId(),
          res.reason(),
          evt.reservations().stream().map(i -> new PaymentFailedEvent.Item(i.sku(), i.qty())).toList(),
          System.currentTimeMillis()
      );
      kafka.send(paymentFailedTopic, evt.orderId(), out);
      log.info("🚫 published {}", paymentFailedTopic);
    }
  }

  // 2) 재고 거절됨 → 상태만 업데이트
  @KafkaListener(
      topics = "${app.inventory.rejected-topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      properties = {"spring.json.value.default.type=com.workshop.order.events.InventoryRejectedEvent"}
  )
  @Transactional
  public void onInventoryRejected(InventoryRejectedEvent evt) {
    log.info("🟥 onInventoryRejected orderId={} reason={}", evt.orderId(), evt.reason());
    orders.findById(evt.orderId()).ifPresent(o -> {
      o.setStatus(OrderStatus.INVENTORY_REJECTED);
      orders.save(o);
    });
  }

  // 3) (선택) fulfillment_scheduled 수신 시 완료 처리
  @KafkaListener(
      topics = "${app.fulfillment.scheduled-topic:${app.event.ns}.fulfillment_scheduled}",
      groupId = "${spring.kafka.consumer.group-id}",
      properties = {"spring.json.value.default.type=com.workshop.order.events.FulfillmentScheduledEvent"}
  )
  @Transactional
  public void onFulfillmentScheduled(FulfillmentScheduledEvent evt) {
    log.info("📦 onFulfillmentScheduled orderId={} shippingId={}", evt.orderId(), evt.shippingId());
    orders.findById(evt.orderId()).ifPresent(o -> {
      o.setStatus(OrderStatus.COMPLETED);
      orders.save(o);
    });
  }
}
