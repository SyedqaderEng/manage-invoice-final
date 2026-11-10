# Changelog

## SaaS Starter

- Converted the application identity and user interface to a reusable SaaS starter.
- Kept authentication, workspace isolation, and workspace administration; generalized workspace roles and types.
- Added public plan display, Stripe Checkout and portal session creation, and signature-verified idempotent webhooks.
- Removed invoice, expense, document/OCR, review, vendor, and category application code.
- Preserved historical Flyway migrations for existing database compatibility.
