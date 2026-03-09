-- Migration: Add event_count_summary table for performance optimization
-- Date: 2026-03-09
-- Description: Pre-aggregated event counts to improve query performance

-- Create event_count_summary table
CREATE TABLE IF NOT EXISTS event_count_summary (
    event_type VARCHAR(100) PRIMARY KEY,
    count BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_event_count_summary_last_updated 
ON event_count_summary(last_updated);

-- Initialize with existing data from event_log
INSERT INTO event_count_summary (event_type, count, last_updated)
SELECT 
    event_type,
    COUNT(*) as count,
    CURRENT_TIMESTAMP as last_updated
FROM event_log
GROUP BY event_type
ON CONFLICT (event_type) DO UPDATE
SET 
    count = EXCLUDED.count,
    last_updated = EXCLUDED.last_updated;

-- Add a special row for total count
INSERT INTO event_count_summary (event_type, count, last_updated)
SELECT 
    '_TOTAL_' as event_type,
    COUNT(*) as count,
    CURRENT_TIMESTAMP as last_updated
FROM event_log
ON CONFLICT (event_type) DO UPDATE
SET 
    count = EXCLUDED.count,
    last_updated = EXCLUDED.last_updated;

-- Verify the migration
SELECT 'Migration completed successfully. Total rows: ' || COUNT(*) as status
FROM event_count_summary;

-- Made with Bob
