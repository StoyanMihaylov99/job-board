# Job Board

Personal job-application tracker. Spring Boot 4.2 (Java 21) REST API backed by PostgreSQL 18 in Docker,
with an Angular 22 frontend in `frontend/`. Per-user authentication will be added in a later phase.

## Prerequisites
- Java 21
- Docker (running)
- Node.js 22+ (for the frontend)

## Run
```bash
./gradlew bootRun
```
Spring Boot's Docker Compose support starts the `postgres` service from `compose.yaml` automatically
(host port **5433**, so it doesn't clash with a locally installed Postgres on 5432) and Flyway applies the
migrations in `src/main/resources/db/migration`.

To manage the database yourself: `docker compose up -d` / `docker compose down` (add `-v` to wipe data).
Datasource settings can be overridden with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

- API base: `http://localhost:8080/api/v1/applications`
- Swagger UI: `http://localhost:8080/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`
- Sample requests: `http/applications.http` (IntelliJ HTTP client)

## Frontend
```bash
cd frontend
npm install
npm start          # http://localhost:4200, proxies /api to the backend on :8080
npm test           # unit tests (Vitest)
npm run build      # production build in frontend/dist
```
Start the backend first (`./gradlew bootRun`). The dev server proxies `/api` calls via `proxy.conf.json`, so no CORS
setup is needed in development.

Stack: standalone components, signals, `httpResource` for data loading, Signal Forms for the add/edit form,
native `<dialog>` for modals. Design tokens (colours, type scale, spacing) live in `frontend/src/styles.css`.

## Test
```bash
./gradlew test
```
Repository and integration tests spin up a throwaway Postgres via Testcontainers (Docker required).

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/applications` | Create → `201` + `Location` |
| `GET` | `/api/v1/applications` | Paged list. Filters: `status` (repeatable), `company`, `q` (title/company/position), `appliedFrom`, `appliedTo`. Paging: `page`, `size` (max 100), `sort` (default `createdAt,desc`) |
| `GET` | `/api/v1/applications/{id}` | Get one |
| `PUT` | `/api/v1/applications/{id}` | Full replace (omitted optional fields are cleared; omitted `status` is kept) |
| `PATCH` | `/api/v1/applications/{id}/status` | Change status only: `{"status": "OFFER"}` |
| `DELETE` | `/api/v1/applications/{id}` | Delete → `204` |
| `GET` | `/api/v1/applications/stats` | `{ total, byStatus: { WISHLIST: n, ... } }` |

**Statuses:** `WISHLIST, APPLIED, INTERVIEWING, OFFER, ACCEPTED, REJECTED, WITHDRAWN, GHOSTED`
(default `APPLIED`). Any status other than `WISHLIST` gets `appliedDate = today` if none was provided.

**Work modes:** `ONSITE, HYBRID, REMOTE`

Errors follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457); validation errors include an
`errors` map of `field → message`.
