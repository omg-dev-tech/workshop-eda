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
    long now = System.currentTimeMillis();
    var evt = new OrderCreatedEvent(
        UUID.randomUUID().toString(),
        ns + ".created",
        saved.getId().toString(),  // UUID를 String으로 변환
        saved.getCustomerId(),
        saved.getAmount(),
        saved.getCurrency(),
        req.items() == null ? java.util.List.of() :
            req.items().stream().map(i -> new OrderCreatedEvent.Item(i.sku(), i.qty())).toList(),
        now
    );
    var topic = ns + ".created";
    kafka.send(topic, saved.getId().toString(), evt);  // UUID를 String으로 변환

    return saved;
  }
}
