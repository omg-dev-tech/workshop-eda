package com.workshop.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderMetricsRepository extends JpaRepository<OrderMetricsEntity, Long> {

    Optional<OrderMetricsEntity> findByDateAndHour(LocalDate date, Integer hour);

    List<OrderMetricsEntity> findByDate(LocalDate date);

    @Query("SELECT o FROM OrderMetricsEntity o WHERE o.date >= :startDate AND o.date <= :endDate ORDER BY o.date, o.hour")
    List<OrderMetricsEntity> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT SUM(o.totalOrders) FROM OrderMetricsEntity o WHERE o.date = :date")
    Long getTotalOrdersByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(o.totalAmount) FROM OrderMetricsEntity o WHERE o.date = :date")
    Long getTotalAmountByDate(@Param("date") LocalDate date);
}

// Made with Bob
