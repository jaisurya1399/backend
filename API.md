# Project Management API

Base path: `/api`. JSON unless noted (multipart for uploads, SSE for realtime).

## Authentication

Send `Authorization: Bearer <accessToken>` on authenticated routes.

### Public (no JWT)

| Method | Path |
| --- | --- |
| POST | `/auth/login` |
| POST | `/auth/signup` |
| POST | `/auth/refresh` |
| POST | `/auth/password-reset/request` |
| POST | `/auth/password-reset/confirm` |
| POST | `/auth/email-verification/confirm` |
| GET | `/web-push/vapid-public-key` |

`POST /auth/email-verification/resend`, `GET /auth/me`, and `POST /auth/logout`
require a valid access token.

### Token lifecycle

`POST /auth/login` body: `{ "email", "password" }`. Response includes
`accessToken`, `refreshToken`, `tokenType`, `userId`, `name`, `email`, `role`.

`POST /auth/refresh` body: `{ "refreshToken" }`. The previous refresh token is
invalidated. Access tokens last `jwt.expiration` (default 24h). Refresh tokens
last `auth.refresh-expiration-days` (default 7 days).

`POST /auth/logout` requires the access token header and `{ "refreshToken" }`.
The access token JTI is revoked; the refresh token is revoked if it belongs to
the caller.

Password reset: `POST /auth/password-reset/request` with `{ "email" }` always
returns `204`. Token lifetime is `auth.password-reset-expiration-minutes`
(default **10** minutes). Confirm with `{ "token", "newPassword" }`
(`newPassword` min length 8). That invalidates the user’s refresh tokens.

Email verification: registrations get a token valid
`auth.email-verification-expiration-hours` (default 24h). Confirm with
`{ "token" }`. Signed-in unverified users can `POST /auth/email-verification/resend`.

Login is rate-limited by email and client IP (`auth.login.max-attempts` /
`auth.login.window-minutes`, default 5 / 15). Set
`app.security.trust-forwarded-headers=true` only behind a proxy that sets
`X-Forwarded-For`.

SSE: `GET /realtime/projects/{projectId}` and `GET /realtime/notifications`
accept `Authorization: Bearer` **or** `?access_token=`. Timeout is 30 minutes.

## Errors

JSON: `timestamp`, `status`, `error`, `message`. Validation failures add
`errors` keyed by field. Typical statuses: `401` unauthenticated, `403`
forbidden, `404` missing, `409` conflict.

## Auth

| Method | Path |
| --- | --- |
| POST | `/auth/login` |
| POST | `/auth/signup` `{ name, email, password }` |
| POST | `/auth/refresh` |
| POST | `/auth/logout` |
| GET | `/auth/me` |
| POST | `/auth/password-reset/request` |
| POST | `/auth/password-reset/confirm` |
| POST | `/auth/email-verification/confirm` |
| POST | `/auth/email-verification/resend` |

## Workspaces

| Method | Path |
| --- | --- |
| GET, POST | `/workspaces` |
| GET, PUT | `/workspaces/{id}` |
| GET, POST | `/workspaces/{id}/members` |
| PUT, DELETE | `/workspaces/{id}/members/{userId}` |
| POST | `/workspaces/{id}/ownership/{newOwnerId}` |

## Projects

| Method | Path |
| --- | --- |
| GET | `/projects`, `/projects/active`, `/projects/{id}` |
| GET | `/projects/owner/{ownerId}`, `/projects/owner/{ownerId}/active` |
| GET | `/projects/status/{statusId}`, `/projects/status/{statusId}/active` |
| GET | `/projects/name/{name}` |
| POST | `/projects` |
| PUT | `/projects/{id}`, `/projects/{id}/restore` |
| DELETE | `/projects/{id}`, `/projects/{id}/permanent` |
| GET | `/projects/{projectId}/analytics` |
| GET | `/projects/{projectId}/audit-events` |
| GET, POST | `/projects/{projectId}/ticket-views` |
| PUT, DELETE | `/projects/{projectId}/ticket-views/{id}` |

Statuses: `/project-statuses` (CRUD, `/active`, `/default`, `/name/{name}`, restore, permanent delete).

Members: `/project-users` (CRUD, by project/user/role, `/check`, counts, bulk delete by project or user).

Favorites: `/project-favorites`.

## Tickets

| Method | Path |
| --- | --- |
| GET | `/tickets`, `/tickets/active`, `/tickets/deleted`, `/tickets/my-tasks` |
| GET | `/tickets/code/{code}`, `/tickets/{id}` |
| GET | `/tickets/project/{projectId}`, `.../active`, `.../root`, `.../board` |
| GET | `/tickets/{id}/children` |
| GET | `/tickets/owner/{ownerId}`, `/tickets/responsible/{responsibleId}` |
| GET | `/tickets/status/{statusId}`, `/tickets/type/{typeId}`, `/tickets/priority/{priorityId}` |
| GET | `/tickets/epic/{epicId}` |
| GET | `/tickets/search?keyword=` |
| GET | `/tickets/project/{projectId}/search?keyword=` |
| GET | `/tickets/project/{projectId}/filter` |
| POST | `/tickets` |
| PUT | `/tickets/{id}` |
| PUT | `/tickets/{id}/transition` `{ statusId }` |
| PUT | `/tickets/project/{projectId}/plan` |
| PUT | `/tickets/{id}/restore` |
| DELETE | `/tickets/{id}`, `/tickets/{id}/permanent` |
| GET | `/tickets/{ticketId}/audit-events` |

There are **no** `/tickets/{id}/comments`, `/hours`, or `/attachments` nested
routes. Use the resources below.

Comments `/ticket-comments`: list, `/active`, `/{id}`, `/ticket/{ticketId}`,
`/user/{userId}`, counts, POST `{ ticketId, content }`, PUT, soft delete,
restore, permanent delete, delete by ticket or user.

Hours `/ticket-hours`: CRUD plus filters by ticket/user/activity. POST body:
`{ ticketId, userId, value, comment?, activityId? }`.

Attachments `/ticket-attachments`: `GET/POST /ticket/{ticketId}` (multipart
field `file`), `GET /{id}/download`, `GET /{id}/view`, `DELETE /{id}`.

Also: `/ticket-statuses`, `/ticket-types`, `/ticket-priorities`,
`/ticket-activities`, `/ticket-relations`, `/ticket-subscribers`.

## Sprints

| Method | Path |
| --- | --- |
| GET, POST | `/sprints` |
| GET, PUT, DELETE | `/sprints/{id}` |
| GET | `/sprints/project/{projectId}` |
| POST | `/sprints/{id}/start`, `/complete`, `/complete-with-carry-over`, `/cancel` |
| GET | `/sprints/{id}/burndown`, `/tickets`, `/statistics` |
| GET | `/sprints/project/{projectId}/backlog` |
| POST, DELETE | `/sprints/{sprintId}/tickets/{ticketId}` |
| POST | `/sprints/tickets/{ticketId}/backlog` |

## Labels and milestones

Labels (all under `/projects/{projectId}`):

- `GET, POST /labels`
- `GET, PUT, DELETE /labels/{labelId}`
- `POST, DELETE /tickets/{ticketId}/labels/{labelId}`
- `GET /tickets/{ticketId}/labels`
- `GET /labels/{labelId}/tickets`

Milestones (all under `/projects/{projectId}`):

- `GET, POST /milestones`
- `GET, PUT, DELETE /milestones/{milestoneId}`
- `PATCH /milestones/{milestoneId}/status?status=`
- `PATCH /milestones/{milestoneId}/progress?progress=`
- `POST, DELETE /milestones/{milestoneId}/tickets/{ticketId}`
- `GET /milestones/{milestoneId}/tickets`, `.../statistics`

## Epics, daily scrum, timesheets

Epics `/epics`: list/active/by project/root/children, CRUD, restore, permanent
delete, counts.

Daily scrums `/daily-scrums`: CRUD, `/user/{userId}`, `/user/{userId}/range`,
`/range`, `/project/{projectId}/range`. There is **no** `/daily-scrums/me`.
Body: `{ userId, projectId, scrumDate, yesterdayWork, todayWork, blockers? }`.

Time sheets `/time-sheets`: CRUD, active, by user/project, search `?task=`,
restore, permanent delete.

Cells `/time-sheet-cells`: CRUD, by time sheet, date, trips, counts.

## Users, roles, permissions

| Resource | Path |
| --- | --- |
| Users | `/users` (CRUD, `/email/{email}`, `/exists/email/{email}`) |
| Roles | `/roles` |
| Permissions | `/permissions` |
| Role–permission | `/role-permissions` |
| User–role | `/user-roles` |
| Activities (hour types) | `/activities` |
| Settings | `/settings` |
| Documents | `/documents` (multipart `name` + `file`) |
| Application metadata | `/application-metadata` |
| Developer dashboard | `GET /dashboard/developer` |

## Notifications and push

`/notifications`: list, `/{id}`, `/user` (query `notifiableType`,
`notifiableId`), unread/read, counts, POST, `PUT /{id}/read`, `PUT /{id}/unread`,
DELETE, `DELETE /user`.

Web push (when `app.notification.web-push.enabled=true`):

- `GET /web-push/vapid-public-key` → `{ "publicKey" }`
- `POST /web-push-subscriptions` `{ endpoint, p256dh, auth }`
- `DELETE /web-push-subscriptions?endpoint=`

## Realtime

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/realtime/projects/{projectId}` | `text/event-stream` |
| GET | `/realtime/notifications` | current user only |

## Production / mail / push

Override properties with env vars (Spring relaxed binding). Useful ones:

| Property | Env |
| --- | --- |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `jwt.secret` | `JWT_SECRET` (base64, ≥ 256-bit) |
| `jwt.issuer` | `JWT_ISSUER` |
| `app.cors.allowed-origins` | `APP_CORS_ALLOWED_ORIGINS` |
| `app.mail.enabled` | `APP_MAIL_ENABLED` |
| `app.mail.from` | `APP_MAIL_FROM` |
| `app.frontend-url` | `APP_FRONTEND_URL` |
| `app.notification.email.enabled` | `APP_NOTIFICATION_EMAIL_ENABLED` |
| `app.notification.web-push.enabled` | `APP_NOTIFICATION_WEB_PUSH_ENABLED` |
| `app.security.trust-forwarded-headers` | `APP_SECURITY_TRUST_FORWARDED_HEADERS` |

SMTP uses Spring Mail (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`,
`SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`) when `app.mail.enabled=true`.

Ticket emails also need `app.notification.email.enabled=true` and are sent after
the ticket transaction commits.

Do not use the development JWT secret in production.
