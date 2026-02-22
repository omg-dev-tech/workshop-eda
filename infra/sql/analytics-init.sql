CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Analytics Database Initialization Script

-- Create database (run as postgres user)
CREATE DATABASE analytics_db;
CREATE USER analytics WITH PASSWORD 'analytics123';
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO analytics;

-- Connect to analytics_db and run the following:

-- Event Log Table
-- 모든 도메인 이벤트를 저장하는 테이블
CREATE TABLE IF NOT EXISTS event_log (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_type ON event_log(event_type);
CREATE INDEX IF NOT EXISTS idx_aggregate_id ON event_log(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_timestamp ON event_log(timestamp);

-- Order Metrics Table
-- 시간대별 주문 통계
CREATE TABLE IF NOT EXISTS order_metrics (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    hour INT NOT NULL,
    total_orders INT DEFAULT 0,
    completed_orders INT DEFAULT 0,
    failed_orders INT DEFAULT 0,
    cancelled_orders INT DEFAULT 0,
    total_amount BIGINT DEFAULT 0,
    avg_processing_time_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(date, hour)
);

-- Product Metrics Table
-- 상품별 판매 통계
CREATE TABLE IF NOT EXISTS product_metrics (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    total_sold INT DEFAULT 0,
    total_revenue BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(sku, date)
);

-- Customer Metrics Table
-- 고객별 구매 통계
CREATE TABLE IF NOT EXISTS customer_metrics (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    total_orders INT DEFAULT 0,
    total_amount BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(customer_id, date)
);

-- Payment Metrics Table
-- 결제 통계
CREATE TABLE IF NOT EXISTS payment_metrics (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    hour INT NOT NULL,
    total_attempts INT DEFAULT 0,
    successful_payments INT DEFAULT 0,
    failed_payments INT DEFAULT 0,
    total_amount BIGINT DEFAULT 0,
    avg_processing_time_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(date, hour)
);

-- Inventory Metrics Table
-- 재고 통계
CREATE TABLE IF NOT EXISTS inventory_metrics (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    hour INT NOT NULL,
    total_checks INT DEFAULT 0,
    successful_reservations INT DEFAULT 0,
    rejected_reservations INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(date, hour)
);

-- Fulfillment Metrics Table
-- 배송 통계
CREATE TABLE IF NOT EXISTS fulfillment_metrics (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    hour INT NOT NULL,
    total_scheduled INT DEFAULT 0,
    total_shipped INT DEFAULT 0,
    total_delivered INT DEFAULT 0,
    avg_processing_time_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(date, hour)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_order_metrics_date ON order_metrics(date);
CREATE INDEX IF NOT EXISTS idx_product_metrics_sku ON product_metrics(sku);
CREATE INDEX IF NOT EXISTS idx_product_metrics_date ON product_metrics(date);
CREATE INDEX IF NOT EXISTS idx_customer_metrics_customer_id ON customer_metrics(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_metrics_date ON customer_metrics(date);
CREATE INDEX IF NOT EXISTS idx_payment_metrics_date ON payment_metrics(date);
CREATE INDEX IF NOT EXISTS idx_inventory_metrics_date ON inventory_metrics(date);
CREATE INDEX IF NOT EXISTS idx_fulfillment_metrics_date ON fulfillment_metrics(date);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO analytics;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO analytics;

-- Made with Bob
