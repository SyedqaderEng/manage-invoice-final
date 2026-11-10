# Database

## Runtime

PostgreSQL 16 is the development database. Flyway applies versioned migrations at startup; Hibernate validates the resulting schema.

## Local service

`compose.yml` starts PostgreSQL. Values can be overridden with the same database variables used by the API:

- Database: `saas_starter`
- User: `saas_starter`
- Password: `saas_starter` (local development only)
- Port: `5432`

## SaaS data

- Users own workspace memberships.
- Workspaces isolate team membership and subscription records.
- `workspace_subscriptions` stores the Stripe customer/subscription identifiers and current plan state.
- `processed_stripe_events` makes webhook handling idempotent.
- UUIDs identify users, workspaces, and subscriptions.

Flyway migrations V1-V8 are retained as immutable project history so existing databases remain migratable. V9 normalizes workspace roles/types and adds billing tables. The old application-specific tables are no longer used by the code; cleanup of those historical tables is intentionally not destructive.
