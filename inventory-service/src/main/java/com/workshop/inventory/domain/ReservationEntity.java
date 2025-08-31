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
  @Builder.Default
  private String id = UUID.randomUUID().toString();

  private String orderId;
  private String sku;
  private Integer qty;
  private OffsetDateTime expiresAt;
}
