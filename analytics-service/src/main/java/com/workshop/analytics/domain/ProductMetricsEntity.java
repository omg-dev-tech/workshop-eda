package com.workshop.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_metrics",
    uniqueConstraints = @UniqueConstraint(columnNames = {"sku", "date"}),
    indexes = {
        @Index(name = "idx_product_metrics_sku", columnList = "sku"),
        @Index(name = "idx_product_metrics_date", columnList = "date")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalSold = 0;

    @Column(nullable = false)
    @Builder.Default
    private Long totalRevenue = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// Made with Bob
