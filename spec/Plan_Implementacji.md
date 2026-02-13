# ShipTracker — Plan Implementacji (krok po kroku)

---

## ETAP 0: Przygotowanie środowiska

### Co robimy i dlaczego
Zanim napiszemy linię kodu, musimy mieć działające narzędzia. To jak budowa domu — zanim wlejemy fundament, sprawdzamy czy mamy betoniarkę.

### Kroki:

**0.1 Weryfikacja narzędzi**
```bash
java --version        # potrzebujemy Java 21+
mvn --version         # Maven do budowania backendu
node --version        # Node.js do Angulara
npm --version
ng version            # Angular CLI (jeśli nie ma: npm install -g @angular/cli)
docker --version      # Docker do bazy danych
git --version
```

**0.2 Utwórz plik `.env` z kluczem API**

W katalogu głównym projektu (obok `docker-compose.yml`):
```
RANDOMMER_API_KEY=twoj_klucz_api
```
Plik `.env` musi być w `.gitignore` — klucz nie może trafić do repozytorium.

**0.3 Uruchom bazę danych (tylko postgres na etapie developmentu)**
```bash
docker compose up postgres -d

# Weryfikacja:
docker ps   # powinien być kontener postgres:16 działający na porcie 5432
```
Na etapie developmentu uruchamiamy tylko postgresa. Backend i frontend odpalamy lokalnie (`./mvnw spring-boot:run`, `ng serve`) dla wygody (hot-reload). Pełny `docker compose up` (wszystkie 3 serwisy) uruchamiamy dopiero po ukończeniu frontendu w Etapie 5.

**0.4 Stwórz repozytorium GitHub**
- Utwórz nowe publiczne repo na GitHub (np. `ship-tracker`)
- `git init`, `git remote add origin <URL>`
- Dodaj `.gitignore` dla Java + Node + plik `.env`

### Definicja "done":
- `docker ps` pokazuje działający kontener postgres
- Plik `.env` istnieje w katalogu głównym i jest w `.gitignore`
- Java 21 i Maven dostępne w terminalu
- Angular CLI dostępne (`ng version`)
- Puste repo na GitHubie gotowe

---

## ETAP 1: Backend — Fundament (Spring Boot + Liquibase + Security)

### Co robimy i dlaczego
Backend to serce aplikacji. Frontend bez backendu to tylko statyczna strona. Zaczynamy od backendu, bo:
1. Definiuje kontrakt danych (co zwraca API)
2. Angular będzie się do niego podłączał

### Kroki:

**1.1 Generuj projekt przez Spring Initializr**
- Wejdź na: https://start.spring.io
- Ustaw:
  - Project: Maven
  - Language: Java
  - Spring Boot: 3.4.x
  - Group: `com.shiptracker`
  - Artifact: `ship-tracker-backend`
  - Java: 21
- Dodaj dependencies:
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - PostgreSQL Driver
  - Liquibase Migration
  - Validation

**1.2 Skonfiguruj application.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/shiptracker_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
randommer.api.key=${RANDOMMER_API_KEY:demo-key}
```

`ddl-auto=validate` — Liquibase zarządza schematem, Hibernate tylko go weryfikuje przy starcie.

**1.3 Skonfiguruj Liquibase**

Plik `db.changelog-master.xml` — master lista migracji:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog" ...>
    <include file="classpath:db/changelog/001-create-tables.xml"/>
    <include file="classpath:db/changelog/002-seed-data.xml"/>
</databaseChangeLog>
```

Plik `001-create-tables.xml` — tworzy tabele:
- `users` (id BIGSERIAL PK, username VARCHAR UNIQUE NOT NULL, password VARCHAR NOT NULL, role VARCHAR NOT NULL)
- `ships` (id BIGSERIAL PK, name VARCHAR NOT NULL, launch_date DATE NOT NULL, ship_type VARCHAR NOT NULL, tonnage NUMERIC(12,2) NOT NULL)
- `location_reports` (id BIGSERIAL PK, ship_id BIGINT FK→ships, report_date DATE NOT NULL, country VARCHAR NOT NULL, port VARCHAR NOT NULL)

Liquibase zamiast schema.sql — śledzi historię migracji, można dodawać kolejne wersje schematu bez niszczenia danych. Standard w projektach produkcyjnych.

Plik `002-seed-data.xml` — dane testowe:
- 1 użytkownik `admin` (hasło zahashowane BCrypt)
- 4 statki z różnymi typami (Cargo, Tanker, Container, Bulk Carrier)
- 3–5 wpisów lokalizacji per statek

**1.4 Stwórz encje (Entity)**

`User.java`:
- `id` (Long, @GeneratedValue)
- `username` (String, @Column(unique=true))
- `password` (String — hashowane BCrypt)
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

> **Uwaga:** Adnotacje Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`) należą do warstwy DTO (`ShipRequest`, `LocationReportRequest`), nie do encji. Encje są chronione przez `nullable = false` na poziomie bazy danych. Walidacja wejścia odbywa się w kontrolerze przez `@Valid @RequestBody`.

LocationReport jest immutable by design — brak pól updatedAt, brak endpointu PUT/PATCH.

**1.5 Stwórz repozytoria**
```java
ShipRepository extends JpaRepository<Ship, Long>

LocationReportRepository extends JpaRepository<LocationReport, Long>
    // + findByShipIdOrderByReportDateAsc(Long shipId)

UserRepository extends JpaRepository<User, Long>
    // + Optional<User> findByUsername(String username)
```

**1.6 DTO jako Java Records (Java 21)**

W Java 21 DTO piszemy jako `record` zamiast klas z getterami/setterami. Record jest immutable i kompaktowy — kompilator generuje konstruktor, gettery, equals, hashCode i toString automatycznie. Brak potrzeby Lomboka.

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

**1.7 Stwórz serwisy (Service)**

`ShipService`:
- `findAll()` → `List<ShipResponse>`
- `findById(Long id)` → `ShipResponse` (rzuca ResourceNotFoundException jeśli brak)
- `create(ShipRequest dto)` → `ShipResponse`
- `update(Long id, ShipRequest dto)` → `ShipResponse`

`LocationReportService`:
- `findByShipId(Long shipId)` → `List<LocationReportResponse>` posortowane wg daty
- `create(Long shipId, LocationReportRequest dto)` → `LocationReportResponse`

`NameGeneratorService`:
- `generateName()` → `String` (wywołuje randommer.io przez RestClient — nowe API Spring 6)
- Klucz API wstrzykiwany z `application.properties` przez `@Value`
- Obsługa wyjątku gdy API niedostępne → rzuca wyjątek z komunikatem

**1.8 Stwórz kontrolery (Controller)**

```
GET  /api/ships              → ShipController.getAll()             → 200 List<ShipResponse>
POST /api/ships              → ShipController.create(...)          → 201 ShipResponse
GET  /api/ships/{id}         → ShipController.getById()            → 200 ShipResponse
PUT  /api/ships/{id}         → ShipController.update(...)          → 200 ShipResponse
GET  /api/ships/generate-name → ShipController.generateName()      → 200 { "name": "..." }
GET  /api/ships/{id}/reports → LocationReportController.getByShip() → 200 List<LocationReportResponse>
POST /api/ships/{id}/reports → LocationReportController.create(...) → 201 LocationReportResponse
POST /api/auth/login         → AuthController.login()              → 200 { "message": "Logged in successfully" }
POST /api/auth/logout        → AuthController.logout(HttpSession)  → 200 { "message": "Logged out successfully" }
```

**1.9 Skonfiguruj Spring Security**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // permitAll: POST /api/auth/login
    // authenticated: wszystkie inne /api/**
    // session-based (SessionCreationPolicy.IF_REQUIRED)
    // CORS: allowedOrigins localhost:4200, allowCredentials true
    // CSRF: disabled (REST API)
    // 401 dla niezalogowanych (nie redirect na login page HTML)
}
```

Session-based zamiast JWT — prostsze dla tej skali. JWT jest potrzebny przy mikroserwisach lub gdy frontend jest hostowany oddzielnie od backendu na innej domenie.

**1.10 Globalny handler błędów**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String message, int status, Instant timestamp) {}

    // MethodArgumentNotValidException → 400 + Map<String, String> (pole → komunikat błędu)
    //   Uwaga: ten handler zwraca Map, nie ErrorResponse — inna struktura niż pozostałe
    // ResourceNotFoundException       → 404 + ErrorResponse
    // AuthenticationException         → 401 + ErrorResponse ("Invalid credentials")
    // ExternalApiException            → 503 + ErrorResponse
    // Exception (fallback)            → 500 + ErrorResponse ("Internal server error")
}
```

`ExternalApiException` ma dwa konstruktory:
- `ExternalApiException(String message)` — ogólny błąd API
- `ExternalApiException(String message, Throwable cause)` — używany w `NameGeneratorService` przy `RestClientException`, zachowuje oryginalny stack trace

### Testy backendu:

```bash
./mvnw spring-boot:run

# Logowanie
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt
# Oczekiwane: 200 OK, ciasteczko JSESSIONID w cookies.txt

# Logowanie — złe hasło
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}'
# Oczekiwane: 401 Unauthorized

# Brak sesji → 401
curl http://localhost:8080/api/ships
# Oczekiwane: 401 Unauthorized

# Lista statków (wymaga sesji)
curl http://localhost:8080/api/ships -b cookies.txt
# Oczekiwane: JSON array z 4 statkami z seed data

# Dodanie statku
curl -X POST http://localhost:8080/api/ships \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"name":"Black Pearl","launchDate":"2010-05-15","shipType":"Cargo","tonnage":50000}'
# Oczekiwane: 201 Created, JSON z id

# Walidacja — puste pola
curl -X POST http://localhost:8080/api/ships \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"name":""}'
# Oczekiwane: 400 Bad Request, JSON z mapą błędów pól

# Walidacja — tonaż ujemny
curl -X POST http://localhost:8080/api/ships \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"name":"X","launchDate":"2010-01-01","shipType":"Cargo","tonnage":-1}'
# Oczekiwane: 400 Bad Request

# Nieistniejący statek
curl http://localhost:8080/api/ships/99999 -b cookies.txt
# Oczekiwane: 404 Not Found

# Dodanie wpisu lokalizacji
curl -X POST http://localhost:8080/api/ships/1/reports \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"reportDate":"2024-03-10","country":"Poland","port":"Gdańsk"}'
# Oczekiwane: 201 Created

# Dodanie wpisu do nieistniejącego statku
curl -X POST http://localhost:8080/api/ships/99999/reports \
  -H "Content-Type: application/json" -b cookies.txt \
  -d '{"reportDate":"2024-03-10","country":"Poland","port":"Gdańsk"}'
# Oczekiwane: 404 Not Found

# Generowanie nazwy
curl http://localhost:8080/api/ships/generate-name -b cookies.txt
# Oczekiwane: 200 OK, {"name":"<losowa nazwa>"}
# Gdy API niedostępne: 503 Service Unavailable
```

### Definicja "done":
- Liquibase tworzy tabele i wgrywa seed data przy starcie
- Logowanie zwraca 200 + ciasteczko sesji
- Błędne dane logowania → 401
- GET /api/ships → lista 4 statków (z seed)
- POST /api/ships → tworzenie z walidacją (400 przy błędnych polach)
- GET /api/ships/{id} → 404 dla nieistniejącego id
- GET /api/ships/{id}/reports → lista posortowana chronologicznie
- POST /api/ships/{id}/reports → 404 gdy statek nie istnieje
- 401 dla requestów bez sesji
- GET /api/ships/generate-name → losowa nazwa lub 503 gdy API pada

---

## ETAP 2: Frontend — Projekt Angular + Logowanie

### Co robimy i dlaczego
Budujemy interfejs. Zaczynamy od logowania — to brama do całej aplikacji. Użytkownik niezalogowany nie powinien widzieć nic poza formularzem login.

### Kroki:

**2.1 Wygeneruj projekt Angular**
```bash
ng new ship-tracker-frontend --routing=true --style=scss
cd ship-tracker-frontend
ng serve   # powinno działać na localhost:4200
```

**2.2 Zainstaluj Angular Material**
```bash
ng add @angular/material
```

Angular Material to oficjalna biblioteka UI dla Angulara. Daje gotowe komponenty (formularze, tabele, karty, toolbar) — spójny wygląd bez pisania CSS od zera.

**2.3 Skonfiguruj środowisko**

`environments/environment.ts`:
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

**2.4 Skonfiguruj HttpClient z interceptorami**

`app.config.ts`:
```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([credentialsInterceptor, errorInterceptor])),
    provideRouter(routes)
  ]
};
```

Dwa interceptory:
- `credentialsInterceptor` — dodaje `{ withCredentials: true }` do każdego żądania; bez tego przeglądarka nie wyśle ciasteczka sesji i Spring Security odrzuci każdy request jako niezalogowany
- `errorInterceptor` — globalnie przechwytuje błąd 401 i przekierowuje na `/login` (wygaśnięcie sesji)

**Konwencje bezpieczeństwa (obowiązują przez cały projekt):**
- Nigdy nie używać `localStorage` / `sessionStorage` do przechowywania sesji — sesja żyje wyłącznie w HttpOnly cookie
- Nigdy nie używać `[innerHTML]` ani `DomSanitizer.bypassSecurityTrust*` — Angular automatycznie escapuje wartości
- Blokować przycisk submit podczas trwającego żądania HTTP (flaga `isLoading`)

**2.5 Stwórz AuthService**
```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);       // inject() zamiast konstruktora
  private readonly router = inject(Router);
  private readonly loggedIn = signal(false);        // Signal zamiast BehaviorSubject

  login(username: string, password: string): Observable<void>
  logout(): Observable<void>
  isLoggedIn(): boolean
}
```

`signal` to nowy mechanizm reaktywności w Angular 17+ — lżejszy i prostszy niż RxJS dla prostego stanu logowania. `inject()` zastępuje konstruktor z parametrami — czytelniejsze w serwisach i guardach.

**2.6 Stwórz AuthGuard (functional guard)**
```typescript
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.createUrlTree(['/login']);
};
```

Guard stoi przed każdą chronioną trasą. Jeśli użytkownik nie jest zalogowany — redirect na `/login`.

**2.7 Stwórz LoginComponent**
- Reactive Form: username + password
- Przycisk "Zaloguj"
- Obsługa błędu 401 → komunikat „Nieprawidłowe dane logowania"
- Po sukcesie: redirect na `/ships`

**2.8 Skonfiguruj routing (tylko trasy Etapu 2)**

Routing budujemy przyrostowo — dodajemy trasy tylko dla komponentów które już istnieją. Trasy `/ships` zostaną dodane w Etapie 3.

```typescript
export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];
```

### Jak uruchomić do testów

```bash
# Terminal 1 — baza danych
docker compose up postgres -d

# Terminal 2 — backend
cd ship-tracker-backend
./mvnw spring-boot:run

# Terminal 3 — frontend
cd ship-tracker-frontend
ng serve
```

Otwórz: `http://localhost:4200`

### Test:

1. Otwórz `http://localhost:4200` → redirect na `/login`
2. Wpisz złe hasło → komunikat „Nieprawidłowe dane logowania", brak redirectu
3. Wpisz `admin` / `admin123` → redirect na `/ships` (pusta strona — komponent powstanie w Etapie 3, redirect jest poprawny)
4. Przycisk „Zaloguj się" zablokowany podczas trwającego żądania

### Definicja "done":
- `ng serve` działa bez błędów kompilacji
- Strona logowania wyświetla się pod `localhost:4200/login`
- Błędne dane → komunikat błędu widoczny pod formularzem
- Poprawne dane → redirect na `/ships` (route jeszcze nie wyrenderuje widoku — to Etap 3)

---

## ETAP 3: Frontend — Lista statków + Formularz

### Co robimy i dlaczego
Główny ekran aplikacji. Użytkownik zobaczy tu swoją flotę i będzie mógł nią zarządzać.

### Kroki:

**3.1 Stwórz modele TypeScript**
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

**3.2 Stwórz ShipService**
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

**3.3 Stwórz ShipListComponent**
- `mat-table` z kolumnami: Nazwa, Typ, Tonaż, Data wodowania, Akcje
- Przyciski: „Szczegóły", „Edytuj"
- Przycisk „Dodaj nowy statek" nad tabelą
- Dane ładowane z `/api/ships` przy inicjalizacji (`ngOnInit`)

**3.4 Stwórz ShipFormComponent (dodaj + edytuj)**

Jeden komponent dla obu akcji — wykrywa tryb po obecności `:id` w URL. Jeśli id jest → tryb edycji (preload danych), bez id → tryb dodawania.

Formularz (Strictly Typed Reactive Forms):
```typescript
form = new FormGroup({
  name:       new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
  launchDate: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
  shipType:   new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
  tonnage:    new FormControl<number | null>(null, [Validators.required, Validators.min(1)])
});
```

`nonNullable: true` oznacza że `reset()` przywraca wartość domyślną zamiast `null` — brak niespodzianek typowych. TypeScript wykryje błędy typów w czasie kompilacji.
- Przycisk „Generuj nazwę statku"

**3.5 Zaimplementuj przycisk „Generuj nazwę statku"**

```typescript
generateName(): void {
  this.shipService.generateName().subscribe({
    next: ({ name }) => this.shipForm.patchValue({ name }),
    error: () => this.nameError = 'Nie udało się pobrać nazwy. Spróbuj ponownie.'
  });
}
```

Backend wywołuje randommer.io — klucz API jest bezpieczny po stronie serwera, nie widoczny w przeglądarce.

**3.6 Walidacja formularza**
- Wyświetl błędy pod polami (np. „Pole wymagane")
- Zablokuj submit jeśli formularz niepoprawny

**3.7 Dodaj trasy do routingu**

Rozszerzamy `app.routes.ts` o trasy statków. Trasa `/ships/:id` (szczegóły) zostanie dodana w Etapie 4.

```typescript
export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'ships', component: ShipList, canActivate: [authGuard] },
  { path: 'ships/new', component: ShipForm, canActivate: [authGuard] },
  { path: 'ships/:id/edit', component: ShipForm, canActivate: [authGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];
```

Uwaga: `ships/new` musi być przed `ships/:id` (Etap 4) — Angular dopasowuje trasy od góry, `new` byłoby inaczej potraktowane jako `:id`.

### Jak uruchomić do testów

```bash
# Terminal 1 — baza danych
docker compose up postgres -d

# Terminal 2 — backend
cd ship-tracker-backend
./mvnw spring-boot:run

# Terminal 3 — frontend
cd ship-tracker-frontend
ng serve
```

Otwórz: `http://localhost:4200` → zaloguj się → lista statków

### Test:

1. Wejdź na `localhost:4200` → zaloguj się → tabela z 4 statkami z seed data
2. Bezpośredni URL `/ships` bez zalogowania → redirect na `/login` (guard działa)
3. Kliknij „Dodaj nowy statek" → formularz pusty
4. Submit bez wypełnienia pól → błędy walidacji pod polami
5. Kliknij „Generuj nazwę" → pole name uzupełnione
6. Wypełnij pozostałe pola → submit → redirect na listę, nowy statek widoczny
7. Kliknij „Edytuj" przy statku → formularz z danymi statku
8. Zmień nazwę → submit → lista zaktualizowana

### Definicja "done":
- Lista statków wyświetla dane z bazy (seed data — 4 statki)
- Bezpośrednie wejście na `/ships` bez logowania → redirect na `/login`
- Formularz dodawania działa — nowy statek pojawia się na liście
- Formularz edycji działa — dane się zapisują
- Przycisk „Generuj nazwę" wypełnia pole nazwy
- Walidacja wyświetla błędy pod polami

---

## ETAP 4: Frontend — Szczegóły statku + Dodawanie lokalizacji

### Co robimy i dlaczego
To serce funkcjonalne zadania — raportowanie podróży. Widok szczegółów statku to miejsce gdzie widzimy całą historię i dodajemy nowe wpisy.

### Kroki:

**4.1 Stwórz modele TypeScript**
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

**4.2 Stwórz LocationReportService**
```typescript
@Injectable({ providedIn: 'root' })
export class LocationReportService {
  getByShipId(shipId: number): Observable<LocationReport[]>
  create(shipId: number, report: LocationReportRequest): Observable<LocationReport>
}
```

**4.3 Przygotuj słownik krajów**

```typescript
// src/app/core/data/countries.data.ts
export const COUNTRIES: string[] = [
  'Albania', 'Belgium', 'Bulgaria', 'Croatia', 'Cyprus',
  'Denmark', 'Estonia', 'Finland', 'France', 'Germany',
  'Greece', 'Latvia', 'Lithuania', 'Malta', 'Netherlands',
  'Norway', 'Poland', 'Portugal', 'Romania', 'Spain',
  'Sweden', 'Turkey', 'Ukraine', 'United Kingdom',
  // + pełna lista
];
```

Statyczna lista zamiast zewnętrznego API — prostsze, bez zależności.

**4.4 Stwórz ShipDetailComponent**

Zawiera:
1. Karta z danymi statku (pobrana z `ShipService.getById(id)`)
2. `LocationReportFormComponent` — formularz dodawania wpisu
3. `TimelineComponent` — historia podróży

Po dodaniu nowego wpisu → odśwież timeline (re-fetch lub append do listy).

**4.5 Stwórz LocationReportFormComponent**

Reactive Form:
- `reportDate` — `<input type="date">` lub `<mat-datepicker>`
- `country` — `<mat-select>` z `COUNTRIES`
- `port` — `<input matInput>`
- Walidatory: required na wszystkich polach
- Submit → `LocationReportService.create()` → emituje zdarzenie do rodzica

**4.6 Dodaj trasę szczegółów do routingu**

Rozszerzamy `app.routes.ts` o ostatnią brakującą trasę. Musi być po `ships/new` i `ships/:id/edit`:

```typescript
{ path: 'ships/:id', component: ShipDetail, canActivate: [authGuard] },
```

**4.7 Stwórz TimelineComponent**

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

`@for` to nowa składnia Angular 17+ (control flow) zamiast `*ngFor`. CSS timeline: linia pionowa po lewej, kropki przy każdym wpisie, karty z danymi.

### Jak uruchomić do testów

```bash
# Terminal 1 — baza danych
docker compose up postgres -d

# Terminal 2 — backend
cd ship-tracker-backend
./mvnw spring-boot:run

# Terminal 3 — frontend
cd ship-tracker-frontend
ng serve
```

Otwórz: `http://localhost:4200` → zaloguj się → lista statków → kliknij „Szczegóły"

### Test:

1. Kliknij „Szczegóły" przy pierwszym statku z seed data → widok szczegółów z kartą danych statku
2. Timeline wyświetla historię lokalizacji z seed data (posortowana chronologicznie)
3. Wypełnij formularz lokalizacji: data + kraj (select) + port → submit
4. Nowy wpis pojawia się na timeline bez przeładowania strony
5. Odśwież stronę (F5) → wpis nadal widoczny (zapisany w bazie)
6. Brak przycisków edycji przy wpisach timeline

### Definicja "done":
- Widok szczegółów statku wyświetla dane statku
- Timeline pokazuje historię lokalizacji (z seed data) posortowaną chronologicznie
- Formularz dodawania wpisu lokalizacji działa
- Nowy wpis pojawia się na timeline bez przeładowania strony
- Pola formularza są walidowane (wymagane)
- Brak możliwości edycji istniejących wpisów (tylko wyświetlanie)

---

## ETAP 5: Nawigacja, error handling, konteneryzacja, finalizacja

### Co robimy i dlaczego
Aplikacja działa — teraz ją dopracowujemy: nawigacja, obsługa błędów, wygląd. Na końcu konteneryzujemy frontend żeby całość uruchamiała się jedną komendą.

### Kroki:

**5.1 Dodaj nawigację (toolbar)**
- `mat-toolbar` z nazwą aplikacji
- Przycisk „Wyloguj" (w prawym rogu) → `AuthService.logout()` → redirect `/login`
- Link „Lista statków"

**5.2 HTTP Interceptor dla błędów globalnych**
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

Jeśli sesja wygaśnie i backend zwróci 401 — użytkownik zostanie automatycznie przekierowany na `/login`.

**5.3 Dockerfile dla frontendu**

Plik `ship-tracker-frontend/Dockerfile`:
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
- Etap 1: Node.js buduje aplikację (`ng build`) → pliki statyczne HTML/JS/CSS w katalogu `dist/`
- Etap 2: nginx serwuje te pliki pod portem 80 (mapowanym na 4200 w docker-compose)

Dlaczego nginx zamiast `ng serve` w kontenerze? `ng serve` to serwer deweloperski z hot-reloadem — nie nadaje się do "produkcji". nginx to lekki serwer HTTP zoptymalizowany do serwowania plików statycznych.

**5.4 Finalna weryfikacja wymagań**

- [ ] Ekran logowania — dostęp tylko po zalogowaniu
- [ ] Lista statków: Dodaj / Edytuj / Szczegóły
- [ ] Formularz statku: 4 pola + generowanie nazwy (randommer.io)
- [ ] Widok szczegółów statku
- [ ] Formularz dodawania lokalizacji: data + kraj (słownik) + port
- [ ] Wpisy lokalizacji są immutable (brak edycji)
- [ ] Timeline — oś czasu posortowana chronologicznie
- [ ] `docker compose up` uruchamia postgres + backend + frontend
- [ ] Plik `.env` w `.gitignore`, klucz API nie w repozytorium
- [ ] Liquibase zarządza schematem i seed data
- [ ] Projekt na publicznym GitHubie z README

**5.5 Zaktualizuj README**
```markdown
# Ship Tracker

## Uruchomienie

### Wymagania
- Docker i Docker Compose
- Klucz API randommer.io

### Konfiguracja
Utwórz plik `.env` w katalogu głównym projektu:
RANDOMMER_API_KEY=twoj_klucz_api

### Uruchomienie (jedna komenda)
docker compose up

Aplikacja dostępna pod: http://localhost:4200
Backend API pod: http://localhost:8080

### Logowanie
Login: admin | Hasło: admin123
```

**5.6 Push na GitHub**
```bash
git add .
git commit -m "feat(frontend): finalize Angular app with navbar and docker support"
git push origin main
```

### Jak uruchomić do testów (finalne — cały stack w Dockerze)

```bash
# Upewnij się że plik .env istnieje z kluczem RANDOMMER_API_KEY
docker compose up
```

Otwórz: `http://localhost:4200`

### Test:

1. `docker compose up` uruchamia wszystkie 3 serwisy bez błędów
2. Wejdź na `localhost:4200` → formularz logowania
3. Zaloguj się `admin/admin123` → lista statków z seed data
4. Dodaj statek (z wygenerowaną nazwą) → pojawia się na liście
5. Edytuj statek — zmień tonaż → dane zaktualizowane
6. Przejdź do szczegółów statku → karta + timeline z seed data
7. Dodaj wpis lokalizacji: data + kraj + port → pojawia się na timeline
8. Odśwież stronę → wszystkie dane zachowane
9. Kliknij „Wyloguj" → redirect na `/login`, próba wejścia na `/ships` → redirect na `/login`

### Definicja "done":
- Wszystkie checkboxy z sekcji 5.4 zaznaczone
- `docker compose up` uruchamia całość — postgres, backend, frontend
- Aplikacja działa end-to-end: logowanie → statki → lokalizacje → timeline
- Kod na publicznym GitHubie z działającym README

---

## Kolejność realizacji (podsumowanie)

| Etap | Co | Efekt |
|------|----|-------|
| 0 | Środowisko + Docker + GitHub | Fundament |
| 1 | Spring Boot + Liquibase + Security + API | Działający backend |
| 2 | Angular setup + logowanie | Brama do aplikacji |
| 3 | Lista statków + formularz | Zarządzanie flotą |
| 4 | Szczegóły + lokalizacje + timeline | Główna funkcja biznesowa |
| 5 | Nawigacja + error handling + finalizacja | Gotowe |

---

## Priorytety

### 🔴 KRYTYCZNE (bez tego zadanie jest niekompletne):
- Logowanie (Spring Security)
- CRUD statków (lista + dodaj + edytuj)
- Dodawanie wpisów lokalizacji
- Timeline (oś czasu)
- Docker + PostgreSQL + Liquibase + seed data

### 🟡 WAŻNE (wymagane w treści zadania):
- Przycisk „Generuj nazwę" (randommer.io przez backend proxy)
- Słownik krajów w formularzu lokalizacji
- Brak możliwości edycji lokalizacji

### 🟢 NICE TO HAVE:
- Loading spinnery podczas ładowania danych
- Komunikaty błędów dla niedostępności backendu

---

## Testy integracyjne — scenariusz end-to-end

```bash
# Terminal 1
docker compose up -d
cd ship-tracker-backend && ./mvnw spring-boot:run

# Terminal 2
cd ship-tracker-frontend && ng serve

# Przeglądarka: http://localhost:4200
```

1. Wejdź na `/ships` bez logowania → redirect na `/login`
2. Zaloguj się `admin` / `admin123` → lista statków z seed data
3. Dodaj nowy statek (z wygenerowaną nazwą) → pojawia się na liście
4. Edytuj statek — zmień tonaż → dane zaktualizowane
5. Przejdź do szczegółów statku → karta + timeline z seed data
6. Dodaj wpis lokalizacji: data + kraj + port → pojawia się na timeline
7. Odśwież stronę → wszystkie dane zachowane
8. Wyloguj → redirect na `/login`, próba wejścia na `/ships` → redirect
