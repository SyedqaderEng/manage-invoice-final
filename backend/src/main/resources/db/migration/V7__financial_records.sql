CREATE TABLE vendors (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    normalized_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(320), phone VARCHAR(50), address TEXT, tax_identifier_encrypted TEXT,
    default_category_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(workspace_id, normalized_name)
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT, color VARCHAR(7), category_type VARCHAR(30) NOT NULL DEFAULT 'EXPENSE',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(workspace_id, name)
);

ALTER TABLE vendors ADD CONSTRAINT fk_vendor_default_category FOREIGN KEY (default_category_id) REFERENCES categories(id);

CREATE TABLE expense_transactions (
    id UUID PRIMARY KEY, workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id UUID REFERENCES documents(id), vendor_id UUID REFERENCES vendors(id), category_id UUID REFERENCES categories(id),
    description TEXT, transaction_date DATE NOT NULL, amount NUMERIC(19,4) NOT NULL CHECK (amount >= 0),
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0, currency CHAR(3) NOT NULL, payment_status VARCHAR(30) NOT NULL,
    expense_status VARCHAR(30) NOT NULL, source_type VARCHAR(30) NOT NULL, created_by UUID NOT NULL REFERENCES users(id), approved_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expense_line_items (
    id UUID PRIMARY KEY, workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    expense_id UUID NOT NULL REFERENCES expense_transactions(id) ON DELETE CASCADE,
    description TEXT NOT NULL, quantity NUMERIC(19,4) NOT NULL, unit_price NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0, line_total NUMERIC(19,4) NOT NULL, category_id UUID REFERENCES categories(id)
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY, workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id UUID REFERENCES documents(id), vendor_id UUID REFERENCES vendors(id), invoice_number VARCHAR(100) NOT NULL,
    invoice_date DATE, due_date DATE, subtotal NUMERIC(19,4) NOT NULL, tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL, amount_paid NUMERIC(19,4) NOT NULL DEFAULT 0,
    balance_due NUMERIC(19,4) NOT NULL, currency CHAR(3) NOT NULL, status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(workspace_id, vendor_id, invoice_number)
);

CREATE TABLE invoice_line_items (
    id UUID PRIMARY KEY, workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description TEXT NOT NULL, quantity NUMERIC(19,4) NOT NULL, unit_price NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0, line_total NUMERIC(19,4) NOT NULL
);

CREATE TABLE payments (
    id UUID PRIMARY KEY, workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE, payment_date DATE NOT NULL,
    amount NUMERIC(19,4) NOT NULL CHECK (amount >= 0), currency CHAR(3) NOT NULL,
    payment_method VARCHAR(50), reference_number VARCHAR(100), status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_relationships (
    id UUID PRIMARY KEY, workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    source_document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    related_document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    relationship_type VARCHAR(30) NOT NULL, confidence_score NUMERIC(5,4) NOT NULL,
    confirmation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_expenses_workspace_date ON expense_transactions(workspace_id, transaction_date DESC);
CREATE INDEX idx_invoices_workspace_due ON invoices(workspace_id, due_date);
CREATE INDEX idx_payments_invoice ON payments(invoice_id, payment_date);
