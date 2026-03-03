CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now());

CREATE INDEX IF NOT EXISTS idx_documents_created_at
    ON documents (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_documents_metadata_gin
    ON documents USING gin (metadata);

CREATE INDEX IF NOT EXISTS documents_embedding_ivfflat_idx
    ON documents USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

--update at trigger
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_documents_updated_at ON documents;
CREATE TRIGGER trg_documents_updated_at
BEFORE UPDATE ON documents
FOR EACH ROW EXECUTE FUNCTION set_updated_at();