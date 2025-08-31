-- fulfillment 초기 스키마 (PostgreSQL)
-- 컨테이너가 "처음" 올라올 때 /docker-entrypoint-initdb.d 에서 자동 실행됨

-- 타임스탬프 자동 갱신 트리거
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 출고(fulfillment) 테이블
CREATE TABLE IF NOT EXISTS fulfillments (
  id            BIGSERIAL PRIMARY KEY,
  order_id      varchar(64) NOT NULL UNIQUE,     -- 주문 ID 1:1
  status        varchar(32) NOT NULL DEFAULT 'PENDING',  -- PENDING|SCHEDULED|FAILED
  event_time_ms bigint NOT NULL,                 -- 예약 이벤트 시각(ms)
  shipping_id   varchar(64),                     -- 스케줄 후 부여
  created_at    timestamptz NOT NULL DEFAULT NOW(),
  updated_at    timestamptz NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_fulfillments_updated_at
BEFORE UPDATE ON fulfillments
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_fulfillments_status ON fulfillments(status);

-- ※ 초깃값은 필요 없음. inventory_reserved 이벤트가 들어오면 앱이 insert/update 합니다.
-- -- 예시(테스트 용) — 필요 시 주석 해제
-- INSERT INTO fulfillments(order_id, status, event_time_ms, shipping_id)
-- VALUES ('o-sample-ful-001', 'SCHEDULED', EXTRACT(EPOCH FROM NOW())*1000, 'shp-sample-001')
-- ON CONFLICT(order_id) DO NOTHING;
