package com.workshop.inventory.service;

import com.workshop.inventory.domain.*;
import com.workshop.inventory.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

  private final InventoryRepository inventoryRepo;
  private final ReservationRepository reservationRepo;
  private final KafkaTemplate<String, Object> kafka;

  @Value("${app.event.ns}")
  private String ns;

  @Value("${app.payment.failed-topic:${app.event.ns}.payment_failed}")
  private String paymentFailedTopic;

  private String reservedTopic() { return ns + ".inventory_reserved"; }
  private String rejectedTopic() { return ns + ".inventory_rejected"; }

  @KafkaListener(
      topics = "${app.order.created-topic}",
      groupId = "${spring.kafka.consumer.group-id}"
  )
  @Transactional
  public void onOrderCreated(OrderCreatedEvent evt) {
    log.info("Receive Event {}", evt);
    boolean allOk = evt.items() == null || evt.items().stream().allMatch(this::hasStock);
    log.info("allOk: {}", allOk);

    if (allOk) {
      if (evt.items() != null) {
        for (var it : evt.items()) {
          log.info("Save inventory");
          var inv = inventoryRepo.findById(it.sku()).orElseGet(() -> {
            var newInv = new InventoryEntity();
            newInv.setSku(it.sku());
            newInv.setQty(0);
            return newInv;
          });
          inv.setQty(inv.getQty() - it.qty());
          inventoryRepo.save(inv);

          reservationRepo.save(ReservationEntity.builder()
              .orderId(evt.orderId())
              .sku(it.sku())
              .qty(it.qty())
              .expiresAt(OffsetDateTime.now().plusMinutes(5))
              .build());
        }
      }

      var out = new InventoryReservedEvent(
          UUID.randomUUID().toString(),
          reservedTopic(),
          evt.orderId(),
          evt.items() == null ? List.of() :
              evt.items().stream()
                  .map(i -> new InventoryReservedEvent.Item(i.sku(), i.qty()))
                  .toList(),
          System.currentTimeMillis()
      );
      kafka.send(reservedTopic(), evt.orderId(), out);
      log.info("Send Reserved Event");

    } else {
      var out = new InventoryRejectedEvent(
          UUID.randomUUID().toString(),
          rejectedTopic(),
          evt.orderId(),
          "OUT_OF_STOCK",
          evt.items() == null ? List.of() :
              evt.items().stream()
                  .map(i -> new InventoryRejectedEvent.Item(i.sku(), i.qty()))
                  .toList(),
          System.currentTimeMillis()
      );
      kafka.send(rejectedTopic(), evt.orderId(), out);
      log.info("Send Rejected Event");
    }
  }

  private boolean hasStock(OrderCreatedEvent.Item item) {
    log.info("Item is {}", item);
    log.info("DB Item is {}", inventoryRepo.findById(item.sku()));
    return inventoryRepo.findById(item.sku())
        .map(inv -> inv.getQty() >= item.qty())
        .orElse(false);
  }

  /**
   * 결제 실패 시 예약된 재고를 롤백합니다.
   */
  @KafkaListener(
      topics = "${app.payment.failed-topic:${app.event.ns}.payment_failed}",
      groupId = "${spring.kafka.consumer.group-id}",
      properties = {"spring.json.value.default.type=com.workshop.inventory.events.PaymentFailedEvent"}
  )
  @Transactional
  public void onPaymentFailed(PaymentFailedEvent evt) {
    log.info("🔄 onPaymentFailed orderId={} reason={}", evt.orderId(), evt.reason());
    
    // 예약된 재고 찾기
    List<ReservationEntity> reservations = reservationRepo.findByOrderId(evt.orderId());
    
    if (reservations.isEmpty()) {
      log.warn("⚠️ No reservations found for orderId={}", evt.orderId());
      return;
    }
    
    // 재고 롤백
    for (ReservationEntity reservation : reservations) {
      inventoryRepo.findById(reservation.getSku()).ifPresent(inv -> {
        inv.setQty(inv.getQty() + reservation.getQty());
        inventoryRepo.save(inv);
        log.info("✅ Rolled back inventory: sku={} qty={}", reservation.getSku(), reservation.getQty());
      });
      
      // 예약 삭제
      reservationRepo.delete(reservation);
    }
    
    log.info("✅ Inventory rollback completed for orderId={}", evt.orderId());
  }
}
