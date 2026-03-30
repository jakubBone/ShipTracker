# ShipTracker – Application for Managing Ships and Voyage Reporting

---

# 1. Problem

Companies in the maritime transport industry need a tool to:
- Manage their fleet of ships (adding, editing vessel data)
- Track the position/voyage history of each ship
- View a clear chronological timeline of routes

---

# 2. User

Fleet operator / dispatcher in a maritime transport company:
- Adds ships to the system and manages their data
- Logs the current or historical position of a ship
- Reviews the voyage history of a given vessel

---

# 3. Functional Scope

## Module 1: Authentication
- Login screen (username + password)
- System access only after logging in
- One test account provided via Liquibase seed data

## Module 2: Ship Management
- List of all ships with actions: Add / Edit / Go to details
- Ship form:
  - Name (text, required)
  - Launch date (date, required)
  - Ship type (text, required)
  - Tonnage (number, required, > 0)
  - "Generate Name" button → calls backend endpoint → randommer.io API

## Module 3: Voyage Reporting
- Adding a location entry from the ship detail view
- Entry form:
  - Date (date, required)
  - Country (select from dictionary — static list)
  - Port (text, required)
- Entries are immutable — no editing after saving

## Module 4: Timeline
- Ship detail view contains a timeline
- Entries sorted chronologically
- Visual style: timeline with dots and data cards

---

# 4. Technical Requirements

| Component | Technology |
|-----------|------------|
| Backend | Spring Boot 3.4.x, Java 21 |
| Database | PostgreSQL 16 in Docker |
| Migrations & seed | Liquibase |
| Frontend | Angular (latest stable) |
| Containerisation | Docker Compose — postgres + backend + frontend |
| Repository | Public GitHub repository |

### Java 21 — Approach Used

- DTOs as **Java Records** (immutable, concise) — replace classes with getters/setters/constructors; no Lombok
- **`Optional` + `.orElseThrow()`** — idiomatic handling of missing resources (404) instead of null checks
- **`stream().toList()`** (Java 16+) — immutable list as the result of mapping entities to DTOs
- **`RestClient`** (Spring 6 / Boot 3.2+) — modern HTTP API instead of the deprecated `RestTemplate`
- **Pattern matching, switch expressions, text blocks** — not applied; the code is too simple for their use to feel natural rather than forced

### Angular 17+ — Approach Used

- **Standalone Components** — no `NgModule`; each component declares its own dependencies (analogous to no Lombok — less magic, more explicitness)
- **`inject()` function** — dependency injection without a constructor; more readable in functional guards and interceptors
- **`@for`, `@if`, `@switch`** — new control flow syntax (Angular 17+) instead of `*ngFor`, `*ngIf` directives
- **Signals** — reactive state (`signal()`, `computed()`) instead of `BehaviorSubject` for simple state (e.g. isLoggedIn)
- **Strictly Typed Reactive Forms** — `FormGroup<{ name: FormControl<string> }>` instead of the non-generic `FormGroup`; type errors caught at compile time
- **`readonly` on interfaces** — data models immutable by convention (analogous to Java Records)
- **Functional Guards** — `CanActivateFn` instead of classes implementing an interface

### Frontend Security (OWASP)

- **No `localStorage`** for session storage — session held exclusively in an HttpOnly cookie managed by Spring Security
- **No `[innerHTML]` or `bypassSecurityTrust*`** — Angular automatically escapes interpolated values; we do not bypass this mechanism
- **401 Interceptor** — session expiry automatically redirects to `/login`
- **Disable submit during request** — prevents multiple form submissions

---

# 5. External API

**Randommer.io — ship name generation**
- API key stored in the `.env` file in the project root (not in code)
- `.env` file is in `.gitignore` — the key does not reach the repository
- Docker Compose reads the key from `.env` and passes it to the backend container as an environment variable
- Spring Boot reads it via `${RANDOMMER_API_KEY}` in `application.properties`
- Backend exposes the endpoint `/api/ships/generate-name` → calls randommer.io
- Frontend calls only the backend (API key is not visible on the client side)
- Error handling: when the API does not respond → 503 with a message

**Country dictionary**
- Static list in Angular service (`countries.data.ts`)
- No dependency on an external API

---

# 6. Business Rules and Edge Cases

## Business Rules
- Location entries are **immutable** — no PUT/PATCH endpoints for `location_reports`; they cannot be edited after saving
- A ship can have many location entries (a `@OneToMany` relationship); entries are displayed chronologically
- Validation required on the backend (Bean Validation) and frontend (Reactive Forms)

## Edge Cases — Error Handling

| Scenario | Expected Behaviour |
|---|---|
| Request to `/api/**` without an active session | `401 Unauthorized` |
| Login with incorrect credentials | `401 Unauthorized` |
| Creating/editing a ship with empty or invalid fields | `400 Bad Request` + field error map |
| Tonnage equal to zero or negative | `400 Bad Request` |
| Requesting details of a non-existent ship (`GET /api/ships/{id}`) | `404 Not Found` |
| Editing a non-existent ship (`PUT /api/ships/{id}`) | `404 Not Found` |
| Adding a report to a non-existent ship | `404 Not Found` |
| External API randommer.io unavailable | `503 Service Unavailable` + message; form remains active |

---

# 7. Seed Data (Liquibase)

| Type | Content |
|------|---------|
| Users | 1 account: `admin` / `admin123` (BCrypt hash) |
| Ships | 4–5 ships of different types (Cargo, Tanker, Container, Bulk Carrier) |
| Location entries | 3–5 entries per ship (various countries and dates — for timeline verification) |

---

# 8. Project Structure

## Backend (`ship-tracker-backend/`)
```
src/main/java/com/shiptracker/
├── config/
│   ├── SecurityConfig.java          # CORS, session, CSRF, AuthenticationManager, UserDetailsService
│   ├── AppConfig.java               # Bean RestClient (HTTP client for NameGeneratorService)
│   └── OpenApiConfig.java           # Swagger/OpenAPI — title, version, cookie session scheme
├── controller/
│   ├── AuthController.java
│   ├── ShipController.java
│   └── LocationReportController.java
├── service/
│   ├── AuthService.java             # Login and logout logic (SRP)
│   ├── ShipService.java
│   ├── LocationReportService.java
│   └── NameGeneratorService.java
├── repository/
│   ├── UserRepository.java
│   ├── ShipRepository.java
│   └── LocationReportRepository.java
├── entity/
│   ├── User.java
│   ├── Ship.java
│   └── LocationReport.java
├── dto/
│   ├── LoginRequest.java            # record
│   ├── LoginResponse.java           # record
│   ├── UserResponse.java            # record
│   ├── GeneratedNameResponse.java   # record
│   ├── ShipRequest.java             # record
│   ├── ShipResponse.java            # record
│   ├── LocationReportRequest.java   # record
│   └── LocationReportResponse.java  # record
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── ExternalApiException.java

src/main/resources/
├── application.properties
└── db/changelog/
    ├── db.changelog-master.xml
    ├── 001-create-tables.xml
    └── 002-seed-data.xml
```

## Frontend (`ship-tracker-frontend/`)
```
src/app/
├── core/
│   ├── models/
│   │   ├── ship.model.ts
│   │   └── location-report.model.ts
│   ├── services/
│   │   ├── auth.service.ts
│   │   ├── ship.service.ts
│   │   └── location-report.service.ts
│   ├── guards/
│   │   └── auth.guard.ts
│   └── data/
│       └── countries.data.ts
├── features/
│   ├── auth/login/
│   ├── ships/ship-list/
│   ├── ships/ship-form/
│   ├── ships/ship-detail/
│   └── location-reports/location-report-form/
└── shared/components/timeline/
```

## Docker

All three services started with a single command: `docker compose up`

Randommer.io API key stored in the `.env` file (outside the repository):
```
RANDOMMER_API_KEY=your_api_key
```

### `docker-compose.yml`
```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: shiptracker_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    build: ./ship-tracker-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/shiptracker_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      RANDOMMER_API_KEY: ${RANDOMMER_API_KEY}
    depends_on:
      postgres:
        condition: service_healthy

  frontend:
    build: ./ship-tracker-frontend
    ports:
      - "4200:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

### `ship-tracker-backend/Dockerfile`
Multi-stage build: JDK for compilation → JRE for runtime (smaller final image).

### `ship-tracker-frontend/Dockerfile`
Multi-stage build: Node.js for `ng build` → nginx for serving static files.

---

# 9. REST API

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | /api/auth/login | - | Login |
| POST | /api/auth/logout | YES | Logout |
| GET | /api/ships | YES | List ships |
| POST | /api/ships | YES | Add ship |
| GET | /api/ships/{id} | YES | Ship details |
| PUT | /api/ships/{id} | YES | Edit ship |
| GET | /api/ships/generate-name | YES | Random name (proxy to randommer.io) |
| GET | /api/ships/{id}/reports | YES | Location history |
| POST | /api/ships/{id}/reports | YES | Add location entry |

---

# 10. Security

- Spring Security — session-based authentication
- Password hashed with BCrypt
- All `/api/**` endpoints (except `/api/auth/login`) require a session
- CORS allows `http://localhost:4200`
- CSRF disabled (REST API, not an HTML form)
- Frontend sends requests with `withCredentials: true`
