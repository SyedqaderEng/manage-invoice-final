CREATE TABLE processing_jobs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    job_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(100),
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processing_jobs_document ON processing_jobs(document_id, created_at DESC);
CREATE INDEX idx_processing_jobs_status ON processing_jobs(status, created_at);

CREATE TABLE document_extractions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    extraction_version INTEGER NOT NULL,
    raw_extraction_json TEXT NOT NULL,
    normalized_data_json TEXT NOT NULL,
    overall_confidence NUMERIC(5,4),
    model_provider VARCHAR(100),
    model_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, extraction_version)
);

CREATE TABLE extraction_fields (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    extraction_id UUID NOT NULL REFERENCES document_extractions(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    extracted_value TEXT,
    normalized_value TEXT,
    confidence_score NUMERIC(5,4),
    source_page INTEGER,
    source_region VARCHAR(255),
    validation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    correction_status VARCHAR(30) NOT NULL DEFAULT 'UNMODIFIED'
);
