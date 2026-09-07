# Project Management API

All endpoints are prefixed with `/api`. Except `POST /auth/login` and
`POST /auth/signup`, send `Authorization: Bearer <accessToken>`.

## Core project workflow

| Capability | Endpoint |
| --- | --- |
| Workspaces | `GET, POST /workspaces` |
| Workspace members | `GET, POST /workspaces/{id}/members` |
| Projects | `GET, POST /projects` |
| Project members | `GET, POST /project-users` |
| Issues | `GET, POST /tickets` |
| Issue board | `GET /tickets/project/{projectId}/board` |
| Move an issue | `PUT /tickets/{id}/transition` |
| Plan/rank issues | `PUT /tickets/project/{projectId}/plan` |
| Search/filter | `GET /tickets/project/{projectId}/filter` |
| Saved views | `GET, POST /projects/{projectId}/ticket-views` |
| Analytics | `GET /projects/{projectId}/analytics` |

## Error contract

Errors use a consistent JSON body containing `timestamp`, `status`, `error`, and
`message`. Bean-validation failures additionally return an `errors` map keyed by
request field. Clients should treat `401` as unauthenticated, `403` as forbidden,
`404` as unavailable, `409` as conflict, and `422` as semantic validation when
those statuses are introduced by future APIs.

## Production configuration

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (base64, at least 256-bit),
`JWT_ISSUER`, and `CORS_ALLOWED_ORIGINS`. Do not use the development JWT default in
production. `CORS_ALLOWED_ORIGINS` is a comma-separated allow-list of frontend URLs.
