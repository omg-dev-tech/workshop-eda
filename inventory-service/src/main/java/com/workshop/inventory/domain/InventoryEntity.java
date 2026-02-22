package com.workshop.inventory.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryEntity {
  @Id
  @Column(name = "sku")
  private String sku;
  
  @Column(name = "product_name")
  private String productName;
  
  @Column(name = "qty")
  private Integer qty;
}
