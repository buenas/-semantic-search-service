-- Normalize metadata so it's always an object (or null).
-- If metadata is a JSON string (e.g. "category=billing"), convert it to {"raw": "<string>"}.
UPDATE documents
SET metadata = jsonb_build_object('raw', metadata)
WHERE metadata IS NOT NULL
  AND jsonb_typeof(metadata) = 'string';