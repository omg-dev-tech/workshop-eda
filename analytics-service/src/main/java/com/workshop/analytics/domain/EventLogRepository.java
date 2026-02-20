package com.workshop.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventLogRepository extends JpaRepository<EventLogEntity, Long> {

    Optional<EventLogEntity> findByEventId(String eventId);

    List<EventLogEntity> findByEventType(String eventType);

    List<EventLogEntity> findByAggregateId(String aggregateId);

    @Query("SELECT e FROM EventLogEntity e WHERE e.eventType = :eventType AND e.timestamp >= :startTime AND e.timestamp <= :endTime ORDER BY e.timestamp DESC")
    List<EventLogEntity> findByEventTypeAndTimeRange(
        @Param("eventType") String eventType,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );

    @Query("SELECT COUNT(e) FROM EventLogEntity e WHERE e.eventType = :eventType")
    Long countByEventType(@Param("eventType") String eventType);
}

// Made with Bob
