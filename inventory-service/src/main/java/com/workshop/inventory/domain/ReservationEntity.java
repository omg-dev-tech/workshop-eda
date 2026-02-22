package com.workshop.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationEntity {
  @Id
  @Column(name = "id")
  @Builder.Default
  private String id = UUID.randomUUID().toString();

  @Column(name = "order_id")
  private String orderId;
  
  @Column(name = "sku")
  private String sku;
  
  @Column(name = "qty")
  private Integer qty;
  
  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;
}
