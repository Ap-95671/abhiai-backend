ALTER TABLE media_assets ADD COLUMN optimized_storage_key VARCHAR(120);
ALTER TABLE media_assets ADD COLUMN thumbnail_storage_key VARCHAR(120);
ALTER TABLE media_assets ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED';
ALTER TABLE media_assets ADD COLUMN processed_at TIMESTAMPTZ;
UPDATE media_assets SET processing_status = 'PENDING' WHERE content_type IN ('image/jpeg', 'image/png');
