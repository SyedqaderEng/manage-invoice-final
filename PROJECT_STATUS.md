# Ledgerly Project Status

The implementation follows `mange-invoice-new-plan.txt` phase by phase. This status distinguishes completed work from future functionality.

## Phase 1 — Product UI and Design Foundation

Implemented:

- Product-branded landing page with responsive navigation, receipt-to-record illustration, how-it-works, document types, pricing, FAQ, and final CTA.
- Registration and sign-in screens wired to existing authentication endpoints.
- Personal dashboard shell and profile editing screen.
- Responsive personal navigation, mobile drawer, accessible focus styles, and reduced-motion behavior.
- Empty states that do not show fabricated customer metrics.
- Shared visual design system and basic price-formatting test.

Validation:

- Frontend tests pass.
- TypeScript check and production build pass.
- Landing page opened and visually inspected at desktop size.

## Phase 2 — Customer Accounts and Document Management (In Progress)

Implemented so far:

- Forward-only V10 migration adds owner-account columns and backfills existing documents, vendors, and categories from their workspace owners.
- Account-scoped document upload, paginated history, original preview/download, and deletion APIs.
- Private local file storage with owner-only directory/file permissions; upload size, file-signature, and per-account duplicate checks.
- Account-scoped vendor/category list and create APIs, including same-account validation for vendor default categories.
- Responsive document library upload/list/preview/download/delete UI, plus vendor and category list/create screens.
- Integration coverage for cross-account document access, upload validation, duplicates, and vendor/category isolation.

Validation:

- Backend Maven tests pass (8 tests, including the new document/account-isolation cases).
- Frontend Vitest and production build pass.
- The V10 migration has not yet been run against a PostgreSQL instance.

Still required to complete Phase 2:

- Vendor/category edit and removal/deactivation workflows.
- Execute and verify the forward migration against PostgreSQL.

## Remaining phases

- **Phase 2 remaining:** vendor/category editing and removal/deactivation, plus PostgreSQL migration verification.
- **Phase 3:** OCR/AI extraction, confidence and accuracy handling, review, correction, and confirmation.
- **Phase 4:** real-data expenses, analytics, search, filters, and reports.
- **Phase 5:** CSV/XML/PDF exports and original-document downloads.
- **Phase 6:** Free/Pro account subscriptions, Stripe lifecycle, configurable usage limits, and billing history.
- **Phase 7:** end-to-end, security, responsive, performance, and production readiness.

The existing backend still contains SaaS workspace/member APIs and workspace-based Stripe code. They are not yet adapted to the specification's individual-account model and must not be considered product-complete. Historical Flyway migrations will be preserved and evolved with forward-only migrations.
