# Testing

## Backend

JUnit 5 and Spring Boot Test cover API startup, health, authentication, workspace authorization, billing APIs, and Stripe webhook behavior.

```bash
cd backend
mvn test
```

Tests use an in-memory H2 database and a test-only JWT signing secret.

## Frontend

Vitest runs frontend unit tests; the production build runs TypeScript checks.

```bash
cd frontend
npm test
npm run build
```
