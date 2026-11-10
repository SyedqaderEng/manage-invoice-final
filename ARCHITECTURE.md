# Architecture

The starter is a modular monolith with a React client and Spring Boot REST API.

## Backend

The API owns authentication, authorization, tenant boundaries, validation, subscription state, and persistence. Controllers delegate to services. PostgreSQL is the source of truth; Flyway owns schema changes.

## Frontend

React and TypeScript provide the public landing/pricing experience and authenticated workspace shell. The browser calls REST endpoints under `/api/v1`; authorization decisions and subscription updates remain server-side.

## Tenancy

Workspaces provide the tenant boundary. Membership is checked for each workspace-scoped operation. Owners and admins can manage members and subscriptions; owners alone can grant administrator access.

## Billing

The backend creates Stripe Checkout and customer portal sessions. A signed Stripe webhook is the source of truth for subscription state, with event IDs persisted to prevent duplicate processing.
