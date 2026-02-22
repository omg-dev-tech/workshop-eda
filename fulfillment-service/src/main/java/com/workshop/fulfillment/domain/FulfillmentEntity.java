package com.workshop.fulfillment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

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

  @Column(nullable = false, updatable = false)
  private OffsetDateTime createdAt;
  
  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = OffsetDateTime.now();
    updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
