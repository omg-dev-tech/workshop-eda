package com.workshop.order.service;

import com.workshop.order.api.dto.CreateOrderReq;
import com.workshop.order.domain.OrderEntity;
import com.workshop.order.domain.OrderRepository;
import com.workshop.order.domain.OrderStatus;
import com.workshop.order.events.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OrderService {

  private final OrderRepository repo;
  private final KafkaTemplate<String, Object> kafka;
  private final String ns;

  public OrderService(OrderRepository repo, KafkaTemplate<String, Object> kafka,
                         @Value("${app.event.ns}") String ns) {
    this.repo = repo;
    this.kafka = kafka;
    this.ns = ns;
  }

  public OrderEntity create(CreateOrderReq req) {
    // 1) 주문 저장
    var entity = new OrderEntity();
    // ID는 @GeneratedValue로 자동 생성됨
    entity.setCustomerId(req.customerId());
    entity.setAmount(req.amount());
    entity.setCurrency(req.currency());
    entity.setStatus(OrderStatus.PENDING);
    var saved = repo.save(entity);

    // 2) 이벤트 발행 (orders.v1.created)
    publishOrderCreatedEvent(saved, req);

    return saved;
  }

  /**
   * 실패하거나 멈춘 주문을 재처리합니다.
   * 재고가 추가되거나 시스템이 복구된 후 관리자가 수동으로 호출할 수 있습니다.
   */
  public OrderEntity retry(UUID orderId) {
    var order = repo.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

    // 재처리 가능한 상태 확인
    if (order.getStatus() != OrderStatus.INVENTORY_REJECTED &&
        order.getStatus() != OrderStatus.PAYMENT_FAILED &&
        order.getStatus() != OrderStatus.INVENTORY_RESERVED &&
        order.getStatus() != OrderStatus.PENDING) {
      throw new IllegalStateException("Cannot retry order in status: " + order.getStatus());
    }

    // 상태를 PENDING으로 변경
    order.setStatus(OrderStatus.PENDING);
    var saved = repo.save(order);

    // OrderCreated 이벤트 재발행
    publishOrderCreatedEvent(saved, null);

    return saved;
  }

  private void publishOrderCreatedEvent(OrderEntity order, CreateOrderReq req) {
    long now = System.currentTimeMillis();
    var evt = new OrderCreatedEvent(
        UUID.randomUUID().toString(),
        ns + ".created",
        order.getId().toString(),
        order.getCustomerId(),
        order.getAmount(),
        order.getCurrency(),
        req != null && req.items() != null ?
            req.items().stream().map(i -> new OrderCreatedEvent.Item(i.sku(), i.qty())).toList() :
            java.util.List.of(),
        now
    );
    var topic = ns + ".created";
    kafka.send(topic, order.getId().toString(), evt);
  }
}
