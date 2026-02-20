package com.workshop.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_metrics", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"date", "hour"}),
    indexes = @Index(name = "idx_order_metrics_date", columnList = "date")
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer hour;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer completedOrders = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer failedOrders = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer cancelledOrders = 0;

    @Column(nullable = false)
    @Builder.Default
    private Long totalAmount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long avgProcessingTimeMs = 0L;

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
