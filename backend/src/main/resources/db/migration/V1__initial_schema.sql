CREATE TABLE application_metadata (
    key_name VARCHAR(100) PRIMARY KEY,
    key_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO application_metadata (key_name, key_value)
VALUES ('schema_version', '1');
