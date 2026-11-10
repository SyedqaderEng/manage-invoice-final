# REST API

The API base path is `/api/v1`. Authenticated endpoints require `Authorization: Bearer <JWT>`.

## Health and documentation

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/health` | Application health |
| GET | `/actuator/health` | Operational health |
| GET | `/api/v1/openapi` | OpenAPI document |
| GET | `/api/v1/docs` | Swagger UI |

## Identity

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a user and issue a JWT |
| POST | `/api/v1/auth/login` | Authenticate and issue a JWT |
| POST | `/api/v1/auth/logout` | Stateless logout acknowledgement |
| GET | `/api/v1/auth/me` | Read the authenticated account |
| PATCH | `/api/v1/auth/me` | Update the account profile |

## Workspaces and members

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/api/v1/workspaces` | List or create workspaces |
| GET | `/api/v1/workspaces/{workspaceId}/members` | List members after membership authorization |
| POST | `/api/v1/workspaces/{workspaceId}/members` | Add an existing account (owner/admin) |
| PATCH | `/api/v1/workspaces/{workspaceId}/members/{memberId}` | Change a member role |
| DELETE | `/api/v1/workspaces/{workspaceId}/members/{memberId}` | Remove a member |

Only owners can grant or revoke administrator access. Ownership cannot be changed through member management. Membership and role checks are performed by the API, not trusted from the UI.

## Billing

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/billing/plans` | Public plan information |
| GET | `/api/v1/billing/workspaces/{workspaceId}` | Read the authorized workspace subscription |
| POST | `/api/v1/billing/workspaces/{workspaceId}/checkout` | Create Stripe Checkout session (owner/admin) |
| POST | `/api/v1/billing/workspaces/{workspaceId}/portal` | Create Stripe customer portal session (owner/admin) |
| POST | `/api/v1/billing/webhook` | Receive verified Stripe subscription events |

Stripe webhook events are signature-verified and deduplicated by event ID. Checkout and portal URLs are returned by the API and are never constructed by the frontend.
