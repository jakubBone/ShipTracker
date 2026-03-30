# ShipTracker — Implementation Plan (Step by Step)

---

## STAGE 0: Environment Setup

### What and Why
Before writing a single line of code, we need working tools. Like building a house — before pouring the foundation, check that the cement mixer is ready.

### Steps:

**0.1 Verify tools**
```bash
java --version        # requires Java 21+
mvn --version         # Maven for building the backend
node --version        # Node.js for Angular
npm --version
ng version            # Angular CLI (if missing: npm install -g @angular/cli)
docker --version      # Docker for the database
git --version
```

**0.2 Create the `.env` file with the API key**

In the project root (next to `docker-compose.yml`):
```
RANDOMMER_API_KEY=your_api_key
```
The `.env` file must be in `.gitignore` — the key must not reach the repository.

**0.3 Start the database (postgres only during development)**
```bash
docker compose up postgres -d

# Verify:
docker ps   # should show a postgres:16 container running on port 5432
```
During development, we only start postgres. The backend and frontend run locally (`./mvnw spring-boot:run`, `ng serve`) for convenience (hot-reload). Full `docker compose up` (all 3 services) is run only after completing the frontend in Stage 5.

**0.4 Create a GitHub repository**
- Create a new public repo on GitHub (e.g. `ship-tracker`)
- `git init`, `git remote add origin <URL>`
- Add `.gitignore` for Java + Node + `.env` file

### Definition of Done:
- `docker ps` shows a running postgres container
- `.env` file exists in the project root and is in `.gitignore`
- Java 21 and Maven available in the terminal
- Angular CLI available (`ng version`)
- Empty GitHub repo ready

---

## STAGE 1: Backend — Foundation (Spring Boot + Liquibase + Security)

### What and Why
The backend is the heart of the application. A frontend without a backend is just a static page. We start with the backend because:
1. It defines the data contract (what the API returns)
2. Angular will connect to it

### Steps:

**1.1 Generate the project via Spring Initializr**
- Go to: https://start.spring.io
- Set:
  - Project: Maven
  - Language: Java
  - Spring Boot: 3.4.x
  - Group: `com.shiptracker`
  - Artifact: `ship-tracker-backend`
  - Java: 21
- Add dependencies:
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - PostgreSQL Driver
  - Liquibase Migration
  - Validation

**1.2 Configure application.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/shiptracker_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
randommer.api.key=${RANDOMMER_API_KEY:demo-key}
```

`ddl-auto=validate` — Liquibase manages the schema, Hibernate only validates it at startup.

**1.3 Configure Liquibase**

`db.changelog-master.xml` — master migration list:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog" ...>
    <include file="classpath:db/changelog/001-create-tables.xml"/>
    <include file="classpath:db/changelog/002-seed-data.xml"/>
</databaseChangeLog>
```

`001-create-tables.xml` — creates tables:
- `users` (id BIGSERIAL PK, username VARCHAR UNIQUE NOT NULL, password VARCHAR NOT NULL, role VARCHAR NOT NULL)
- `ships` (id BIGSERIAL PK, name VARCHAR NOT NULL, launch_date DATE NOT NULL, ship_type VARCHAR NOT NULL, tonnage NUMERIC(12,2) NOT NULL)
- `location_reports` (id BIGSERIAL PK, ship_id BIGINT FK→ships, report_date DATE NOT NULL, country VARCHAR NOT NULL, port VARCHAR NOT NULL)

Liquibase instead of schema.sql — tracks migration history, allows adding new schema versions without destroying data. Standard practice in production projects.

`002-seed-data.xml` — test data:
- 1 user `admin` (password hashed with BCrypt)
- 4 ships of different types (Cargo, Tanker, Container, Bulk Carrier)
- 3–5 location entries per ship

**1.4 Create entities (Entity)**

`User.java`:
- `id` (Long, @GeneratedValue)
- `username` (String, @Column(unique=true))
- `password` (String — BCrypt hashed)
- `role` (String — "ROLE_USER")

`Ship.java`:
- `id` (Long)
- `name` (String, `@Column(nullable = false)`)
- `launchDate` (LocalDate, `@Column(nullable = false)`)
- `shipType` (String, `@Column(nullable = false)`)
- `tonnage` (BigDecimal, `@Column(nullable = false, precision=12, scale=2)`)
- `locationReports` (`@OneToMany`, mappedBy="ship", cascade=ALL, orphanRemoval=true)

`LocationReport.java`:
- `id` (Long)
- `ship` (`@ManyToOne(fetch = LAZY)`, `@JoinColumn`)
- `reportDate` (LocalDate, `@Column(nullable = false)`)
- `country` (String, `@Column(nullable = false)`)
- `port` (String, `@Column(nullable = false)`)

> **Note:** Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Positive`) belong to the DTO layer (`ShipRequest`, `LocationReportRequest`), not to entities. Entities are protected by `nullable = false` at the database level. Input validation happens in the controller via `@Valid @RequestBody`.

LocationReport is immutable by design — no updatedAt fields, no PUT/PATCH endpoint.

**1.5 Create repositories**
```java
ShipRepository extends JpaRepository<Ship, Long>

LocationReportRepository extends JpaRepository<LocationReport, Long>
    // + findByShipIdOrderByReportDateAsc(Long shipId)

UserRepository extends JpaRepository<User, Long>
    // + Optional<User> findByUsername(String username)
```

**1.6 DTOs as Java Records (Java 21)**

In Java 21, DTOs are written as `record` instead of classes with getters/setters. A record is immutable and concise — the compiler automatically generates the constructor, getters, equals, hashCode, and toString. No need for Lombok.

```java
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}

public record ShipRequest(
    @NotBlank String name,
    @NotNull LocalDate launchDate,
    @NotBlank String shipType,
    @NotNull @Positive BigDecimal tonnage
) {}

public record ShipResponse(
    Long id,
    String name,
    LocalDate launchDate,
    String shipType,
    BigDecimal tonnage,
    int reportCount
) {}

public record LocationReportRequest(
    @NotNull LocalDate reportDate,
    @NotBlank String country,
    @NotBlank String port
) {}

public record LocationReportResponse(
    Long id,
    LocalDate reportDate,
    String country,
    String port
) {}
```

**1.7 Create services (Service)**

`AuthService`:
- `login(String username, String password, HttpSession session)` — authentication via `AuthenticationManager`, set `SecurityContextHolder`, save session
- `logout(HttpSession session, HttpServletResponse response)` — invalidate session, clear context, remove cookie

Extracted from `AuthController` — the controller is only responsible for HTTP, session logic belongs to the service (SRP).

`ShipService`:
- `findAll()` → `List<ShipResponse>`
- `findById(Long id)` → `ShipResponse` (throws ResourceNotFoundException if missing)
- `create(ShipRequest dto)` → `ShipResponse`
- `update(Long id, ShipRequest dto)` → `ShipResponse`

`LocationReportService`:
- `findByShipId(Long shipId)` → `List<LocationReportResponse>` sorted by date
- `create(Long shipId, LocationReportRequest dto)` → `LocationReportResponse`

`NameGeneratorService`:
- `generateName()` → `String` (calls randommer.io via RestClient — the new Spring 6 API)
- API key injected from `application.properties` via `@Value`
- Exception handling when the API is unavailable → throws an exception with a message

**1.8 Create controllers (Controller)**

```
GET  /api/ships              → ShipController.getAll()             → 200 List<ShipResponse>
POST /api/ships              → ShipController.create(...)          → 201 ShipResponse
GET  /api/ships/{id}         → ShipController.getById()            → 200 ShipResponse
PUT  /api/ships/{id}         → ShipController.update(...)          → 200 ShipResponse
GET  /api/ships/generate-name → ShipController.generateName()      → 200 GeneratedNameResponse
GET  /api/ships/{id}/reports → LocationReportController.getByShip() → 200 List<LocationReportResponse>
POST /api/ships/{id}/reports → LocationReportController.create(...) → 201 LocationReportResponse
POST /api/auth/login         → AuthController.login()              → 200 LoginResponse
POST /api/auth/logout        → AuthController.logout(HttpSession)  → 200 LoginResponse
GET  /api/auth/me            → AuthController.me()                 → 200 UserResponse
```

**1.9 Configure Spring Security**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // permitAll: POST /api/auth/login
    // authenticated: all other /api/**
    // session-based (SessionCreationPolicy.IF_REQUIRED)
    // CORS: allowedOrigins localhost:4200, allowCredentials true
    // CSRF: disabled (REST API)
    // 401 for unauthenticated users (not redirect to HTML login page)
}
```

Session-based instead of JWT — simpler at this scale. JWT is needed for microservices or when the frontend is hosted separately on a different domain from the backend.

**1.10 Global error handler**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String message, int status, Instant timestamp) {}

    // MethodArgumentNotValidException → 400 + Map<String, String> (field → error message)
    //   Note: this handler returns Map, not ErrorResponse — different structure from the rest
    // ResourceNotFoundException       → 404 + ErrorResponse
    // AuthenticationException         → 401 + ErrorResponse ("Invalid credentials")
    // ExternalApiException            → 503 + ErrorResponse
    // Exception (fallback)            → 500 + ErrorResponse ("Internal server error")
}
```

`ExternalApiException` has two constructors:
- `ExternalApiException(String message)` — general API error
- `ExternalApiException(String message, Throwable cause)` — used in `NameGeneratorService` on `RestClientException`, preserving the original stack trace

### Backend Tests:

```bash
./mvnw spring-boot:run

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt
# Expected: 200 OK, JSESSIONID cookie in cookies.txt

# Login — wrong password
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}'
# Expected: 401 Unauthorized

# No session → 401
curl http://localhost:8080/api/ships
# Expected: 401 Unauthorized

# List ships (requires session)
curl http://localhost:8080/api/ships -b cookies.txt
# Expected: JSON array with 4 ships from seed data

# Add a ship
curl -X POST http://localhost:8080/api/ships \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"name":"Black Pearl","launchDate":"2010-05-15","shipType":"Cargo","tonnage":50000}'
# Expected: 201 Created, JSON with id

# Validation — empty fields
curl -X POST http://localhost:8080/api/ships \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"name":""}'
# Expected: 400 Bad Request, JSON with field error map

# Validation — negative tonnage
curl -X POST http://localhost:8080/api/ships \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"name":"X","launchDate":"2010-01-01","shipType":"Cargo","tonnage":-1}'
# Expected: 400 Bad Request

# Non-existent ship
curl http://localhost:8080/api/ships/99999 -b cookies.txt
# Expected: 404 Not Found

# Add a location report
curl -X POST http://localhost:8080/api/ships/1/reports \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"reportDate":"2024-03-10","country":"Poland","port":"Gdańsk"}'
# Expected: 201 Created

# Add report to non-existent ship
curl -X POST http://localhost:8080/api/ships/99999/reports \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"reportDate":"2024-03-10","country":"Poland","port":"Gdańsk"}'
# Expected: 404 Not Found

# Generate name
curl http://localhost:8080/api/ships/generate-name -b cookies.txt
# Expected: 200 OK, {"name":"<random name>"}
# When API unavailable: 503 Service Unavailable
```

### Definition of Done:
- Liquibase creates tables and loads seed data at startup
- Login returns 200 + session cookie
- Invalid login credentials → 401
- GET /api/ships → list of 4 ships (from seed)
- POST /api/ships → creation with validation (400 on invalid fields)
- GET /api/ships/{id} → 404 for non-existent id
- GET /api/ships/{id}/reports → list sorted chronologically
- POST /api/ships/{id}/reports → 404 when ship does not exist
- 401 for requests without a session
- GET /api/ships/generate-name → random name or 503 when API is down

---

## STAGE 2: Frontend — Angular Setup + Login

### What and Why
We build the interface. We start with login — it is the gateway to the entire application. An unauthenticated user should see nothing but the login form.

### Steps:

**2.1 Generate the Angular project**
```bash
ng new ship-tracker-frontend --routing=true --style=scss
cd ship-tracker-frontend
ng serve   # should run at localhost:4200
```

**2.2 Install Angular Material**
```bash
ng add @angular/material
```

Angular Material is the official UI library for Angular. It provides ready-made components (forms, tables, cards, toolbar) — consistent appearance without writing CSS from scratch.

**2.3 Configure the environment**

`environments/environment.ts`:
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

**2.4 Configure HttpClient with interceptors**

`app.config.ts`:
```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([credentialsInterceptor, errorInterceptor])),
    provideRouter(routes)
  ]
};
```

Two interceptors:
- `credentialsInterceptor` — adds `{ withCredentials: true }` to every request; without this, the browser will not send the session cookie and Spring Security will reject every request as unauthenticated
- `errorInterceptor` — globally catches 401 errors and redirects to `/login` (session expiry)

**Security conventions (apply throughout the project):**
- Never use `localStorage` / `sessionStorage` to store sessions — the session lives exclusively in an HttpOnly cookie
- Never use `[innerHTML]` or `DomSanitizer.bypassSecurityTrust*` — Angular automatically escapes values
- Disable the submit button during an ongoing HTTP request (an `isLoading` flag)

**2.5 Create AuthService**
```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);       // inject() instead of constructor
  private readonly router = inject(Router);
  private readonly loggedIn = signal(false);        // Signal instead of BehaviorSubject

  login(username: string, password: string): Observable<void>
  logout(): Observable<void>
  isLoggedIn(): boolean
}
```

`signal` is the new reactivity mechanism in Angular 17+ — lighter and simpler than RxJS for simple login state. `inject()` replaces the constructor with parameters — more readable in services and guards.

**2.6 Create AuthGuard (functional guard)**
```typescript
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.createUrlTree(['/login']);
};
```

The guard stands in front of every protected route. If the user is not logged in — redirect to `/login`.

**2.7 Create LoginComponent**
- Reactive Form: username + password
- "Login" button
- 401 error handling → message "Invalid credentials"
- On success: redirect to `/ships`

**2.8 Configure routing (Stage 2 routes only)**

Routing is built incrementally — we only add routes for components that already exist. The `/ships` routes will be added in Stage 3.

```typescript
export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];
```

### How to Run for Testing

```bash
# Terminal 1 — database
docker compose up postgres -d

# Terminal 2 — backend
cd ship-tracker-backend
./mvnw spring-boot:run

# Terminal 3 — frontend
cd ship-tracker-frontend
ng serve
```

Open: `http://localhost:4200`

### Tests:

1. Open `http://localhost:4200` → redirect to `/login`
2. Enter wrong password → "Invalid credentials" message, no redirect
3. Enter `admin` / `admin123` → redirect to `/ships` (blank page — component will be created in Stage 3, redirect is correct)
4. "Login" button disabled during an ongoing request

### Definition of Done:
- `ng serve` runs without compilation errors
- Login page appears at `localhost:4200/login`
- Invalid credentials → error message visible below the form
- Valid credentials → redirect to `/ships` (route not yet rendered — that is Stage 3)

---

## STAGE 3: Frontend — Ship List + Form

### What and Why
The main application screen. The user will see their fleet here and be able to manage it.

### Steps:

**3.1 Create TypeScript models**
```typescript
// src/app/core/models/ship.model.ts
export interface Ship {
  id: number;
  name: string;
  launchDate: string;   // ISO format: "2010-05-15"
  shipType: string;
  tonnage: number;
  reportCount: number;
}

export interface ShipRequest {
  name: string;
  launchDate: string;
  shipType: string;
  tonnage: number;
}
```

**3.2 Create ShipService**
```typescript
@Injectable({ providedIn: 'root' })
export class ShipService {
  getAll(): Observable<Ship[]>
  getById(id: number): Observable<Ship>
  create(ship: ShipRequest): Observable<Ship>
  update(id: number, ship: ShipRequest): Observable<Ship>
  generateName(): Observable<{ name: string }>
}
```

**3.3 Create ShipListComponent**
- `mat-table` with columns: Name, Type, Tonnage, Launch Date, Actions
- Buttons: "Details", "Edit"
- "Add new ship" button above the table
- Data loaded from `/api/ships` on init (`ngOnInit`)

**3.4 Create ShipFormComponent (add + edit)**

One component for both actions — detects the mode based on the presence of `:id` in the URL. If id is present → edit mode (preload data), no id → add mode.

Form (Strictly Typed Reactive Forms):
```typescript
form = new FormGroup({
  name:       new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
  launchDate: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
  shipType:   new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
  tonnage:    new FormControl<number | null>(null, [Validators.required, Validators.min(1)])
});
```

`nonNullable: true` means `reset()` restores the default value instead of `null` — no type surprises. TypeScript catches type errors at compile time.
- "Generate ship name" button

**3.5 Implement the "Generate ship name" button**

```typescript
generateName(): void {
  this.shipService.generateName().subscribe({
    next: ({ name }) => this.shipForm.patchValue({ name }),
    error: () => this.nameError = 'Failed to fetch name. Please try again.'
  });
}
```

The backend calls randommer.io — the API key is secure on the server side, not visible in the browser.

**3.6 Form validation**
- Display errors below fields (e.g. "Field required")
- Disable submit if the form is invalid

**3.7 Add routes to the router**

Extend `app.routes.ts` with ship routes. The `/ships/:id` route (details) will be added in Stage 4.

```typescript
export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'ships', component: ShipList, canActivate: [authGuard] },
  { path: 'ships/new', component: ShipForm, canActivate: [authGuard] },
  { path: 'ships/:id/edit', component: ShipForm, canActivate: [authGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];
```

Note: `ships/new` must come before `ships/:id` (Stage 4) — Angular matches routes top-down, `new` would otherwise be treated as `:id`.

### How to Run for Testing

```bash
# Terminal 1 — database
docker compose up postgres -d

# Terminal 2 — backend
cd ship-tracker-backend
./mvnw spring-boot:run

# Terminal 3 — frontend
cd ship-tracker-frontend
ng serve
```

Open: `http://localhost:4200` → log in → ship list

### Tests:

1. Go to `localhost:4200` → log in → table with 4 ships from seed data
2. Direct URL `/ships` without login → redirect to `/login` (guard works)
3. Click "Add new ship" → empty form
4. Submit without filling in fields → validation errors below fields
5. Click "Generate name" → name field filled in
6. Fill in remaining fields → submit → redirect to list, new ship visible
7. Click "Edit" on a ship → form with ship data
8. Change name → submit → list updated

### Definition of Done:
- Ship list displays data from the database (seed data — 4 ships)
- Direct access to `/ships` without login → redirect to `/login`
- Add form works — new ship appears in the list
- Edit form works — data is saved
- "Generate name" button fills in the name field
- Validation displays errors below fields

---

## STAGE 4: Frontend — Ship Details + Adding Location Reports

### What and Why
This is the functional core of the task — voyage reporting. The ship detail view is where we see the full history and add new entries.

### Steps:

**4.1 Create TypeScript models**
```typescript
export interface LocationReport {
  id: number;
  reportDate: string;
  country: string;
  port: string;
}

export interface LocationReportRequest {
  reportDate: string;
  country: string;
  port: string;
}
```

**4.2 Create LocationReportService**
```typescript
@Injectable({ providedIn: 'root' })
export class LocationReportService {
  getByShipId(shipId: number): Observable<LocationReport[]>
  create(shipId: number, report: LocationReportRequest): Observable<LocationReport>
}
```

**4.3 Prepare the country dictionary**

```typescript
// src/app/core/data/countries.data.ts
export const COUNTRIES: string[] = [
  'Albania', 'Belgium', 'Bulgaria', 'Croatia', 'Cyprus',
  'Denmark', 'Estonia', 'Finland', 'France', 'Germany',
  'Greece', 'Latvia', 'Lithuania', 'Malta', 'Netherlands',
  'Norway', 'Poland', 'Portugal', 'Romania', 'Spain',
  'Sweden', 'Turkey', 'Ukraine', 'United Kingdom',
  // + full list
];
```

Static list instead of an external API — simpler, no dependencies.

**4.4 Create ShipDetailComponent**

Contains:
1. Ship data card (fetched from `ShipService.getById(id)`)
2. `LocationReportFormComponent` — form for adding an entry
3. `TimelineComponent` — voyage history

After adding a new entry → refresh the timeline (re-fetch or append to the list).

**4.5 Create LocationReportFormComponent**

Reactive Form:
- `reportDate` — `<input type="date">` or `<mat-datepicker>`
- `country` — `<mat-select>` with `COUNTRIES`
- `port` — `<input matInput>`
- Validators: required on all fields
- Submit → `LocationReportService.create()` → emits event to parent

**4.6 Add the detail route to the router**

Extend `app.routes.ts` with the last missing route. Must come after `ships/new` and `ships/:id/edit`:

```typescript
{ path: 'ships/:id', component: ShipDetail, canActivate: [authGuard] },
```

**4.7 Create TimelineComponent**

Input: `@Input() reports: LocationReport[]`

```html
<div class="timeline">
  @for (report of reports; track report.id) {
    <div class="timeline-item">
      <div class="timeline-marker"></div>
      <div class="timeline-content">
        <span class="timeline-date">{{ report.reportDate | date:'dd.MM.yyyy' }}</span>
        <span class="timeline-country">{{ report.country }}</span>
        <span class="timeline-port">Port: {{ report.port }}</span>
      </div>
    </div>
  }
</div>
```

`@for` is the new Angular 17+ syntax (control flow) instead of `*ngFor`. CSS timeline: vertical line on the left, dots at each entry, data cards.

### How to Run for Testing

```bash
# Terminal 1 — database
docker compose up postgres -d

# Terminal 2 — backend
cd ship-tracker-backend
./mvnw spring-boot:run

# Terminal 3 — frontend
cd ship-tracker-frontend
ng serve
```

Open: `http://localhost:4200` → log in → ship list → click "Details"

### Tests:

1. Click "Details" on the first ship from seed data → detail view with ship data card
2. Timeline displays location history from seed data (sorted chronologically)
3. Fill in the location form: date + country (select) + port → submit
4. New entry appears on the timeline without page reload
5. Refresh the page (F5) → entry still visible (saved in the database)
6. No edit buttons on timeline entries

### Definition of Done:
- Ship detail view displays ship data
- Timeline shows location history (from seed data) sorted chronologically
- Add location entry form works
- New entry appears on the timeline without page reload
- Form fields are validated (required)
- No ability to edit existing entries (display only)

---

## STAGE 5: Navigation, Error Handling, Containerisation, Finalisation

### What and Why
The application works — now we polish it: navigation, error handling, appearance. Finally, we containerise the frontend so the whole stack starts with a single command.

### Steps:

**5.1 Add navigation (toolbar)**
- `mat-toolbar` with the application name
- "Logout" button (top right) → `AuthService.logout()` → redirect to `/login`
- "Ship list" link

**5.2 HTTP Interceptor for global errors**
```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError(err => {
      if (err.status === 401) router.navigate(['/login']);
      return throwError(() => err);
    })
  );
};
```

If the session expires and the backend returns 401 — the user is automatically redirected to `/login`.

**5.3 Dockerfile for the frontend**

File `ship-tracker-frontend/Dockerfile`:
```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/ship-tracker-frontend/browser /usr/share/nginx/html
EXPOSE 80
```

Multi-stage build:
- Stage 1: Node.js builds the application (`ng build`) → static HTML/JS/CSS files in the `dist/` directory
- Stage 2: nginx serves those files on port 80 (mapped to 4200 in docker-compose)

Why nginx instead of `ng serve` in the container? `ng serve` is a development server with hot-reload — not suitable for "production". nginx is a lightweight HTTP server optimised for serving static files.

**5.4 Final requirements verification**

- [ ] Login screen — access only after logging in
- [ ] Ship list: Add / Edit / Details
- [ ] Ship form: 4 fields + name generation (randommer.io)
- [ ] Ship detail view
- [ ] Add location form: date + country (dictionary) + port
- [ ] Location entries are immutable (no editing)
- [ ] Timeline — chronologically sorted
- [ ] `docker compose up` starts postgres + backend + frontend
- [ ] `.env` file in `.gitignore`, API key not in the repository
- [ ] Liquibase manages schema and seed data
- [ ] Project on a public GitHub with README

**5.5 Update README**
```markdown
# Ship Tracker

## Running

### Requirements
- Docker and Docker Compose
- randommer.io API key

### Configuration
Create a `.env` file in the project root:
RANDOMMER_API_KEY=your_api_key

### Start (single command)
docker compose up

Application available at: http://localhost:4200
Backend API at: http://localhost:8080

### Login
Username: admin | Password: admin123
```

**5.6 Push to GitHub**
```bash
git add .
git commit -m "feat(frontend): finalize Angular app with navbar and docker support"
git push origin main
```

### How to Run for Testing (final — full stack in Docker)

```bash
# Ensure .env file exists with RANDOMMER_API_KEY
docker compose up
```

Open: `http://localhost:4200`

### Tests:

1. `docker compose up` starts all 3 services without errors
2. Go to `localhost:4200` → login form
3. Log in with `admin/admin123` → ship list with seed data
4. Add a ship (with generated name) → appears in the list
5. Edit a ship — change tonnage → data updated
6. Go to ship details → card + timeline with seed data
7. Add a location entry: date + country + port → appears on the timeline
8. Refresh the page → all data preserved
9. Click "Logout" → redirect to `/login`, attempt to access `/ships` → redirect to `/login`

### Definition of Done:
- All checkboxes from section 5.4 checked
- `docker compose up` starts everything — postgres, backend, frontend
- Application works end-to-end: login → ships → locations → timeline
- Code on a public GitHub with a working README

---

## Implementation Order (Summary)

| Stage | What | Result |
|-------|------|--------|
| 0 | Environment + Docker + GitHub | Foundation |
| 1 | Spring Boot + Liquibase + Security + API | Working backend |
| 2 | Angular setup + login | Gateway to the application |
| 3 | Ship list + form | Fleet management |
| 4 | Details + locations + timeline | Core business feature |
| 5 | Navigation + error handling + finalisation | Done |

---

## Priorities

### 🔴 CRITICAL (without this the task is incomplete):
- Login (Spring Security)
- Ship CRUD (list + add + edit)
- Adding location entries
- Timeline
- Docker + PostgreSQL + Liquibase + seed data

### 🟡 IMPORTANT (required in the task description):
- "Generate name" button (randommer.io via backend proxy)
- Country dictionary in the location form
- No ability to edit locations

### 🟢 NICE TO HAVE:
- Loading spinners while fetching data
- Error messages for backend unavailability

---

## Integration Tests — End-to-End Scenario

```bash
# Full stack startup
docker compose up -d

# Wait for services to be ready, then:

# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt

# 2. List ships (seed data)
curl http://localhost:8080/api/ships -b cookies.txt

# 3. Add a ship
curl -X POST http://localhost:8080/api/ships \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"name":"Test Ship","launchDate":"2020-01-01","shipType":"Cargo","tonnage":10000}'

# 4. Add a location report
curl -X POST http://localhost:8080/api/ships/1/reports \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"reportDate":"2024-06-01","country":"Germany","port":"Hamburg"}'

# 5. Get location reports
curl http://localhost:8080/api/ships/1/reports -b cookies.txt

# 6. Generate name
curl http://localhost:8080/api/ships/generate-name -b cookies.txt

# 7. Logout
curl -X POST http://localhost:8080/api/auth/logout -b cookies.txt

# 8. Attempt to access after logout → 401
curl http://localhost:8080/api/ships -b cookies.txt
```
