# Database And RLS

## Current Target

- Supabase PostgreSQL database: `postgres`.
- Application schema: `manage-receipt`. The hyphen requires a quoted SQL identifier.
- Supabase Auth manages passwords, identities and sessions. `users.id` references `auth.users.id`.
- The backend uses the existing `users` and `revoked_tokens` tables. Its document, category, vendor, receipt, OCR and usage tables are isolated with a `ledgerly_` prefix so they do not alter the existing application tables.

## Tables

- Identity: `users`, `revoked_tokens` (SHA-256 token hashes and expiration only).
- Documents and processing: `documents`, `processing_jobs`, `extraction_versions`.
- Classification: `categories`, `vendors`, `vendor_aliases`.
- Receipt app additions: `ledgerly_documents`, `ledgerly_categories`, `ledgerly_vendors`, `ledgerly_personal_receipt_usage`, `ledgerly_digital_receipt_records`, `ledgerly_document_ocr_text`.
- Digital records: `transactions`, `line_items`, `transaction_charges`, `category_allocations`.
- Product comparison: `products`, `product_matches`.
- Plans and billing: `plans`, `plan_prices`, `subscriptions`, `stripe_events`, `usage_periods`, `usage_reservations`.
- Reports and operations: `export_jobs`, `saved_reports`, `notifications`, `audit_events`.

Customer-owned child records use ownership-aware foreign keys where they reference documents, transactions, categories, vendors or products. Transactions are linked to a single document and monetary amounts use PostgreSQL numeric types. A document has at most one transaction; its draft or confirmed status must be enforced by the application.

## Access

All 24 existing application tables and the six additive `ledgerly_` tables have RLS enabled. No direct-client policies are configured; at the time of the access audit, `anon`, `authenticated`, and `service_role` had no schema usage or table privileges on this schema. The backend database role is `postgres`, which has table privileges and bypasses RLS. Enabling RLS does not constrain that role.

The authentication-only development backend currently uses the configured administrative identity; it is not production-ready. Spring Boot needs a separate least-privileged database identity and a verified request-to-user authorization design before accessing customer data. Database policies and privileges must be reviewed together with that design; do not grant clients direct access simply to make the application work. The backend must verify the user's identity and enforce ownership even where RLS provides additional protection.

## Processing And Privacy

Original documents are stored in the private Amazon S3 bucket; the database stores object keys, not public URLs. The backend scopes document reads and deletes to the authenticated owner. `extraction_versions.sanitized_result` must receive sanitized content only: its JSON type alone does not prevent card details or other sensitive values from being stored. OCR filtering, retention, access auditing and safe logging still need implementation and verification.

## Plans And Limits

The Free and Pro catalog entries and three USD Stripe Price IDs are configured as inactive test-mode records. Free has a lifetime limit of 15 processed receipts; Pro has a monthly limit of 60. Storage limits are unset. Checkout, webhook verification, entitlement enforcement, atomic quota reservations and live-mode prices are not configured. The monthly reset boundary and treatment of retries, technical failures and plan changes still need an explicit policy. Do not treat inactive records or unset limits as permission to grant unlimited access.

## Recovery And Changes

The backend's additive application tables are managed by versioned Flyway migrations in `backend/src/main/resources/db/migration`. Flyway baselines the existing non-empty schema at version 0 before applying these migrations. Existing tables are not modified by the app migration. A verified backup and restore procedure is still required before relying on this database.