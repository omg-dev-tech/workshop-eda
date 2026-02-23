-- =================================================================
-- Order Service Database Migration
-- Add status check constraint for PAYMENT_FAILED status
-- =================================================================

-- Drop existing constraint if exists (for idempotency)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

-- Add new constraint with all valid statuses
ALTER TABLE orders ADD CONSTRAINT orders_status_check 
    CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELED', 'INVENTORY_RESERVED', 'INVENTORY_REJECTED', 'PAYMENT_FAILED'));

-- Verify constraint was added
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'orders'::regclass AND conname = 'orders_status_check';

-- Made with Bob
