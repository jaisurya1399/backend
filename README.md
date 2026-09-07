# Project Management Backend

Spring Boot 3 (Java 21) REST API for project/ticket/sprint management, with JWT auth,
PostgreSQL + Flyway migrations.

## Requirements

- JDK 21
- Maven 3.9+
- PostgreSQL 14+

## Configuration

All environment-specific and secret values are read from environment variables
(with dev-friendly local defaults baked in so it still runs out of the box):

| Variable                 | Default                                       | Purpose                                                          |
| ------------------------ | --------------------------------------------- | ---------------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE` | `dev`                                         | `dev` or `prod`                                                  |
| `SERVER_PORT`            | `8080`                                        | HTTP port                                                        |
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/pmt`        | Postgres JDBC URL                                                |
| `DB_USERNAME`            | `postgres`                                    | DB user                                                          |
| `DB_PASSWORD`            | `postgres`                                    | DB password                                                      |
| `JWT_SECRET`             | (dev placeholder in `application.properties`) | HMAC signing key — **must** be overridden in any real deployment |
| `JWT_EXPIRATION`         | `86400000` (24h, ms)                          | Token lifetime                                                   |

**Never deploy with the default `JWT_SECRET` or `DB_PASSWORD`.** Set real values via
env vars / your secrets manager.

## Running locally

```bash
createdb pmt
mvn spring-boot:run
```

Flyway will run all migrations in `src/main/resources/db/migration` on startup and
seed a default admin user:

- email: `admin@example.com`
- password: `Admin@123`

**Change this password immediately** if you deploy this anywhere beyond your own machine.

## Workspace API

Projects now belong to a workspace. Run Flyway before using the API; existing data is
moved to a `default` workspace. Create a workspace with `POST /api/workspaces`, then
include its `workspaceId` when creating or updating a project. Workspace owners/admins
can manage members at `/api/workspaces/{workspaceId}/members`; ownership can be
transferred with `POST /api/workspaces/{workspaceId}/ownership/{newOwnerId}`.

Project members use one of three roles: `ADMIN`, `MEMBER`, or `VIEWER`. A `VIEWER`
can read project issues, a `MEMBER` can create and edit issues, and an `ADMIN` (or
the project owner) manages project settings, memberships, and destructive actions.

Issues can be nested as sub-tasks by passing `parentId` in the ticket payload. Use
`GET /api/tickets/project/{projectId}/root` for root issues and
`GET /api/tickets/{ticketId}/children` for direct sub-tasks.

Ticket statuses have categories: `BACKLOG`, `TODO`, `IN_PROGRESS`, `DONE`, and
`CANCELLED`. Use `PUT /api/tickets/{ticketId}/transition` with a `statusId` to
perform a tracked status change. `GET /api/tickets/project/{projectId}/board`
returns active issues grouped into ordered Kanban columns.

For atomic drag/drop and backlog grooming, call
`PUT /api/tickets/project/{projectId}/plan` with an ordered `ticketIds` list.
Optionally provide `statusId` to move a board column, `sprintId` to plan a sprint,
or `moveToBacklog: true` to remove the issues from their sprint.

Use `GET /api/tickets/project/{projectId}/filter` for paginated issue search. It
accepts `q`, `statusId`, `priorityId`, `responsibleId`, `sprintId`, `epicId`,
`labelId`, `rootOnly`, `page`, `size`, `sort`, and `direction`. Personal saved
filters are available under `/api/projects/{projectId}/ticket-views`.

Comments now use the authenticated user as author (the deprecated `userId` request
field is ignored). Authors are auto-subscribed; ticket assignees, subscribers, and
valid `@email@example.com` mentions receive in-app notifications.

Project reporting is available at `GET /api/projects/{projectId}/analytics`. It
returns issue health, estimate completion, assignee workload, average lead/cycle
time in hours, and completed-sprint velocity.

See [API.md](API.md) for the endpoint index, authentication convention, errors,
and production environment variables.

## Building

```bash
mvn clean package
java -jar target/project-management-backend.jar
```

## Notes on this version

This copy was cleaned up from an earlier drop that was missing a build file and had a
few rough edges:

- Added `pom.xml` (Spring Boot 3.3.4, Java 21, jjwt 0.12.6, PostgreSQL, Flyway, Lombok) —
  the project previously had no build descriptor at all.
- Removed a leftover `security/` package containing three empty, unused duplicate
  files (`JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`) that
  shadowed the real, working versions in `auth/`.
- Removed a debug `System.out.println` in `main()` that logged a BCrypt hash on
  every startup.
- Split logging/error-detail configuration properly between `application-dev.properties`
  (verbose SQL/logging, full error detail) and `application-prod.properties` (quiet,
  no stack traces to clients) — previously both files were empty and everything was
  hardcoded on in the base `application.properties`, including in production.
- Moved the DB password and JWT secret to environment-variable overrides instead of
  being committed as plain values.
- Replaced a real personal email address and its default seed password in
  `V20__create_default_admin_user.sql` with a generic `admin@example.com` placeholder.
- Fixed two migration filename issues: `V24__create_sprints_and_ticket_sprint.sql.sql`
  → `.sql` (was double-extensioned), and `V22__Create_daily_scrums.sql` →
  `V22__create_daily_scrums.sql` (case consistency with the rest of the series).
- Added a minimal `ProjectManagementApplicationTests` smoke test and `test` profile.
- Removed five empty, unreferenced placeholder packages/folders (`common/util`,
  `common/exception`, `common/response`, `roadmap`, `timelog`) that contained no
  files and weren't imported anywhere — leftover scaffolding for features that
  were never built.
