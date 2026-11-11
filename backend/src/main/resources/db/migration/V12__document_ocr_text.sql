CREATE TABLE document_ocr_text (
    document_id UUID PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    extracted_text TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
