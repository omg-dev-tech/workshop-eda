-- ─────────────────────────────────────────────────────────────
-- order DB 초기 스키마 (PostgreSQL)
-- 컨테이너가 처음 올라올 때 1회 실행됨
-- ─────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- gen_random_uuid() 용

-- 상태값: PENDING → (INVENTORY_RESERVED | INVENTORY_REJECTED) → COMPLETED | CANCELED
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
    CREATE TYPE order_status AS ENUM (
      'PENDING',
      'INVENTORY_RESERVED',
      'INVENTORY_REJECTED',
      'COMPLETED',
      'CANCELED'
    );
  END IF;
END$$;

-- 타임스탬프 자동 갱신 트리거
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 주문 테이블
CREATE TABLE IF NOT EXISTS orders (
  id              varchar(64) PRIMARY KEY,              -- 예: o-276e0d...
  customer_id     varchar(64) NOT NULL,
  amount          bigint NOT NULL,
  currency        varchar(8) NOT NULL,                  -- KRW, USD 등
  status          order_status NOT NULL DEFAULT 'PENDING',
  created_at      timestamptz NOT NULL DEFAULT NOW(),
  updated_at      timestamptz NOT NULL DEFAULT NOW(),
  version         bigint NOT NULL DEFAULT 0
);

CREATE TRIGGER trg_orders_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status   ON orders(status);

-- 주문 항목 테이블
CREATE TABLE IF NOT EXISTS order_items (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id    varchar(64) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  sku         varchar(64) NOT NULL,
  qty         int NOT NULL CHECK (qty > 0)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order  ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_sku    ON order_items(sku);

-- (옵션) 샘플 주문을 미리 넣고 싶다면 주석 해제
-- INSERT INTO orders(id, customer_id, amount, currency, status)
-- VALUES ('o-sample-001', 'c-77', 56000, 'KRW', 'PENDING');
-- INSERT INTO order_items(order_id, sku, qty) VALUES
-- ('o-sample-001', 'A-001', 2);
