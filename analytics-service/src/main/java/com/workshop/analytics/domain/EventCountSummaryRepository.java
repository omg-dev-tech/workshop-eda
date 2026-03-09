package com.workshop.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 이벤트 카운트 집계 Repository
 * 
 * 성능 개선을 위한 집계 테이블 접근
 * - eventType별 카운트를 빠르게 조회
 * - 전체 카운트 합계 조회
 */
@Repository
public interface EventCountSummaryRepository extends JpaRepository<EventCountSummaryEntity, String> {

    /**
     * 특정 이벤트 타입의 카운트 조회
     */
    Optional<EventCountSummaryEntity> findByEventType(String eventType);

    /**
     * 전체 이벤트 카운트 합계 조회
     */
    @Query("SELECT SUM(e.count) FROM EventCountSummaryEntity e")
    Long getTotalCount();
}

// Made with Bob