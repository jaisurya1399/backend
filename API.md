# Project Management API

All endpoints are prefixed with `/api`. Except `POST /auth/login`, `POST /auth/signup`,
and `POST /auth/refresh`, send `Authorization: Bearer <accessToken>`.

## Authentication token lifecycle

`POST /auth/login` returns a short-lived `accessToken` and a durable `refreshToken`.
Submit `{ "refreshToken": "..." }` to `POST /auth/refresh` to rotate it; the old
refresh token becomes unusable. To sign out, call `POST /auth/logout` with the access
token in the `Authorization` header and the refresh token in the JSON body. The access
token's JWT ID is revoked immediately and the supplied refresh token is revoked when it
belongs to the signed-in user.

Password reset uses `POST /auth/password-reset/request` with `{ "email": "..." }` and
always returns `204`, preventing account enumeration. The delivery provider receives a
single-use reset token that expires after 30 minutes by default. Submit that token and a
new password to `POST /auth/password-reset/confirm`; it invalidates all existing refresh
tokens for that user.

New registrations receive a single-use email-verification token, valid for 24 hours by
default. Send `{ "token": "..." }` to `POST /auth/email-verification/confirm` to mark
the email as verified. A signed-in, unverified user can request a replacement through
`POST /auth/email-verification/resend`; only the most recently issued token is valid.

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
To send password-reset and verification email, set `MAIL_ENABLED=true`, `MAIL_FROM`,
`FRONTEND_URL`, and Spring's SMTP variables (for example `SPRING_MAIL_HOST`,
`SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, and `SPRING_MAIL_PASSWORD`).

Login is rate-limited by both normalized email and client IP: five failed attempts per
15-minute window by default. Configure `AUTH_LOGIN_MAX_ATTEMPTS` and
`AUTH_LOGIN_WINDOW_MINUTES` as required. Set `TRUST_FORWARDED_HEADERS=true` only when
the API is behind a trusted reverse proxy that overwrites `X-Forwarded-For`.
