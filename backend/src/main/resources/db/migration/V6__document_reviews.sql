CREATE TABLE document_reviews (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    reviewer_user_id UUID NOT NULL REFERENCES users(id),
    review_status VARCHAR(30) NOT NULL,
    original_data_json TEXT NOT NULL,
    corrected_data_json TEXT NOT NULL,
    review_notes TEXT,
    reviewed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_reviews_document ON document_reviews(document_id, reviewed_at DESC);
