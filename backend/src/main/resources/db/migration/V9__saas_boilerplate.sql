UPDATE workspace_members SET role = 'MEMBER' WHERE role IN ('REVIEWER', 'VIEWER');
UPDATE workspaces SET workspace_type = 'ORGANIZATION' WHERE workspace_type IN ('BUSINESS', 'ACCOUNTANT');

CREATE TABLE workspace_subscriptions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL UNIQUE REFERENCES workspaces(id) ON DELETE CASCADE,
    stripe_customer_id VARCHAR(255) UNIQUE,
    stripe_subscription_id VARCHAR(255) UNIQUE,
    plan_id VARCHAR(50) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_period_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE processed_stripe_events (
    stripe_event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
