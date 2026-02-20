package com.workshop.inventory.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryEntity {
  @Id
  private String sku;
  
  private String productName;
  private Integer qty;
}
