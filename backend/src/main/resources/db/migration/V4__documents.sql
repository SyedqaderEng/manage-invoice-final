CREATE TABLE documents (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size >= 0),
    checksum VARCHAR(64) NOT NULL,
    document_type VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    processing_status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    review_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_documents_workspace_uploaded ON documents(workspace_id, uploaded_at DESC);
CREATE INDEX idx_documents_workspace_checksum ON documents(workspace_id, checksum);
