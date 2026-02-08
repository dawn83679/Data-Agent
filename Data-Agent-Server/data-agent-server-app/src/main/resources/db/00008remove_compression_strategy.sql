-- ===============================================
-- Remove compression_strategy column from ai_compression_record
-- ===============================================

ALTER TABLE ai_compression_record DROP COLUMN IF EXISTS compression_strategy;
