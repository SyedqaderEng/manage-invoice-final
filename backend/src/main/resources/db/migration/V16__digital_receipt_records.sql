CREATE TABLE digital_receipt_records (
    document_id UUID PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED')),
    data_json TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
