# Job Board

Personal job-application tracker. Spring Boot 4.2 (Java 21) REST API backed by PostgreSQL 18 in Docker,
with an Angular 22 frontend in `frontend/`. Each user registers, signs in and sees only their own applications.

## Prerequisites
- Java 21
- Docker (running)
- Node.js 22+ (for the frontend)

## Run everything with Docker
```bash
docker compose up -d --build     # first run builds the images (~1 min)
open http://localhost:8088
docker compose logs -f backend   # follow backend logs
docker compose down              # stop (data is kept in the postgres-data volume)
docker compose down -v           # stop and wipe the database
```

| Service | Image | Reachable at |
|---|---|---|
| `frontend` | nginx serving the Angular build, proxying `/api`, `/swagger-ui`, `/v3/api-docs` to the backend | http://localhost:8088 |
| `backend` | Spring Boot (JRE 21, non-root) | internal only, via the frontend proxy |
| `postgres` | PostgreSQL 18 | `localhost:5433` (user/db `jobboard`) |

Startup is ordered by health checks: postgres → backend (`/actuator/health/readiness`) → frontend.
Optional settings go in a `.env` file next to `compose.yaml`; see `.env.example` (set `JWT_SECRET` there so
sessions survive backend restarts without a token refresh). After changing code, rebuild with `docker compose up -d --build`.

## Run for development
```bash
./gradlew bootRun
```
Spring Boot's Docker Compose support starts only the `postgres` service from `compose.yaml`
(host port **5433**, so it doesn't clash with a locally installed Postgres on 5432) and Flyway applies the
migrations in `src/main/resources/db/migration`.

To manage the database yourself: `docker compose up -d` / `docker compose down` (add `-v` to wipe data).
Datasource settings can be overridden with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | random per run | Base64 HMAC key (≥ 32 bytes) for signing access tokens. **Set it** anywhere but local dev: `openssl rand -base64 32` |
| `REFRESH_COOKIE_SECURE` | `true` | `Secure` flag on the refresh cookie. Chrome/Firefox accept it on `http://localhost`; set `false` only if your dev browser drops it |

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

## Authentication

- `POST /api/v1/auth/register` `{email, password, displayName}` → `201`, `POST /api/v1/auth/login` `{email, password}` → `200`.
  Both return `{accessToken, tokenType: "Bearer", expiresIn, user}` and set the refresh cookie.
- Send `Authorization: Bearer <accessToken>` on every other `/api/**` call. Access tokens are HS256 JWTs valid for 15 minutes.
- `POST /api/v1/auth/refresh` exchanges the refresh cookie for a new access token **and a new refresh cookie** (rotation).
  Re-using an already-rotated refresh token signs that user out everywhere (theft detection), except within 10 seconds,
  which covers two tabs refreshing at once.
- `POST /api/v1/auth/logout` revokes the refresh token and clears the cookie.
- The refresh token is 256 random bits, stored only as a SHA-256 hash, and lives in an `HttpOnly; Secure; SameSite=Strict`
  cookie scoped to `/api/v1/auth` for 14 days. JavaScript never sees it; the frontend keeps the access token in memory only.
- Profile: `GET /api/v1/me`, `PATCH /api/v1/me` `{displayName}`, `PUT /api/v1/me/password` `{currentPassword, newPassword}`
  (signs out all other sessions and returns a fresh one).
- Passwords: 8+ characters, at most 72 bytes (BCrypt's limit), hashed with BCrypt. Wrong password and unknown email return
  the same `401`.
- Another user's application id returns `404`, never `403`, so ids can't be probed.

In Swagger UI, call `/api/v1/auth/login`, copy `accessToken`, then use **Authorize**.

## API

All endpoints below require a bearer token and only see the caller's own applications.

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
