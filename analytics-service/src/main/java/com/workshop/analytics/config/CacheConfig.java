package com.workshop.analytics.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정
 * 
 * 성능 개선을 위한 Spring Cache 설정
 * - eventCount 캐시: 이벤트 카운트 조회 결과를 60초간 캐싱
 * - Caffeine 캐시 사용으로 높은 성능과 메모리 효율성 제공
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("eventCount");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)  // TTL: 60초
                .maximumSize(1000)  // 최대 1000개 엔트리
                .recordStats();  // 캐시 통계 기록
    }
}

// Made with Bob