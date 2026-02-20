package com.workshop.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductMetricsRepository extends JpaRepository<ProductMetricsEntity, Long> {

    Optional<ProductMetricsEntity> findBySkuAndDate(String sku, LocalDate date);

    List<ProductMetricsEntity> findBySku(String sku);

    List<ProductMetricsEntity> findByDate(LocalDate date);

    @Query("SELECT p FROM ProductMetricsEntity p WHERE p.date >= :startDate AND p.date <= :endDate ORDER BY p.totalRevenue DESC")
    List<ProductMetricsEntity> findTopSellingProducts(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT SUM(p.totalSold) FROM ProductMetricsEntity p WHERE p.sku = :sku AND p.date >= :startDate AND p.date <= :endDate")
    Long getTotalSoldBySku(
        @Param("sku") String sku,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}

// Made with Bob
