# Ledgerly

Ledgerly is a personal invoice and expense management application. Customers can keep receipts, invoices, and bills together with their original documents, review organized details, and understand their spending.

## Technology

- React + TypeScript + Vite frontend
- Spring Boot REST API (Java 21)
- PostgreSQL with Flyway migrations
- JWT authentication
- Stripe subscriptions (planned for Phase 6)

## Current implementation status

Phase 1 establishes the product landing page, pricing presentation, authentication screens, responsive personal dashboard shell, account profile UI, and shared design system.

Document upload, account-scoped document management, AI processing, reporting, exports, and production subscriptions are being implemented in the subsequent phases described in [mange-invoice-new-plan.txt](./mange-invoice-new-plan.txt). The dashboard currently uses honest new-account empty states; it does not display fabricated customer analytics.

## Local development

Requirements: Java 21, Maven, Node.js, npm, and Docker Compose.

1. Copy `.env.example` to `.env`. Set a random `JWT_SECRET` of at least 32 characters and local PostgreSQL connection values.
2. Start PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

3. Start the API:

   ```bash
   set -a && . ./.env && set +a
   cd backend && mvn spring-boot:run
   ```

4. Start the frontend in another terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

The frontend runs at http://localhost:5173/. The API runs at http://localhost:8080/.

## Validation

```bash
cd backend && mvn test
cd frontend && npm test && npm run build
```

See [ARCHITECTURE.md](./ARCHITECTURE.md), [API.md](./API.md), [DATABASE.md](./DATABASE.md), [DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md), and [TESTING.md](./TESTING.md) for technical references.
