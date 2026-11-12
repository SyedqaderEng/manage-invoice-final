# Invoice & Expense Management SaaS

## Master Product, Feature, Workflow, Data Analytics & Go-Live Implementation Specification

**Purpose:** This is the implementation contract for Codespaces. Build the
existing product described in `manage-invoice-plan-final-customer.md`,
extending and clarifying it with explicit features, screen inventories,
button behavior, workflows, data requirements, analytics, integrations,
testing, and a realistic launch sequence.

**Product model:** Individual/personal accounts only. This is not a
multi-tenant business workspace product. Each person's records are
private and isolated.

**Commercial model:** Free plan and Pro plan (\$10/month; optional
\$99/year if configured in Stripe). Keep prices, limits, entitlements,
and Stripe Price IDs in configuration/database rather than scattering
hardcoded values through the code.

**Delivery goal:** Go live this week. Prioritize a reliable, narrow
production release over unfinished breadth. Do not pretend a feature is
complete if its external credentials, integration, tests, or operational
setup are missing.

------------------------------------------------------------------------

# 0. Non-negotiable instructions to Codespaces

1.  Read the existing project and the uploaded source product
    specification before changing code.
2.  Preserve the existing stack, working features, visual design
    direction, and data unless a documented defect requires a change.
3.  Do not rebuild the application from scratch merely because the
    existing implementation is incomplete.
4.  Inventory every existing route, screen, component, visible control,
    API, database table, integration, and background job.
5.  Map every visible button, link, menu item, tab, filter, form, row
    action, modal action, and keyboard action to a real behavior.
6.  No dead buttons, decorative controls that look interactive, fake
    success messages, fake analytics, mock production data, or
    placeholder APIs.
7.  Every action must have loading, success, validation, error, empty,
    and retry behavior as appropriate.
8.  Validate on the server as well as in the browser. Never trust
    client-supplied user IDs, totals, plan entitlements, payment status,
    or file paths.
9.  Use real customer-owned data. Demo data is allowed only in a clearly
    isolated demo/development environment.
10. Build in the order in this document. Finish and test each phase
    before starting the next.
11. Maintain the implementation documents listed in Section 20.
12. A feature is not done because code exists. It is done only when the
    user journey works and its tests pass.
13. Do not silently delete customer data, original files, or historical
    records when a subscription changes.
14. If a requirement is blocked by missing credentials or a decision,
    document the exact blocker and continue with independent work.
15. Keep a production release checklist and do not deploy a known
    critical security, data-loss, payment, or document-access defect.

## Definition of "working"

For each feature, Codespaces must verify: - The user can reach it through the
intended navigation. - The control is enabled/disabled correctly. - The
input is validated. - The backend performs the real operation. - The
correct user's data is used. - The database/storage state is correct. -
The user sees an accurate result. - Failures are understandable and
recoverable. - Refreshing or signing in again shows persisted state. -
Another account cannot access the data. - Automated tests cover the
important positive and negative paths.

------------------------------------------------------------------------

# 1. Product scope and release boundaries

## 1.1 Core product promise

A customer can capture or upload a receipt, invoice, bill, or expense
document; the application extracts structured information; the customer
reviews uncertain details; the original document and digital record are
linked; and the application turns accumulated records into useful
spending insights and exports.

Primary journey:

**Visit → Register → Dashboard → Upload → Process → Review → Confirm →
Save → Analyze → Search/Export → Manage subscription.**

## 1.2 Supported document types

-   Retail receipts.
-   Online order invoices and receipts.
-   General invoices.
-   Contractor/service invoices.
-   Hospital and medical bills.
-   Utility bills.
-   Other expense-related PDF or image documents.

Do not claim that every document type has specialized extraction
accuracy. Use a common extraction pipeline, identify document type, and
make uncertain fields reviewable.

## 1.3 Release priorities

### P0 --- Must work before public launch

-   Landing page and pricing.
-   Registration, login, logout, email verification policy, forgot/reset
    password.
-   Account isolation and authorization.
-   Free-plan onboarding.
-   Upload image/PDF from device and mobile camera where supported.
-   Private original-file storage.
-   OCR/extraction and processing status.
-   Review/edit/confirm extracted record.
-   Document list, detail, original preview/download, search and basic
    filters.
-   Vendor and category assignment.
-   Dashboard with accurate totals and basic charts.
-   Core monthly analytics and vendor/category breakdown.
-   CSV export of confirmed records.
-   Free/Pro entitlement enforcement.
-   Stripe checkout and webhook synchronization if Pro is offered at
    launch.
-   Error handling, backups, monitoring, production configuration,
    privacy/legal pages.
-   End-to-end smoke tests.

### P1 --- Build after P0 is stable; launch only if verified

-   Advanced product-level analytics.
-   Product normalization and matching across stores.
-   Store-to-store price comparison.
-   PDF reports.
-   XML export.
-   Bulk ZIP downloads.
-   Advanced custom report builder.
-   AI natural-language analytics assistant.
-   More sophisticated duplicate detection.
-   Annual billing, if not ready for launch.

### P2 --- Later roadmap; do not block this week's launch

-   Automatic email receipt ingestion.
-   Bank/card transaction imports and reconciliation.
-   Retailer account integrations.
-   Mobile native applications.
-   Shared household accounts.
-   Tax preparation integrations.
-   Budget planning and alerts.
-   Subscription detection for recurring purchases.
-   Predictive forecasting and recommendations.

Codespaces must inspect the current application and adjust these release
labels only with documented reasons. Do not silently expand launch
scope.

------------------------------------------------------------------------

# 2. Recommended architecture and services

Use the existing project stack wherever it is already sound. The
following is the recommended target architecture, not permission to
replace working infrastructure without approval.

## 2.1 Core services

  -----------------------------------------------------------------------
  Concern                 Recommended service     Responsibility
  ----------------------- ----------------------- -----------------------
  Authentication          Supabase Auth           Registration, login,
                                                  password recovery,
                                                  email verification,
                                                  sessions

  Relational database     Supabase Postgres       Users, documents,
                                                  extracted records, line
                                                  items, vendors,
                                                  categories,
                                                  subscriptions, usage,
                                                  analytics source data

  Authorization           Supabase RLS + backend  Enforce per-user
                          authorization           isolation on every
                                                  private record

  Original document       Amazon S3 private       Original images/PDFs
  storage                 bucket                  and generated exports

  Temporary               Short-lived S3          Secure upload and
  upload/download access  presigned URLs, issued  download without public
                          only after server       buckets
                          authorization           

  OCR/document extraction Amazon Textract         Receipt/invoice text,
                          AnalyzeExpense          fields, line items,
                                                  confidence and geometry
                                                  where supported

  AI enrichment           OpenAI API, server-side Normalize descriptions,
                          only                    classify uncertain
                                                  items, summarize
                                                  trends, answer
                                                  analytics questions

  Payments                Stripe Billing +        Pro monthly/annual
                          Checkout + Customer     billing, payment
                          Portal                  methods, invoices,
                                                  cancellation

  Transactional email     Resend (or existing     Verification, password
                          provider)               reset if delegated,
                                                  processing/payment
                                                  notices

  Charts                  Existing chart library; Responsive data
                          Recharts if React and   visualizations
                          no suitable library     
                          exists                  

  PDF report generation   Existing PDF library or Printable reports and
                          server-side PDF         downloadable summaries
                          renderer                

  Background processing   Existing worker/queue;  OCR, AI enrichment,
                          otherwise a small       exports, retry and
                          server-side worker with failure handling
                          a durable queue         

  Error monitoring        Sentry                  Frontend/backend
                                                  exceptions and release
                                                  health

  Product analytics       PostHog or              Product usage events;
                          privacy-appropriate     never send receipt
                          equivalent, optional    contents or sensitive
                                                  financial details

  Hosting                 Existing hosting        Frontend and backend
                          platform                deployment

  DNS/TLS                 Existing domain         HTTPS, domain and
                          provider/hosting        redirects
  -----------------------------------------------------------------------

## 2.2 Important architecture decision: Supabase plus S3

-   Supabase Auth and Postgres are the source of truth for identity and
    application records.
-   S3 stores original binary files and generated exports. Use a private
    bucket; do not enable public read access.
-   Store only an S3 object key and safe metadata in Postgres, never a
    permanent public object URL.
-   Use short-lived presigned URLs after checking the authenticated user
    owns the document.
-   Use opaque, random object keys such as
    `users/{internal-user-id}/{document-id}/original.ext`; do not put
    email addresses or receipt details in object keys.
-   S3 bucket versioning and lifecycle policies should be considered. Do
    not auto-delete original documents unexpectedly.
-   If the existing application already uses Supabase Storage reliably,
    do not migrate to S3 during the launch week unless there is a
    compelling requirement. Choose one authoritative object store for
    production; do not duplicate storage without a clear reason.
-   Keep database metadata and object storage consistent. If one
    operation succeeds and the other fails, use a recoverable upload
    state and cleanup/reconciliation job.

## 2.3 OCR and AI responsibilities

**OCR is not the same as AI interpretation.**

1.  OCR/document service reads the source document and returns
    text/structured candidates, line items, confidence and page
    references.
2.  Application validation checks types, amounts, totals, dates and
    required fields.
3.  AI enrichment may normalize abbreviated product descriptions,
    suggest categories, detect likely vendor identity, and summarize
    confirmed spending.
4.  The customer reviews uncertain or low-confidence fields.
5.  Only confirmed records are included in authoritative spending
    analytics by default.

Use Textract AnalyzeExpense for receipt/invoice extraction if the AWS
account and region support the required feature. If the current project
uses another capable OCR provider, retain it after evaluating output
quality, cost, latency, data location and credentials. Do not integrate
multiple OCR providers in the first release without a measured need.

OpenAI must not invent missing products, prices, dates, taxes, vendors
or totals. Return structured suggestions with confidence/reason and
preserve raw OCR values. Financial arithmetic and aggregation must be
deterministic application/database code, not an LLM.

## 2.4 Asynchronous processing

OCR and AI work may take longer than a normal web request. Use a durable
background job mechanism.

Document state machine:

`created → uploading → uploaded → queued → processing → needs_review → confirmed`

Alternative terminal/error states: - `upload_failed` -
`processing_failed` - `rejected` - `deleted` (soft deletion or deletion
workflow according to retention policy)

Requirements: - Every job has an idempotency key. - Retried jobs must
not create duplicate invoices or line items. - Store provider job IDs
and safe error codes. - Retry transient errors with bounded backoff. -
Do not retry invalid/unsupported documents indefinitely. - A user can
see status and retry eligible failures. - The UI polls or subscribes for
status updates without excessive requests. - A job cannot access another
user's document. - Record processing attempts, timestamps and provider
usage.

------------------------------------------------------------------------

# 3. Information architecture and navigation

## 3.1 Public navigation

-   Logo → landing page.
-   Features → feature section/page.
-   How It Works → how-it-works section/page.
-   Pricing → pricing section/page.
-   FAQ → FAQ section/page.
-   Sign In → login.
-   Start for Free → registration.
-   Footer links → Privacy, Terms, Contact, Support.

Every link must have a real destination. Hide links for pages that are
not ready rather than linking to empty pages.

## 3.2 Authenticated navigation

Recommended primary navigation: 1. Overview 2. Documents 3. Upload 4.
Vendors 5. Categories 6. Analytics 7. Reports 8. Settings

Account menu: - My Profile - Subscription & Billing - Help/Support -
Sign Out

Do not create organization/workspace switching. This is an individual
account product.

------------------------------------------------------------------------

# 4. Feature-by-feature screen and control inventory

Codespaces must compare this inventory with the existing UI. Add any
additional visible control found in the codebase. No visible interactive
control may be left unmapped.

## 4.1 Landing page

### Sections

-   Header/navigation.
-   Hero and product preview.
-   How it works.
-   Supported document types.
-   OCR/AI explanation.
-   Original-versus-digital record demonstration.
-   Analytics preview.
-   Feature grid.
-   Pricing.
-   FAQ.
-   Final CTA.
-   Footer.

### Controls and behavior

-   **Logo:** navigate to landing page.
-   **Features link:** scroll/navigate to features.
-   **How It Works link:** scroll/navigate to explanation.
-   **Pricing link:** open pricing section.
-   **FAQ link:** open FAQ section.
-   **Sign In button:** open login.
-   **Start for Free button:** open registration; preserve any
    campaign/referral context if supported.
-   **Explore Features button:** navigate to feature section.
-   **Plan CTA:** capture selected plan and billing interval, then begin
    authentication/checkout journey.
-   **FAQ expand/collapse:** reveal answer and update accessible
    expanded state.
-   **Footer links:** open real legal/contact pages.
-   **Mobile menu button:** open/close navigation; close after
    navigation and support keyboard access.

No CTA may lead to a dead page. Public marketing claims must match
actual released functionality.

## 4.2 Registration

Fields: - Name/display name if currently part of the product. - Email. -
Password. - Confirm password if the current UI uses it. - Terms/privacy
acceptance if required.

Controls: - **Create Account:** validate and call Supabase
Auth/backend. - **Show/Hide Password:** toggle password visibility. -
**Sign In:** navigate to login and preserve safe return destination. -
**Terms link / Privacy link:** open legal pages. - **Resend
verification:** send a new verification message subject to rate limit. -
**Continue:** after required verification, resume original intended
destination.

Workflow: 1. Validate required fields and email format. 2. Normalize
email consistently. 3. Check duplicate-account outcome safely; do not
leak account existence through an insecure enumeration endpoint.
Provider response and UX should be designed to avoid exposing sensitive
account state unnecessarily. 4. Create account through Supabase Auth. 5.
Create profile using a trusted server/database trigger or safe backend
operation. 6. Display truthful success/pending-verification message. 7.
Send verification email according to configured Supabase Auth flow. 8.
Preserve selected plan/checkout intent using a signed or server-stored
state, not arbitrary client-supplied price. 9. If email verification is
required, guide user to verify and then continue. 10. If no verification
is required, route to intended destination. 11. Do not route a user who
selected a paid plan directly to the dashboard while checkout is still
pending.

Error cases: - Invalid email. - Weak password. - Password mismatch. -
Existing account. - Rate limit. - Email provider failure. - Network
failure. - Terms not accepted. - Expired or invalid verification link.

## 4.3 Login

Fields: - Email. - Password.

Controls: - **Sign In:** authenticate and create session. - **Show/Hide
Password:** toggle visibility. - **Forgot Password:** navigate to
recovery. - **Create Account:** navigate to registration. - **Google
Sign In:** only if configured and fully tested. - **Return
destination:** after login route to original protected page/checkout
safely.

Test invalid credentials, unverified accounts, disabled accounts, rate
limits, session persistence, logout and expired session.

## 4.4 Forgot password and reset password

Controls: - **Send Reset Link:** request reset email. - **Back to Sign
In:** return to login. - **Show/Hide Password:** toggle. - **Reset
Password:** validate token and new password. - **Resend Link:** request
another link with rate limiting.

Workflow: - Always show a privacy-safe request confirmation. - Use
provider-managed secure recovery tokens. - Handle expired/used/invalid
tokens. - Do not log tokens. - Invalidate/revoke sessions according to
security policy after password reset. - Confirm successful password
change and route to login.

## 4.5 Dashboard / Overview

### Header controls

-   **Upload Receipt:** open upload flow with document type defaulted to
    receipt.
-   **Upload Invoice:** open upload flow with invoice type defaulted.
-   **View All Documents:** navigate to Documents.
-   **View Reports:** navigate to Reports.
-   **View Analytics:** navigate to Analytics.
-   **Search:** open global search or search page.
-   **Date range selector:** recalculate all dashboard metrics for
    selected period.
-   **Notification icon:** open real notifications, if notifications are
    implemented.
-   **Profile menu:** open account menu.

### KPI cards

-   Total spending.
-   Spending this month.
-   Previous month spending.
-   Change versus previous period.
-   Number of documents.
-   Number of confirmed transactions.
-   Average transaction value.
-   Processing items needing review.

Each card: - Uses real data. - Has a clear date/period basis. - Has a
tooltip/description for calculation. - Links to a relevant filtered view
where useful. - Displays an empty state for new accounts. - Never
displays fabricated sample values in production.

### Dashboard panels

-   Spending over time.
-   Spending by vendor/store.
-   Spending by category.
-   Recent transactions.
-   Recent uploads and processing status.
-   Top products, if line-item data quality is sufficient.

Controls: - **Chart period selector:** changes chart period and query. -
**Chart legend item:** toggles a series only if chart semantics support
it. - **View details:** opens the corresponding filtered analytics/list
view. - **Transaction row:** opens document/transaction detail. -
**Recent upload row:** opens processing or review page. - **Retry
processing:** retries eligible failed job. - **Empty-state Upload CTA:**
opens upload.

## 4.6 Upload center

Upload methods: - Drag/drop. - File picker. - Mobile camera capture
where supported. - Multiple file selection if the existing design and
limits support it.

Controls: - **Choose Files:** open native file picker. - **Take Photo:**
open device camera capture on supported mobile devices. - **Drag and
Drop:** accept supported files and show invalid-file feedback. -
**Remove File:** remove a not-yet-submitted item from the upload
queue. - **Upload/Start Processing:** validate and initiate secure
upload. - **Cancel:** cancel a pending upload where safe; explain if
processing already started. - **Retry Upload:** retry transient upload
failures. - **View Document:** open the uploaded document. - **Go to
Documents:** navigate to document list.

Validation: - Allowed MIME types and actual file signature. - File size
limit from configuration. - Page count limit for PDF, if configured. -
Empty file. - Corrupt/unreadable file. - Duplicate file/document
warning. - Storage quota and plan quota. - Malware scanning or
provider-supported file safety checks where practical. - Safe filenames;
never trust path supplied by browser.

Workflow: 1. Select file. 2. Validate before upload. 3. Request
authorized short-lived upload URL or use server-mediated upload. 4.
Upload to private S3 object key. 5. Confirm upload completion with
backend. 6. Create/advance document record. 7. Enqueue processing job.
8. Display processing status. 9. Navigate to review when ready. 10. If
upload fails, allow retry without creating duplicate records.

Do not consume a successful-processing allowance for an upload that
failed before processing. Define and document exactly when quota is
consumed. Use a database transaction/atomic usage reservation to prevent
concurrent uploads exceeding limits.

## 4.7 Processing queue and status page

Display: - Filename. - Document type candidate. - Upload date. - Current
status. - Progress stage. - Retry eligibility. - Failure message in
customer-friendly language.

Controls: - **Open:** open document detail. - **Retry:** enqueue
eligible failed processing. - **Cancel:** cancel only when safe and
supported. - **Delete:** initiate documented deletion flow. - **Refresh
status:** retrieve latest status. - **Review extracted data:** open
review screen when ready.

## 4.8 OCR extraction and AI enrichment

Extraction fields: - Vendor/store/provider name. - Vendor
address/contact where available. - Invoice/receipt number. - Document
type. - Transaction/issue date. - Transaction time. - Currency. -
Line-item raw description. - Normalized/human-readable description. -
SKU/UPC/product code when actually present. - Quantity and unit. - Unit
price. - Line discount. - Line tax if identifiable. - Line total. -
Subtotal. - Tax total. - Document discount. - Tip/fee/shipping where
applicable. - Grand total. - Payment method/type only when safely
available. - Confidence per field and per line item. - Source
page/region reference where provider supports it.

Controls: - **Reprocess:** run extraction again only with an explicit
action and idempotent job. - **Retry AI enrichment:** retry enrichment
without overwriting raw OCR. - **Show Original Text:** show raw
extracted text for troubleshooting/review. - **Mark Unknown:** retain
raw value and mark unresolved. - **Accept Suggestion:** accept a
proposed normalized value. - **Reject Suggestion:** retain raw
description. - **Edit Field:** manually correct a field. - **Save
Draft:** save review progress without confirming. - **Continue to
Review:** open review UI.

Rules: - Preserve immutable raw extraction output or version it. - Store
AI suggestions separately from confirmed values. - Never let an LLM
alter amounts silently. - Never fabricate an item identity based only on
an ambiguous abbreviated code. - Mark uncertain values visibly. -
Validate arithmetic with deterministic code and flag discrepancies
rather than silently changing source totals. - Preserve currency and
locale. - Do not assume all documents are retail receipts.

## 4.9 Invoice/receipt review

Layout: - Original document preview. - Extracted digital record. -
Vendor/date/number/total summary. - Editable line-item table. -
Tax/discount/subtotal/total breakdown. - Confidence/needs-review
indicators.

Controls: - **Edit Vendor:** edit vendor fields. - **Edit Date:**
correct date. - **Edit Document Number:** correct invoice/receipt
identifier. - **Add Line Item:** add manually identified item. - **Edit
Line Item:** edit description, quantity, unit price, discount, tax and
total. - **Delete Line Item:** remove erroneous line after
confirmation. - **Add Tax/Fee:** add a clearly typed charge. - **Edit
Category:** assign category. - **Split Category:** allocate a
transaction/line across categories where supported. - **Save Draft:**
persist corrections without final confirmation. - **Confirm & Save:**
validate and commit the record. - **Cancel Review:** return without
losing saved draft. - **View Original:** open original image/PDF. -
**Download Original:** authorize and download original. - **Report
Extraction Issue:** record issue for diagnostics/support.

On confirm: 1. Revalidate all fields server-side. 2. Calculate totals
deterministically. 3. Flag mismatch between calculated and source
totals. 4. Save confirmed digital record and line items transactionally.
5. Preserve original file link. 6. Update vendor/category links. 7.
Update usage/processing status. 8. Refresh dashboard/analytics queries.
9. Show success only after database commit. 10. Navigate to detail or
documents list.

## 4.10 Documents list

Columns: - Document/vendor. - Document type. - Transaction date. -
Total. - Category. - Processing/confirmation status. - Uploaded date. -
Actions.

Controls: - **Search:** search vendor, description, invoice number and
relevant indexed fields. - **Date filter:** filter by transaction
date. - **Vendor filter:** filter by vendor. - **Category filter:**
filter by category. - **Document type filter:**
receipt/invoice/bill/other. - **Status filter:** processing, needs
review, confirmed, failed. - **Minimum/maximum amount:** filter by
amount and currency. - **Sort:** date, amount, vendor, upload date. -
**Clear filters:** reset filters. - **Select row:** open detail. -
**Select multiple:** enable bulk actions only when supported. -
**Download:** download original or export record. - **Delete:**
soft-delete/retention workflow with confirmation. - **Pagination / Load
More:** fetch next page without loading all records. - **Upload:** open
upload center.

Search and filters must compose correctly. Show active filter chips and
result count. Persist filters in URL where useful so a view can be
refreshed/shared only within the same authenticated user context.

## 4.11 Document detail

Display: - Original document. - Confirmed digital record. - Vendor
details. - Transaction date/time. - Items/services. - Category
allocations. - Totals, taxes and discounts. - Processing and edit
history. - Created/updated timestamps.

Controls: - **Edit:** open edit mode. - **Save Changes:** validate and
persist. - **Cancel Edit:** discard unsaved edits after appropriate
warning. - **View Original:** show original. - **View Digital Record:**
show structured data. - **Download Original:** secure download. -
**Export Record:** CSV/PDF/XML according to entitlement. -
**Duplicate:** create a new draft only if this behavior is intentionally
supported; never duplicate silently. - **Delete:** show confirmation and
explain consequences. - **Back to Documents:** return to list and
preserve filters. - **Report Issue:** submit support/quality feedback.

## 4.12 Vendors

Vendor list fields: - Vendor name. - Vendor type/category. - Number of
documents. - Total confirmed spend. - Last transaction date. - Optional
logo/icon if available and licensed.

Controls: - **Search Vendors:** search names. - **Add Vendor:** create a
vendor manually. - **Edit Vendor:** edit normalized name/type/aliases. -
**Merge Vendors:** merge duplicate vendor records after showing impact
and requiring confirmation. - **View Vendor:** open vendor analytics and
related documents. - **Delete/Archive Vendor:** only if safe; preserve
document history and references. - **Add Alias:** associate known
receipt spelling/abbreviation with vendor. - **Assign Category:** set
default vendor category where useful.

Unknown vendor flow: 1. OCR returns candidate vendor. 2. Match
normalized name and aliases. 3. If confident, link existing vendor. 4.
If not, create a provisional vendor or leave unresolved. 5. Allow
customer to confirm/correct. 6. Store aliases for future matching. 7. Do
not merge unrelated stores based only on similar names.

## 4.13 Categories

Default categories should be editable and suitable for personal
expenses, for example: - Groceries. - Dining. - Household. -
Health/Medical. - Transportation. - Fuel. - Utilities. - Home
Improvement. - Clothing. - Electronics. - Travel. - Professional
Services. - Other.

Controls: - **Add Category:** create custom category. - **Edit
Category:** change name/color/icon. - **Archive Category:** preserve
historic records and reassign if required. - **Set Default:** select
preferred default. - **Assign Category:** apply to document/line item. -
**Bulk Assign:** assign category to selected records if supported. -
**View Category:** open category spending analytics.

Category assignment hierarchy: 1. Customer-confirmed line-item category.
2. Customer-confirmed document category. 3. Vendor default/category
rule. 4. AI-suggested category pending confidence policy. 5.
Unclassified.

Never treat an AI guess as a customer-confirmed category without a
clearly disclosed automatic classification policy.

## 4.14 Analytics

Analytics is a major product differentiator. Full requirements are in
Section 7. Analytics must be generated from confirmed, normalized data
and clearly indicate the selected period, currency and inclusion rules.

Controls: - **Date range selector:** select preset or custom range. -
**Compare with previous period:** turn comparison on/off. - **Group
by:** day/week/month/quarter/year where appropriate. - **Metric
selector:** spending, count, average, tax, discounts. - **Vendor
filter:** select one/multiple vendors. - **Category filter:** select
categories. - **Document type filter:** filter record types. -
**Currency filter:** only where multi-currency is supported. - **Chart
type selector:** switch among appropriate chart types without changing
the underlying metric. - **View transactions:** open supporting
records. - **Export chart/report:** export current filtered data. - **AI
Explain:** request a grounded explanation of the currently displayed
analytics. - **Reset filters:** restore defaults.

## 4.15 Reports

Report types: - Monthly expense summary. - Vendor/store spending
report. - Category spending report. - Product/line-item report. - Tax
and discount report. - Document register. - Custom date-range report.

Controls: - **New Report:** open report configuration. - **Select Report
Type:** choose report template. - **Select Date Range:** define
period. - **Add Filters:** vendor/category/type/amount. - **Generate
Report:** query real data and calculate. - **Preview:** display
report. - **Download PDF:** generate PDF if enabled. - **Export CSV:**
generate CSV. - **Export XML:** generate XML if enabled. - **Save Report
Configuration:** save reusable filters if implemented. - **Delete Saved
Report:** delete saved configuration, not underlying documents.

## 4.16 Global search

Search across: - Vendor names. - Original and normalized item
descriptions. - Service descriptions. - Invoice/receipt numbers. -
Transaction dates. - Amounts. - Categories. - Document filenames.

Controls: - **Search input:** submit/debounce query. - **Clear:** clear
query. - **Result type filter:** documents/vendors/items. -
**Date/vendor/category filters:** narrow results. - **Result row:** open
relevant document or vendor. - **Load more:** paginate.

Do not send sensitive receipt content to third-party search services
unless explicitly required and disclosed. Search should use the
application's own indexed database.

## 4.17 Export and downloads

Formats: - CSV for tabular data. - PDF for human-readable
reports/documents. - XML only if a documented schema and customer use
case are defined.

Controls: - **Export Current View:** export filtered records. - **Export
Selected:** export selected records. - **Export Date Range:** export
records in a chosen period. - **Include Line Items:** include detailed
item rows. - **Include Original Files:** package originals only if ZIP
export is implemented. - **Generate:** create export job. -
**Download:** issue short-lived authorized download. - **Cancel
Export:** cancel pending job if safe.

Rules: - CSV must escape delimiters, quotes and newlines correctly. -
Prevent spreadsheet formula injection for untrusted cell values. - PDF
must handle long vendor/product names and page breaks. - ZIP exports
must not include another user's files. - Large exports must run
asynchronously. - Expiring download links must be refreshable through
authorization. - Show export status and clear failures.

## 4.18 Profile and settings

Sections: - Personal profile. - Email/account security. - Password. -
Preferences. - Default currency and locale. - Date format/time zone. -
Document preferences. - Privacy/data export. - Account deletion. -
Subscription/billing.

Controls: - **Edit Profile:** enable editing. - **Save Profile:**
validate and persist. - **Cancel:** discard unsaved edits. - **Change
Email:** initiate secure verification flow. - **Change Password:**
require appropriate current/re-authentication. - **Default Currency:**
set preference; do not silently convert old records. - **Time Zone:**
affect display and date boundaries, not original transaction facts. -
**Export My Data:** prepare customer data export. - **Delete Account:**
reauthenticate, confirm, explain retention and initiate deletion
workflow. - **Sign Out:** revoke/clear session.

## 4.19 Subscription and billing

Screens: - Current plan and status. - Usage and remaining allowance. -
Plan comparison. - Payment method / Stripe Customer Portal. - Billing
history. - Cancellation status.

Controls: - **Upgrade to Pro:** create Stripe Checkout session for
configured price. - **Choose Monthly/Annual:** select configured
price. - **Manage Billing:** open Stripe Customer Portal. - **Cancel
Subscription:** use Stripe portal or supported cancellation workflow. -
**Resume Subscription:** only if provider state supports it. - **View
Invoice:** open/download Stripe invoice through authorized flow. -
**Refresh Subscription:** retrieve server-synchronized status. - **View
Usage:** show period usage and limit.

Stripe requirements: - Verify webhook signature. - Handle
duplicate/out-of-order events. - Store Stripe
customer/subscription/price IDs. - Map webhook events to internal state
safely. - Do not activate paid access based solely on return URL. -
Handle payment failure, trial if used, past_due, canceled, incomplete
and expired states. - Keep access rules explicit for each status. -
Never delete old documents after downgrade/cancellation. - Enforce
quotas on backend atomically.

## 4.20 Notifications and help

Notification types: - Upload complete. - Processing complete. -
Processing needs review. - Processing failed. - Usage limit warning. -
Payment successful/failed. - Subscription change. - Security events.

Controls: - **Notification bell:** open list. - **Open notification:**
navigate to relevant record. - **Mark as read:** update read state. -
**Mark all read:** update all current user's notifications. -
**Dismiss:** dismiss where supported. - **Help/Contact:** open real
support channel/form.

Do not create a notification feature that has no real events behind it.

## 4.21 Admin/operations functions

If the project includes an admin portal, inventory it separately. Admin
features must not be exposed to normal users.

Minimum operational capabilities: - Search users by safe identifiers. -
View account status and subscription status. - View document processing
status and safe error codes. - View provider job IDs and processing
usage. - Retry failed jobs with authorization. - View aggregate service
health and queue depth. - Review support requests. - Audit admin
actions.

Do not expose receipt content or sensitive medical/financial details to
support/admin users by default. Apply least privilege and audit access.

------------------------------------------------------------------------

# 5. Data model and relationships

Codespaces must inspect the current schema and adapt it. Do not create
duplicate tables if equivalent tables already exist. The following is
the logical model required for the product.

## 5.1 Core entities

### profiles

-   id (same identity as authenticated user ID)
-   display_name
-   preferred_currency
-   locale
-   timezone
-   created_at
-   updated_at

### documents

-   id
-   user_id
-   original_filename
-   mime_type
-   file_size
-   s3_object_key
-   document_type
-   status
-   upload_source (file picker/camera)
-   uploaded_at
-   processed_at
-   confirmed_at
-   deleted_at
-   checksum
-   page_count
-   currency
-   processing_error_code
-   created_at
-   updated_at

### extraction_versions

-   id
-   document_id
-   user_id
-   provider
-   provider_job_id
-   raw_result_location or securely stored structured raw output
-   normalized_candidate_json
-   confidence_summary
-   version_number
-   created_at

Do not store secrets or presigned URLs in this table. Apply retention
and access restrictions to raw provider output.

### transactions (confirmed digital document record)

-   id
-   document_id
-   user_id
-   vendor_id
-   document_number
-   transaction_date
-   transaction_time
-   currency
-   subtotal
-   tax_total
-   discount_total
-   shipping_total
-   tip_total
-   other_fees
-   grand_total
-   payment_type (optional)
-   document_type
-   category_id (optional)
-   confirmation_status
-   created_at
-   updated_at

### line_items

-   id
-   transaction_id
-   user_id
-   raw_description
-   normalized_description
-   product_key (optional)
-   sku_or_upc (optional)
-   quantity
-   unit
-   unit_price
-   discount_amount
-   tax_amount
-   line_total
-   category_id
-   normalization_status
-   confidence
-   source_page
-   created_at
-   updated_at

### vendors

-   id
-   user_id
-   normalized_name
-   display_name
-   vendor_type
-   default_category_id
-   created_at
-   updated_at

### vendor_aliases

-   id
-   vendor_id
-   user_id
-   alias
-   normalized_alias

### categories

-   id
-   user_id
-   name
-   parent_category_id (optional)
-   color
-   icon
-   is_default
-   archived_at

### processing_jobs

-   id
-   user_id
-   document_id
-   job_type
-   status
-   attempts
-   idempotency_key
-   provider_job_id
-   last_error_code
-   started_at
-   completed_at
-   next_retry_at
-   created_at

### subscriptions

-   id
-   user_id
-   provider
-   stripe_customer_id
-   stripe_subscription_id
-   stripe_price_id
-   plan_code
-   status
-   current_period_start
-   current_period_end
-   cancel_at_period_end
-   created_at
-   updated_at

### plans / entitlements

-   plan_code
-   display_name
-   price configuration reference
-   billing interval
-   document processing allowance
-   storage allowance
-   analytics/export feature flags
-   active status

### usage_periods

-   id
-   user_id
-   plan_code
-   period_start
-   period_end
-   documents_processed
-   storage_bytes
-   other metered usage if required

### notifications

-   id
-   user_id
-   type
-   title
-   body
-   related_document_id
-   read_at
-   created_at

### export_jobs

-   id
-   user_id
-   export_type
-   filters_json
-   status
-   s3_object_key
-   error_code
-   created_at
-   completed_at
-   expires_at

### audit_events

-   id
-   user_id (actor)
-   action
-   entity_type
-   entity_id
-   safe_metadata
-   created_at

## 5.2 Data integrity rules

-   All private tables include user ownership or enforce ownership
    through a parent relationship and RLS.
-   Foreign keys and unique constraints prevent invalid relationships.
-   Use decimal/numeric types for money, never binary floating point.
-   Store currency code with monetary values.
-   Do not sum different currencies together without an explicit
    exchange-rate conversion policy.
-   Store transaction date as the source transaction date; store
    timestamps in UTC.
-   Use the user's timezone for reporting period boundaries.
-   Keep raw OCR and confirmed values distinct.
-   Maintain original document relationship even when the digital record
    is edited.
-   Soft-delete or retention behavior must be explicit.
-   Analytics must exclude deleted and unconfirmed records by default.

## 5.3 Security policies

-   Enable RLS for all user-owned tables.
-   Policies must derive user identity from the authenticated session,
    never from a client-supplied `user_id`.
-   Storage objects are private.
-   Backend functions must independently authorize access.
-   Never expose service-role keys to browser code.
-   Never use a public S3 bucket for private receipts.
-   Test cross-user access attempts for every entity and download route.

------------------------------------------------------------------------

# 6. Upload-to-digital-record workflow in detail

1.  Customer clicks **Upload Receipt** or **Upload Invoice**.
2.  Upload modal/page opens.
3.  Customer chooses file, drops file, or takes photo.
4.  Client validates size/type for immediate feedback.
5.  Server validates actual file signature, account, plan allowance and
    rate limits.
6.  Backend creates a document record in `created` state.
7.  Backend issues a short-lived S3 upload URL for a generated private
    key.
8.  Browser uploads directly to S3 using the URL.
9.  Browser calls completion endpoint.
10. Backend verifies object exists and expected metadata/size/checksum
    where possible.
11. Document moves to `uploaded`.
12. Backend reserves/consumes processing quota according to documented
    policy.
13. Backend creates idempotent OCR job.
14. Worker invokes OCR provider.
15. Worker stores raw extraction securely and updates status.
16. Validation checks dates, numeric fields, totals, currency, line
    items and document type.
17. AI enrichment receives only the necessary extracted fields and
    returns structured suggestions.
18. Worker matches or proposes vendor and category.
19. Document moves to `needs_review` or `confirmed` only under an
    explicit safe auto-confirm policy. For initial launch, use customer
    review for uncertain or financially material fields.
20. UI displays progress and then review page.
21. Customer checks original side-by-side with extracted data.
22. Customer edits vendor, date, line items, categories, taxes or
    totals.
23. Customer clicks **Confirm & Save**.
24. Server validates and commits transaction plus line items in one
    database transaction.
25. Document moves to `confirmed`.
26. Dashboard, vendor summaries, category summaries and analytics update
    from confirmed records.
27. Customer sees success and can open the saved record.
28. If any step fails, preserve the original file and show recoverable
    status; do not lose the upload.

------------------------------------------------------------------------

# 7. Dynamic analytics and data visualization engine

This is a core product capability, not decoration. Build a reusable
analytics query layer and chart components that adapt to the amount and
quality of available data.

## 7.1 Analytics principles

1.  Use actual confirmed transaction and line-item data.
2.  Default to confirmed records; provide an explicit option to include
    drafts/unconfirmed records only if useful and clearly labeled.
3.  Every visualization displays its date range, currency, metric and
    applied filters.
4.  All charts must have a useful empty state.
5.  Do not show a pie chart with dozens of tiny slices. Group small
    categories into "Other" and provide a detailed table.
6.  Do not compare incompatible currencies.
7.  Distinguish document count from transaction count and line-item
    count.
8.  Do not imply that a vendor's total is a product-level price
    comparison.
9.  Explain calculations and comparison baselines.
10. Clicking a chart segment/bar/point should open the supporting
    filtered transactions.
11. Charts and tables must use the same filters and totals.
12. Use server-side aggregation for larger histories; do not download
    every receipt to the browser to calculate totals.
13. Cache only where safe and invalidate/revalidate after confirmation,
    edit, deletion or category change.
14. AI explanations must be grounded in the analytics result returned by
    the backend.

## 7.2 Global analytics controls

At the top of Analytics: - Date range: This month, Last month, Last 3
months, Last 6 months, This year, Last year, All time, Custom. -
Comparison: previous equivalent period / same period last year / off. -
Vendor multi-select. - Category multi-select. - Document type. -
Minimum/maximum spend. - Metric: amount, transaction count, average
transaction, tax, discount. - Group interval: day, week, month, quarter,
year. - Currency selector only if multi-currency data is supported. -
Reset filters. - Export current view. - Ask AI / Explain this view.

Changing any filter must refresh all relevant cards, charts and tables
consistently.

## 7.3 KPI cards

-   Total spend in selected period.
-   Number of confirmed transactions.
-   Number of documents.
-   Average transaction value.
-   Highest transaction.
-   Total tax.
-   Total discounts.
-   Unique vendors.
-   Top category.
-   Top vendor.

Each card shows: - Value. - Period. - Change versus comparison period if
selected. - Tooltip explaining calculation. - Link to supporting
records.

## 7.4 Required charts and when to use them

### A. Spending over time --- line or area chart

-   X-axis: day/week/month.
-   Y-axis: spend in one currency.
-   Optional series: current period and comparison period.
-   Use line chart for trend; area fill only when it improves
    readability.
-   Click point to view transactions in that interval.

### B. Spending by store/vendor --- horizontal bar chart

-   Rank vendors by spend.
-   Default top 5 or top 10, with "Other vendors" or a full table.
-   Show vendor name, spend, transaction count and share.
-   Click vendor to open vendor detail and supporting documents.

### C. Spending by category --- donut chart plus table

-   Donut for a small number of meaningful categories.
-   Show total and percentage.
-   If too many categories, use horizontal bar chart instead.
-   Table lists every category, amount, transaction count and
    percentage.
-   Click category to filter transactions.

### D. Store comparison --- grouped bar chart

-   Compare selected vendors over the same period.
-   Show total spend and transaction count separately.
-   Do not imply value/price efficiency from total spend alone; purchase
    quantities and comparable products are required for price
    comparison.

### E. Product spending --- horizontal bar chart

-   Aggregate normalized line-item product names.
-   Show quantity, total spend, average unit price and number of
    receipts.
-   Exclude unverified product identities from exact product matching;
    show them under "Unclassified receipt items" or raw description
    group.

### F. Month-over-month comparison --- grouped columns

-   Current month vs previous month or several months.
-   Use same calendar/time-zone boundaries.
-   Show amount and percentage change.
-   Avoid misleading percentage change when prior period is zero;
    display "New" or "No prior spend."

### G. Transaction distribution --- histogram or amount bands

-   Show how many transactions fall into amount ranges.
-   Useful for understanding small versus large purchases.

### H. Tax and discount analysis --- stacked/grouped bars or summary table

-   Tax paid by month/vendor/category where source data supports it.
-   Discounts by month/vendor.
-   Do not assume all tax fields are correctly extracted; expose
    coverage/quality note.

### I. Calendar heatmap (optional P1)

-   Daily spend intensity.
-   Click day to see receipts.
-   Do not show if sparse data makes it unhelpful.

### J. Vendor/category matrix (optional P1)

-   Rows: vendors.
-   Columns: categories.
-   Cells: spend.
-   Useful for seeing what types of expenses occur at each store.
-   Include accessible table alternative.

## 7.5 Analytics tables

Provide tables alongside or below charts: - Vendor summary. - Category
summary. - Product summary. - Monthly summary. - Transaction details.

Tables need: - Sortable columns. - Search where relevant. -
Pagination. - Currency-aware formatting. - Export current filtered
table. - Row click to supporting record. - Clear empty and loading
states.

## 7.6 Example: 10 receipts from one store and 10 from another

When a customer has 10 Walmart receipts and 10 Costco receipts,
analytics should be able to show:

**Overall summary** - Total confirmed spend across all 20 receipts. -
Number of receipts/transactions. - Average receipt amount. - Date range.

**Vendor comparison** - Walmart total spend, receipt count, average
receipt. - Costco total spend, receipt count, average receipt. -
Percentage of total spend at each store. - Trend by month/week.

**Category comparison** - Groceries at Walmart vs Costco. - Household at
Walmart vs Costco. - Health/medical, electronics, etc., where present. -
A matrix/table and grouped bar chart.

**Product analysis** - Total spent on milk across both stores, only if
product identity and unit are sufficiently normalized. - Quantity
purchased. - Average unit price. - Most recent purchase date. - Price
history by vendor. - Same product comparison only when product identity,
package size, unit and currency are comparable.

**Receipt drill-down** - Click Walmart bar → show the 10 relevant
Walmart records. - Click groceries segment → show matching line
items/transactions. - Click product → show receipts containing the
product. - Click month → show records in that month.

The application must not invent product equivalence. For example, a
1-gallon product and a 2-liter product are not directly comparable
without unit conversion and reliable product identity.

## 7.7 Product normalization and price comparison

This is a high-value feature but must be carefully controlled.

Maintain: - Raw receipt description. - Clean display description. -
Product identity/match key where verified. - Brand if known. -
Size/weight/volume. - Unit of measure. - Quantity. - Unit price. -
Vendor. - Transaction date. - Match confidence and source.

Normalization workflow: 1. Preserve raw OCR description. 2. Remove
obvious noise/formatting only. 3. Use SKU/UPC if actually extracted and
reliable. 4. Use vendor-specific alias rules. 5. Use AI to propose
readable normalization. 6. Compare candidate identity and package size.
7. Accept automatic match only above a conservative threshold and where
evidence is strong. 8. Otherwise mark "Possible match" for user review.
9. Keep match history and allow correction.

Price comparison: - Compare unit price only when product identity,
package size, unit and currency are comparable. - Normalize units only
with safe known conversions. - Display last price, lowest observed
price, highest observed price and date range only where sample size
supports it. - Show the number of observations. - Clearly label "based
on your uploaded receipts," not market-wide pricing. - Do not claim one
store is cheaper from total receipt spend.

## 7.8 AI spending insights

AI is an explanatory layer over deterministic analytics, not the
calculator or database.

Example customer questions: - "Where did I spend the most this month?" -
"Compare my Walmart and Costco spending." - "What categories increased
compared with last month?" - "What products do I buy most often?" - "How
much did I spend on home improvement?" - "Show my top 10 products by
total spend." - "Which items have become more expensive in my
receipts?" - "Summarize my spending this month." - "How much tax did I
pay this quarter?"

AI workflow: 1. User asks a question. 2. Backend authenticates user. 3.
Backend parses intent into an allowlisted analytics query, not arbitrary
SQL. 4. Analytics service executes a parameterized, user-scoped query.
5. Return aggregate data and data-quality metadata to AI. 6. AI
generates a concise explanation with exact numbers supplied by the
analytics service. 7. UI displays answer, date range, filters and
supporting chart/table. 8. "View supporting records" opens the
underlying filtered records. 9. If data is insufficient, AI says so. 10.
Do not send full receipt images or sensitive medical details to the AI
service for routine analytics.

AI must never: - Execute arbitrary SQL. - Access another user's
records. - invent values or causes. - assert a product match without
evidence. - claim savings or price superiority without comparable
data. - treat missing tax as zero. - combine currencies without
conversion. - give tax/legal/medical advice based on expense labels.

## 7.9 Analytics data quality and confidence

Display helpful caveats: - "Based on 18 confirmed documents." - "3
documents still need review and are excluded." - "Product comparison
includes 6 verified matches." - "Tax information was unavailable on 2
receipts." - "Some items could not be identified."

When data is sparse: - Show a simple summary and table instead of
empty/meaningless charts. - Avoid false precision. - Explain that more
confirmed receipts improve trend analysis.

------------------------------------------------------------------------

# 8. Pricing, quotas and subscription behavior

## 8.1 Plan configuration

Initial plans from the source specification: - Free: \$0; proposed 15
processed documents/month. - Pro: \$10/month; proposed 500 processed
documents/month. - Optional Pro annual: \$99/year.

Codespaces must verify actual Stripe products/prices and product owner
decisions. Do not create Stripe price IDs in code or assume they already
exist.

Plan feature flags: - Basic/advanced analytics. - Export formats. - Bulk
download. - Historical reporting. - Storage limit. - Monthly
processed-document allowance. - Priority processing, only if
operationally real.

## 8.2 Quota workflow

1.  User selects a file.
2.  Backend checks current plan and current billing period usage.
3.  Backend reserves capacity atomically or rejects with a clear limit
    message.
4.  Upload proceeds.
5.  Processing outcome updates usage according to documented billing
    definition.
6.  Failed technical processing should have a fair retry/refund policy.
7.  Concurrent uploads cannot exceed allowance due to race conditions.
8.  User sees used/remaining quota.
9.  At limit, show upgrade option and retain already uploaded files.
10. Never delete old documents on downgrade.

## 8.3 Checkout workflow

1.  User clicks Pro plan.
2.  Store plan intent securely.
3.  If unauthenticated, route to sign up/login.
4.  After auth, resume the exact selected plan and interval.
5.  Backend resolves configured Stripe Price ID.
6.  Backend creates/reuses Stripe customer safely.
7.  Backend creates Checkout Session.
8.  Browser redirects to Stripe.
9.  Success/cancel return routes show pending/confirmed state
    appropriately.
10. Stripe webhook verifies and updates subscription.
11. App refreshes entitlement from server.
12. Dashboard shows Pro only after authoritative confirmation.
13. Handle browser closed after payment by webhook-driven
    synchronization.
14. Handle duplicate/out-of-order webhooks idempotently.

------------------------------------------------------------------------

# 9. Notifications and customer communication

Transactional email events: - Account verification. - Password reset. -
Upload/processing complete if enabled. - Processing failure requiring
customer action. - Usage warning. - Payment receipt/confirmation (Stripe
may send invoice emails depending on configuration). - Payment
failure. - Subscription cancellation/status update. - Security-sensitive
account changes.

In-app notifications: - Processing needs review. - Processing failed. -
Export ready. - Usage limit warning. - Subscription status change.

Every notification must link to the correct user-owned resource. Respect
email preferences where applicable. Do not send unnecessary marketing
messages as transactional email.

------------------------------------------------------------------------

# 10. Privacy, legal and security

Before public launch: - Publish Terms of Service. - Publish Privacy
Policy. - Explain document storage, OCR and AI providers. - Explain what
data is sent to each processor. - Provide support/contact method. -
Provide account deletion and data export route. - Define document
retention and deletion behavior. - Review provider data processing terms
and retention settings. - Configure S3 private bucket, encryption at
rest and TLS. - Restrict IAM permissions to required bucket actions. -
Enable Supabase RLS on user-owned data. - Protect service-role/API
keys. - Verify Stripe webhook signatures. - Rate-limit auth, upload,
recovery and AI endpoints. - Validate uploads and file content. - Avoid
logging document contents, passwords, tokens, payment data or sensitive
medical details. - Use Sentry scrubbing to remove sensitive payloads. -
Audit administrative access. - Test cross-user access. - Back up
database and verify restoration procedure.

Medical bills may contain sensitive personal information. Do not claim
HIPAA compliance or accept regulated healthcare workflows unless the
architecture, vendor agreements, policies and operational controls have
been specifically assessed. For launch, treat such documents as private
personal documents and clearly disclose the processing services used.

------------------------------------------------------------------------

# 11. Error states and exact user feedback behavior

Implement consistent messages, not raw stack traces.

  -----------------------------------------------------------------------
  Scenario                            Expected user experience
  ----------------------------------- -----------------------------------
  Registration success                "Your account has been created." If
                                      verification is required, explain
                                      the next step.

  Existing account                    Safe message guiding the user to
                                      sign in or recover access without
                                      exposing unnecessary account
                                      details

  Invalid credentials                 "Email or password is incorrect."

  Reset request                       Privacy-safe confirmation that
                                      instructions will be sent if the
                                      account is eligible

  Upload unsupported file             State supported file types

  File too large                      State configured maximum size

  Upload network failure              Explain upload failed and provide
                                      retry

  Processing underway                 Show current status and allow
                                      navigation away

  OCR uncertain                       Highlight fields requiring review

  Processing failed                   Explain that the original file is
                                      safe and offer retry if eligible

  Duplicate suspected                 Show existing possible match and
                                      let user decide

  Total mismatch                      Show extracted total and calculated
                                      line-item total; ask user to review

  Save success                        Confirm record saved and link to
                                      detail

  Save failure                        Preserve edits and allow retry

  No documents                        Explain how to upload the first
                                      receipt

  No analytics data                   Explain that confirmed documents
                                      are needed before insights appear

  No search results                   Show filters/query and clear-filter
                                      action

  Free quota reached                  Explain limit, reset date and
                                      upgrade option

  Checkout canceled                   Return to pricing/billing with no
                                      false payment-success message

  Payment pending                     Explain that payment confirmation
                                      is still processing

  Payment failed                      Explain status and link to billing
                                      retry/update method

  Session expired                     Preserve safe return destination
                                      and ask user to sign in

  Unauthorized record                 Show not-found/permission-safe
                                      response; never reveal another
                                      user's record
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 12. Testing requirements and acceptance scenarios

## 12.1 Authentication tests

-   New account.
-   Duplicate email.
-   Invalid email.
-   Weak password.
-   Verification success.
-   Expired verification link.
-   Login success/failure.
-   Forgot/reset password.
-   Expired reset link.
-   Logout.
-   Session expiry.
-   Protected route redirect.
-   Return-to-checkout preservation.

## 12.2 Upload and extraction tests

-   Valid JPG.
-   Valid PNG.
-   Valid PDF.
-   Mobile camera photo.
-   Unsupported file.
-   Oversized file.
-   Empty/corrupt file.
-   Network interruption.
-   S3 upload succeeds but DB completion fails.
-   DB record exists but S3 upload fails.
-   OCR provider timeout.
-   OCR provider failure.
-   Retry.
-   Duplicate upload.
-   Low confidence.
-   Missing vendor.
-   Missing date.
-   Missing total.
-   Multiple tax lines.
-   Discount.
-   Tip/fee.
-   Currency.
-   Multi-page PDF.
-   Non-retail invoice.
-   Hospital bill.
-   Contractor invoice.

## 12.3 Review and data tests

-   Edit vendor.
-   Edit date.
-   Edit amount.
-   Add/edit/delete line item.
-   Category assignment.
-   Total discrepancy.
-   Save draft.
-   Confirm.
-   Refresh after confirm.
-   Original file remains linked.
-   Reprocess does not duplicate confirmed transaction.

## 12.4 Analytics tests

-   One receipt.
-   Multiple receipts from same vendor.
-   Receipts from multiple vendors.
-   Multiple categories.
-   Missing category.
-   Unconfirmed record excluded by default.
-   Deleted record excluded.
-   Custom date range.
-   Time-zone boundaries.
-   Zero prior-period spend.
-   Currency mismatch.
-   Product aliases.
-   Same product with different package sizes.
-   Low-confidence product excluded from exact comparison.
-   Chart/table totals agree.
-   Chart click-through filters correctly.
-   AI explanation matches analytics result.
-   User A cannot query User B data.

## 12.5 Billing tests

-   Free plan.
-   Pro monthly checkout.
-   Pro annual checkout if enabled.
-   Checkout canceled.
-   Payment succeeds.
-   Webhook delayed.
-   Webhook repeated.
-   Webhook invalid signature.
-   Payment fails.
-   Cancellation.
-   Renewal.
-   Downgrade.
-   Quota limit.
-   Concurrent upload at quota boundary.
-   Historical records remain after cancellation.

## 12.6 End-to-end launch test

Use a clean test account: 1. Visit landing page. 2. Register. 3. Verify
email if enabled. 4. Reach dashboard. 5. Upload a sample retail receipt.
6. Wait for processing. 7. Review extracted fields. 8. Correct one
field. 9. Confirm and save. 10. Upload another vendor receipt. 11.
Upload a PDF invoice. 12. Search for a product and vendor. 13. Open
original and digital record. 14. Review vendor/category analytics. 15.
Compare current and previous periods. 16. Click a chart and inspect
supporting records. 17. Export CSV. 18. Upgrade to Pro using Stripe test
mode. 19. Confirm webhook-driven entitlement. 20. Open billing portal.
21. Sign out and sign back in. 22. Confirm records and subscription
persist. 23. Attempt unauthorized access using another test account. 24.
Confirm access is denied.

------------------------------------------------------------------------

# 13. Go-live-this-week execution schedule

This is a proposed sequence, not a promise that every optional feature
can be completed in five days. Codespaces must first inspect the existing
implementation and report actual effort/blockers. Protect the P0 release
scope.

## Day 1 --- Audit, architecture and critical path

-   Inspect repository and running application.
-   Inventory every route, button, form and existing API.
-   Inspect Supabase schema, RLS, Auth and migrations.
-   Inspect S3 or current file storage.
-   Inspect OCR/AI and Stripe integrations.
-   Produce defect/gap list.
-   Fix broken registration/login/session/redirect behavior.
-   Confirm environment variables and deployment environments.
-   Verify that a clean account can be created.

**Exit gate:** architecture known; critical auth and account isolation
issues identified/fixed; no destructive schema changes without migration
plan.

## Day 2 --- Upload, storage and document processing

-   Secure upload to private storage.
-   File validation.
-   Document status lifecycle.
-   OCR worker/provider integration.
-   Extraction persistence.
-   Retry/error handling.
-   Review screen.
-   Confirm/save transaction and line items.

**Exit gate:** a real sample receipt and PDF can be uploaded, processed,
reviewed and saved with original file retained.

## Day 3 --- Documents, vendors, categories and dashboard

-   Document list/detail.
-   Search and filters.
-   Vendor matching and aliases.
-   Category assignment.
-   Dashboard real metrics.
-   Recent documents.
-   Basic line-item and transaction data integrity.

**Exit gate:** customer can find, view, edit, categorize and download
their own documents.

## Day 4 --- Analytics, export and billing

-   Monthly spending trend.
-   Vendor and category charts.
-   KPI cards.
-   Date range and comparisons.
-   Click-through to supporting records.
-   CSV export.
-   Stripe checkout and webhooks if Pro launches.
-   Quota enforcement and billing status.

**Exit gate:** chart/table numbers match database results; payment
status is webhook-driven; limits enforced server-side.

## Day 5 --- QA, security and deployment

-   Full end-to-end tests.
-   Cross-user isolation tests.
-   Mobile/responsive tests.
-   Failure/retry tests.
-   Stripe test-mode scenarios.
-   Production environment validation.
-   Domain/HTTPS.
-   Email delivery.
-   S3 access policy.
-   Database backup and restore check.
-   Error monitoring.
-   Terms/privacy/support links.
-   Production smoke test.

**Exit gate:** no open P0 defects; deployment and rollback plan ready;
all external integrations verified.

If a day slips, defer P1/P2 features. Do not skip authorization, private
storage, payment verification, data integrity, backups or core workflow
tests to meet the date.

------------------------------------------------------------------------

# 14. Production environment configuration checklist

Use secret management/environment variables. Never commit secrets.

Expected configuration categories: - Supabase project URL. - Supabase
anon/public key for browser use. - Supabase service-role key for
server-only use. - Database connection configuration where required. -
AWS region. - S3 bucket name. - AWS IAM credentials/role with least
privilege. - Textract configuration/region. - OpenAI API key
(server-only). - Stripe publishable key. - Stripe secret key
(server-only). - Stripe webhook signing secret. - Stripe monthly Price
ID. - Stripe annual Price ID if enabled. - Resend API key. - Email
sender/domain configuration. - Application public URL. - Sentry DSN. -
Worker/queue configuration. - Allowed origins and redirect URLs.

Codespaces must produce an `.env.example` with variable names and safe
placeholders only. Never place real secrets in source control, logs or
frontend bundles.

------------------------------------------------------------------------

# 15. Observability and operational readiness

Track: - Registration/login failures (without logging passwords or
tokens). - Upload success/failure. - OCR processing duration and failure
rate. - AI enrichment duration and failure rate. - Queue depth and retry
counts. - Storage usage. - Processing usage by plan. - Stripe webhook
delivery and processing status. - Export failures. - API error rate and
latency.

Set alerts for: - OCR/worker queue stuck. - Repeated provider
failures. - Stripe webhook failures. - S3 access errors. - Database
connectivity failures. - Sudden increase in API errors. - Unexpected
storage/processing cost.

Provide a safe operational retry mechanism. Retrying must be idempotent.

------------------------------------------------------------------------

# 16. Performance and scale requirements

-   Paginate documents and search results.
-   Use database indexes for user_id, transaction_date, vendor_id,
    category_id, status and common search fields.
-   Aggregate analytics in Postgres/server-side queries.
-   Avoid N+1 queries.
-   Do not load all line items for every dashboard render.
-   Use asynchronous jobs for OCR, AI and large exports.
-   Optimize thumbnails/previews without altering original files.
-   Cache derived aggregates carefully and invalidate after
    edits/deletes/confirmation.
-   Set provider timeouts and bounded retries.
-   Apply per-user rate limits and quotas.
-   Monitor S3, OCR, AI and database cost per active user.

------------------------------------------------------------------------

# 17. Launch decision checklist

A public launch is allowed only when all mandatory items are true:

-   [ ] Production Supabase project and migrations verified.
-   [ ] RLS enabled and cross-user tests pass.
-   [ ] Registration/login/logout work.
-   [ ] Password recovery works.
-   [ ] Private S3 storage and authorized downloads work, or existing
    private storage is verified.
-   [ ] Upload validation works.
-   [ ] OCR works with real test documents.
-   [ ] Processing failures are recoverable.
-   [ ] Review/edit/confirm works.
-   [ ] Original file and digital record remain linked.
-   [ ] Document list/detail/search work.
-   [ ] Dashboard totals are real and correct.
-   [ ] Basic analytics and drill-down work.
-   [ ] CSV export works.
-   [ ] Free plan limits are enforced.
-   [ ] Stripe checkout/webhooks are verified if Pro is sold at launch.
-   [ ] Email delivery is verified.
-   [ ] Privacy policy and terms are published.
-   [ ] Support contact works.
-   [ ] HTTPS and production domain work.
-   [ ] Secrets are not exposed.
-   [ ] Monitoring and error reporting work.
-   [ ] Database backup and recovery plan exist.
-   [ ] Mobile upload and responsive core screens tested.
-   [ ] End-to-end smoke test passes.
-   [ ] No open critical/high severity defect affecting security, data
    integrity, payments or core document workflow.

If Stripe is not ready, launch Free-only rather than accepting payments
with an unreliable billing flow. If OCR is not ready, do not market
automated extraction as working. Be transparent about any temporarily
unavailable feature.

------------------------------------------------------------------------

# 18. Required project documentation

Maintain these files in the repository:

1.  `PRODUCT_FEATURE_INVENTORY.md` --- every page, feature, component,
    control and button.
2.  `USER_WORKFLOWS.md` --- end-to-end workflows, alternate paths and
    error paths.
3.  `ARCHITECTURE_AND_SERVICES.md` --- service responsibilities, data
    flow, secrets and integrations.
4.  `DATABASE_AND_RLS.md` --- schema, relationships, indexes, policies
    and migrations.
5.  `DOCUMENT_PROCESSING_PIPELINE.md` --- upload, OCR, AI, validation,
    review, retry and status lifecycle.
6.  `ANALYTICS_SPECIFICATION.md` --- metrics, formulas, filters, chart
    rules, drill-down and AI insight boundaries.
7.  `SUBSCRIPTION_AND_ENTITLEMENTS.md` --- plans, Stripe events, quotas
    and access rules.
8.  `TEST_PLAN.md` --- test cases and expected outcomes.
9.  `GO_LIVE_CHECKLIST.md` --- production readiness and rollback.
10. `IMPLEMENTATION_STATUS.md` --- status per feature: Not started / In
    progress / Implemented / Tested / Blocked.

For each feature in the inventory, include: - Feature ID. -
Screen/route. - Visible control names. - User action. - Expected
behavior. - Backend/API operation. - Database/storage effect. -
Permission requirement. - Success state/message. - Error and empty
states. - Test case IDs. - Implementation status.

------------------------------------------------------------------------

# 19. Codespaces's step-by-step execution protocol

For every phase:

1.  Read the relevant feature/workflow requirements.
2.  Inspect current code and identify reusable components.
3.  List exact files/components/APIs/database changes to be made.
4.  Identify migration and compatibility risks.
5.  Implement one coherent workflow at a time.
6.  Add/update automated tests.
7.  Run tests and relevant static checks.
8.  Manually verify the UI flow where browser tooling is available.
9.  Update implementation status.
10. Report verified behavior and remaining blockers.
11. Proceed only when the phase exit gate is met.

Do not make a broad code change across the entire application without a
plan. Do not mark a feature "done" because the frontend compiles.

## First instruction to Codespaces

**Start with audit only. Do not change application code yet.**

Inspect the existing repository and the source specification. Produce: -
Complete feature/button inventory. - Current architecture and service
inventory. - Current database/RLS assessment. - User workflow map. -
Missing/broken feature matrix. - P0/P1/P2 launch classification. -
Day-by-day plan based on the actual codebase. - Exact credentials and
external setup still required.

Then stop and present the assessment. After it is reviewed, implement
Phase 1 and continue in controlled steps.

**Final standard:** A customer must be able to upload real documents,
obtain accurate reviewable digital records, understand their spending
through trustworthy dynamic analytics, retrieve/export their
information, and manage their account securely. Every visible control
must work or be clearly unavailable; nothing may pretend to work.
