# ShipTracker – Aplikacja do zarządzania statkami i raportowania podróży

---

# 1. Problem

Firmy z branży transportu morskiego potrzebują narzędzia do:
- Zarządzania flotą statków (dodawanie, edycja danych jednostek)
- Śledzenia historii pozycji / podróży każdego statku
- Przejrzystego podglądu chronologicznego (timeline) tras

---

# 2. Użytkownik

Operator floty / dyspozytor w firmie zajmującej się transportem morskim:
- Dodaje statki do systemu i zarządza ich danymi
- Rejestruje aktualne lub historyczne położenie statku
- Przegląda historię podróży danej jednostki

---

# 3. Zakres funkcjonalny

## Moduł 1: Uwierzytelnianie
- Ekran logowania (username + hasło)
- Dostęp do systemu wyłącznie po zalogowaniu
- Jedno konto testowe dostarczane przez Liquibase seed data

## Moduł 2: Zarządzanie statkami
- Lista wszystkich statków z akcjami: Dodaj / Edytuj / Przejdź do szczegółów
- Formularz statku:
  - Nazwa (text, wymagana)
  - Data wodowania (date, wymagana)
  - Typ statku (text, wymagany)
  - Tonaż (number, wymagany, > 0)
  - Przycisk „Generuj nazwę" → wywołuje endpoint backendu → randommer.io API

## Moduł 3: Raportowanie podróży
- Dodawanie wpisu lokalizacji z widoku szczegółów statku
- Formularz wpisu:
  - Data (date, wymagana)
  - Kraj (select ze słownika — statyczna lista)
  - Port (text, wymagany)
- Wpisy są immutable — brak edycji po zapisaniu

## Moduł 4: Timeline
- Widok szczegółów statku zawiera oś czasu (timeline)
- Wpisy posortowane chronologicznie
- Styl wizualny: linia czasu z punktami i kartami danych

---

# 4. Wymagania techniczne

| Komponent | Technologia |
|-----------|-------------|
| Backend | Spring Boot 3.4.x, Java 21 |
| Baza danych | PostgreSQL 16 w Dockerze |
| Migracje i seed | Liquibase |
| Frontend | Angular (najnowsza stabilna) |
| Konteneryzacja | Docker Compose — postgres + backend + frontend |
| Repo | Publiczne repozytorium GitHub |

### Java 21 — zastosowane podejście

- DTO jako **Java Records** (immutable, kompaktowe) — zastępują klasy z getterami/setterami/konstruktorami; brak Lomboka
- **`Optional` + `.orElseThrow()`** — idiomatyczna obsługa braku zasobu (404) zamiast null-checków
- **`stream().toList()`** (Java 16+) — niemutowalna lista jako wynik mapowania encji na DTO
- **`RestClient`** (Spring 6 / Boot 3.2+) — nowoczesne API HTTP zamiast przestarzałego `RestTemplate`
- **Pattern matching, switch expressions, text blocks** — nie zastosowane; kod jest zbyt prosty, żeby ich użycie było naturalne a nie sztuczne

### Angular 17+ — zastosowane podejście

- **Standalone Components** — brak `NgModule`; każdy komponent sam deklaruje swoje zależności (analogia do braku Lomboka — mniej magii, więcej jawności)
- **`inject()` function** — wstrzykiwanie zależności bez konstruktora; czytelniejsze w funkcyjnych guardach i interceptorach
- **`@for`, `@if`, `@switch`** — nowa składnia control flow (Angular 17+) zamiast dyrektyw `*ngFor`, `*ngIf`
- **Signals** — reaktywny stan (`signal()`, `computed()`) zamiast `BehaviorSubject` dla prostego stanu (np. isLoggedIn)
- **Strictly Typed Reactive Forms** — `FormGroup<{ name: FormControl<string> }>` zamiast niegenerycznego `FormGroup`; błędy typów wykrywane w czasie kompilacji
- **`readonly` na interfejsach** — modele danych niemutowalne przez konwencję (analogia do Java Records)
- **Functional Guards** — `CanActivateFn` zamiast klas implementujących interfejs

### Bezpieczeństwo frontendu (OWASP)

- **Brak `localStorage`** do przechowywania sesji — sesja trzymana wyłącznie w HttpOnly cookie zarządzanym przez Spring Security
- **Brak `[innerHTML]` i `bypassSecurityTrust*`** — Angular automatycznie escapuje interpolowane wartości; nie omijamy tego mechanizmu
- **Interceptor 401** — wygaśnięcie sesji automatycznie przekierowuje na `/login`
- **Disable submit podczas żądania** — zapobiega wielokrotnemu wysłaniu formularza

---

# 5. Zewnętrzne API

**Randommer.io — generowanie nazwy statku**
- Klucz API przechowywany w pliku `.env` w katalogu głównym projektu (nie w kodzie)
- Plik `.env` jest w `.gitignore` — klucz nie trafia do repozytorium
- Docker Compose czyta klucz z `.env` i przekazuje go do kontenera backendu jako zmienna środowiskowa
- Spring Boot odczytuje go przez `${RANDOMMER_API_KEY}` w `application.properties`
- Backend eksponuje endpoint `/api/ships/generate-name` → wywołuje randommer.io
- Frontend wywołuje tylko backend (klucz API nie jest widoczny po stronie klienta)
- Obsługa błędu: gdy API nie odpowie → 503 z komunikatem

**Słownik krajów**
- Statyczna lista w Angular service (`countries.data.ts`)
- Brak zależności od zewnętrznego API

---

# 6. Reguły biznesowe i edge cases

## Reguły biznesowe
- Wpisy lokalizacji są **immutable** — brak endpointów PUT/PATCH dla `location_reports`; po zapisaniu nie można ich edytować
- Statek może mieć wiele wpisów lokalizacji (relacja `@OneToMany`); wpisy wyświetlane są chronologicznie
- Walidacja wymagana po stronie backendu (Bean Validation) oraz frontendu (Reactive Forms)

## Edge cases — obsługa błędów

| Scenariusz | Oczekiwane zachowanie |
|---|---|
| Żądanie do `/api/**` bez aktywnej sesji | `401 Unauthorized` |
| Logowanie z błędnymi danymi | `401 Unauthorized` |
| Tworzenie/edycja statku z pustymi lub niepoprawnymi polami | `400 Bad Request` + mapa błędów pól |
| Tonaż równy zero lub ujemny | `400 Bad Request` |
| Żądanie szczegółów nieistniejącego statku (`GET /api/ships/{id}`) | `404 Not Found` |
| Edycja nieistniejącego statku (`PUT /api/ships/{id}`) | `404 Not Found` |
| Dodanie raportu do nieistniejącego statku | `404 Not Found` |
| Niedostępność zewnętrznego API randommer.io | `503 Service Unavailable` + komunikat; formularz pozostaje aktywny |

---

# 7. Dane startowe (Liquibase seed)

| Typ | Zawartość |
|-----|-----------|
| Użytkownicy | 1 konto: `admin` / `admin123` (BCrypt hash) |
| Statki | 4–5 statków różnych typów (Cargo, Tanker, Container, Bulk Carrier) |
| Wpisy lokalizacji | 3–5 wpisów per statek (różne kraje, daty — do weryfikacji timeline) |

---

# 8. Struktura projektu

## Backend (`ship-tracker-backend/`)
```
src/main/java/com/shiptracker/
├── config/
│   ├── SecurityConfig.java          # CORS, session, CSRF, AuthenticationManager, UserDetailsService
│   ├── AppConfig.java               # Bean RestClient (HTTP client dla NameGeneratorService)
│   └── OpenApiConfig.java           # Swagger/OpenAPI — tytuł, wersja, schemat sesji cookie
├── controller/
│   ├── AuthController.java
│   ├── ShipController.java
│   └── LocationReportController.java
├── service/
│   ├── AuthService.java             # Logika logowania i wylogowania (SRP)
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

Wszystkie trzy serwisy uruchamiane jedną komendą: `docker compose up`

Klucz API randommer.io przechowywany w pliku `.env` (poza repozytorium):
```
RANDOMMER_API_KEY=twoj_klucz_api
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
Multi-stage build: JDK do kompilacji → JRE do uruchomienia (mniejszy obraz końcowy).

### `ship-tracker-frontend/Dockerfile`
Multi-stage build: Node.js do `ng build` → nginx do serwowania plików statycznych.

---

# 9. REST API

| Metoda | URL | Auth | Opis |
|--------|-----|------|------|
| POST | /api/auth/login | - | Logowanie |
| POST | /api/auth/logout | TAK | Wylogowanie |
| GET | /api/ships | TAK | Lista statków |
| POST | /api/ships | TAK | Dodaj statek |
| GET | /api/ships/{id} | TAK | Szczegóły statku |
| PUT | /api/ships/{id} | TAK | Edytuj statek |
| GET | /api/ships/generate-name | TAK | Losowa nazwa (proxy randommer.io) |
| GET | /api/ships/{id}/reports | TAK | Historia lokalizacji |
| POST | /api/ships/{id}/reports | TAK | Dodaj wpis lokalizacji |

---

# 10. Bezpieczeństwo

- Spring Security — session-based authentication
- Hasło hashowane BCrypt
- Wszystkie endpointy `/api/**` (poza `/api/auth/login`) wymagają sesji
- CORS zezwala na `http://localhost:4200`
- CSRF wyłączone (REST API, nie formularz HTML)
- Frontend wysyła żądania z `withCredentials: true`
