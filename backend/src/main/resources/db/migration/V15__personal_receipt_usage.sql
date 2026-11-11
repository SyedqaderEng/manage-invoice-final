CREATE TABLE personal_receipt_usage (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    tracked_uploads BIGINT NOT NULL DEFAULT 0 CHECK (tracked_uploads >= 0),
    history_complete BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO personal_receipt_usage (user_id, tracked_uploads, history_complete)
SELECT u.id, COUNT(d.id), FALSE
FROM users u
LEFT JOIN documents d ON d.owner_user_id = u.id
GROUP BY u.id;
