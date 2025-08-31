-- infra/sql/inventory-init.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS inventory (
  sku varchar(64) PRIMARY KEY,
  qty int NOT NULL
);

INSERT INTO inventory (sku, qty) VALUES
  ('A-001', 1000000000),
  ('B-001',  5000),
  ('C-001', 0)
ON CONFLICT (sku) DO NOTHING;

