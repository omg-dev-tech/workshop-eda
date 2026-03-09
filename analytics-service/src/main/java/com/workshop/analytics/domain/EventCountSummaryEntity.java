package com.workshop.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이벤트 카운트 집계 테이블 엔티티
 * 
 * 성능 개선을 위한 집계 테이블
 * - 전체 테이블 스캔 대신 집계된 카운트를 빠르게 조회
 * - 이벤트 발생 시마다 실시간으로 업데이트
 * - eventType별 카운트를 사전 계산하여 저장
 */
@Entity
@Table(name = "event_count_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCountSummaryEntity {

    @Id
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "count", nullable = false)
    private Long count;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * 카운트 증가
     */
    public void incrementCount() {
        this.count++;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 카운트 증가 (지정된 값만큼)
     */
    public void incrementCount(long amount) {
        this.count += amount;
        this.lastUpdated = LocalDateTime.now();
    }
}

// Made with Bob