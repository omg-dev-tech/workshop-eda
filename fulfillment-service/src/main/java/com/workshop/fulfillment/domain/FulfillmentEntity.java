package com.workshop.fulfillment.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fulfillments", indexes = {
    @Index(name = "idx_fulfillment_order", columnList = "orderId", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FulfillmentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String orderId;

  @Column(nullable = false, length = 32)
  private String status;     // PENDING, SCHEDULED, FAILED

  @Column(nullable = false)
  private long eventTimeMs;

  @Column(length = 64)
  private String shippingId;
}
