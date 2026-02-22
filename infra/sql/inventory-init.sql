CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =================================================================
-- Inventory Service Database Schema
-- =================================================================

-- Enable UUID extension for PostgreSQL 10
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 재고 테이블
CREATE TABLE IF NOT EXISTS inventory (
    sku VARCHAR(255) PRIMARY KEY,
    product_name VARCHAR(255),
    qty INTEGER NOT NULL DEFAULT 0,
    reserved_stock INTEGER NOT NULL DEFAULT 0,
    safety_stock INTEGER NOT NULL DEFAULT 0,
    reorder_point INTEGER NOT NULL DEFAULT 10,
    max_stock INTEGER NOT NULL DEFAULT 1000,
    unit_cost DECIMAL(19,2),
    last_restocked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_levels ON inventory(qty, reserved_stock);

-- 재고 예약 테이블
CREATE TABLE IF NOT EXISTS reservations (
    id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    sku VARCHAR(255) NOT NULL REFERENCES inventory(sku),
    qty INTEGER NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_reservations_order_id ON reservations(order_id);
CREATE INDEX IF NOT EXISTS idx_reservations_sku ON reservations(sku);
CREATE INDEX IF NOT EXISTS idx_reservations_expires_at ON reservations(expires_at);

-- 재고 이력 테이블
CREATE TABLE IF NOT EXISTS inventory_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id VARCHAR(255) NOT NULL,
    change_type VARCHAR(50) NOT NULL, -- RESTOCK, RESERVE, RELEASE, CONSUME
    quantity_change INTEGER NOT NULL,
    previous_stock INTEGER NOT NULL,
    new_stock INTEGER NOT NULL,
    reason VARCHAR(255),
    reference_id VARCHAR(255), -- order_id, restock_id 등
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inventory_history_product_id ON inventory_history(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_history_change_type ON inventory_history(change_type);
CREATE INDEX IF NOT EXISTS idx_inventory_history_created_at ON inventory_history(created_at);

-- Outbox Events 테이블
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    event_version INTEGER DEFAULT 1,
    correlation_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);

-- 기본 재고 데이터 (데모용)
INSERT INTO inventory (sku, product_name, qty, reserved_stock, safety_stock, reorder_point, unit_cost) VALUES
('product-001', 'iPhone 15 Pro', 50, 0, 5, 10, 1200000),
('product-002', 'Samsung Galaxy S24', 30, 0, 3, 8, 1100000),
('product-003', 'MacBook Pro 14"', 20, 0, 2, 5, 2500000),
('product-004', 'iPad Air', 40, 0, 4, 10, 800000),
('product-005', 'AirPods Pro', 100, 0, 10, 20, 300000),
('product-006', 'Apple Watch Series 9', 60, 0, 6, 15, 500000),
('product-007', 'Sony WH-1000XM5', 25, 0, 3, 8, 400000),
('product-008', 'Nintendo Switch OLED', 35, 0, 5, 12, 400000),
('product-009', 'Dell XPS 13', 15, 0, 2, 5, 1800000),
('product-010', 'LG OLED TV 55"', 10, 0, 1, 3, 2000000)
ON CONFLICT (sku) DO NOTHING;

-- 재고 이력 초기 데이터
INSERT INTO inventory_history (product_id, change_type, quantity_change, previous_stock, new_stock, reason)
SELECT
    sku as product_id,
    'RESTOCK' as change_type,
    qty as quantity_change,
    0 as previous_stock,
    qty as new_stock,
    'Initial stock setup' as reason
FROM inventory
ON CONFLICT DO NOTHING;
