CREATE TABLE "manage-receipt".ledgerly_documents (
    id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL REFERENCES "manage-receipt".users (id),
    uploaded_by uuid NOT NULL REFERENCES "manage-receipt".users (id),
    original_filename varchar(255) NOT NULL,
    storage_key varchar(500) NOT NULL UNIQUE,
    mime_type varchar(100) NOT NULL,
    file_size bigint NOT NULL,
    checksum varchar(64) NOT NULL,
    document_type varchar(30) NOT NULL DEFAULT 'UNKNOWN',
    processing_status varchar(30) NOT NULL DEFAULT 'UPLOADED',
    review_status varchar(30) NOT NULL DEFAULT 'NOT_REQUIRED',
    uploaded_at timestamp with time zone NOT NULL DEFAULT now(),
    processed_at timestamp with time zone,
    archived_at timestamp with time zone,
    deleted_at timestamp with time zone
);

CREATE INDEX ledgerly_documents_owner_uploaded_idx
    ON "manage-receipt".ledgerly_documents (owner_user_id, uploaded_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ledgerly_documents_owner_checksum_idx
    ON "manage-receipt".ledgerly_documents (owner_user_id, checksum)
    WHERE deleted_at IS NULL;

CREATE TABLE "manage-receipt".ledgerly_categories (
    id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL REFERENCES "manage-receipt".users (id),
    name varchar(100) NOT NULL,
    description text,
    color varchar(7),
    category_type varchar(30) NOT NULL DEFAULT 'EXPENSE',
    is_system boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ledgerly_categories_owner_name_idx
    ON "manage-receipt".ledgerly_categories (owner_user_id, lower(name));

CREATE TABLE "manage-receipt".ledgerly_vendors (
    id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL REFERENCES "manage-receipt".users (id),
    normalized_name varchar(255) NOT NULL,
    display_name varchar(255) NOT NULL,
    email varchar(320),
    phone varchar(50),
    address text,
    tax_identifier_encrypted text,
    default_category_id uuid REFERENCES "manage-receipt".ledgerly_categories (id),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ledgerly_vendors_owner_normalized_name_idx
    ON "manage-receipt".ledgerly_vendors (owner_user_id, normalized_name);

CREATE TABLE "manage-receipt".ledgerly_personal_receipt_usage (
    user_id uuid PRIMARY KEY REFERENCES "manage-receipt".users (id),
    tracked_uploads bigint NOT NULL DEFAULT 0,
    history_complete boolean NOT NULL DEFAULT true
);

CREATE TABLE "manage-receipt".ledgerly_digital_receipt_records (
    document_id uuid PRIMARY KEY REFERENCES "manage-receipt".ledgerly_documents (id) ON DELETE CASCADE,
    status varchar(16) NOT NULL,
    data_json text NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE "manage-receipt".ledgerly_document_ocr_text (
    document_id uuid PRIMARY KEY REFERENCES "manage-receipt".ledgerly_documents (id) ON DELETE CASCADE,
    extracted_text text NOT NULL,
    processed_at timestamp with time zone NOT NULL DEFAULT now()
);

ALTER TABLE "manage-receipt".ledgerly_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE "manage-receipt".ledgerly_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE "manage-receipt".ledgerly_vendors ENABLE ROW LEVEL SECURITY;
ALTER TABLE "manage-receipt".ledgerly_personal_receipt_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE "manage-receipt".ledgerly_digital_receipt_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE "manage-receipt".ledgerly_document_ocr_text ENABLE ROW LEVEL SECURITY;
