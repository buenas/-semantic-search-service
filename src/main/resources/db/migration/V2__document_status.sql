ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS embedding_error TEXT,
    ADD COLUMN IF NOT EXISTS embedding_updated_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_documents_status_created
    ON documents (status, created_at DESC);