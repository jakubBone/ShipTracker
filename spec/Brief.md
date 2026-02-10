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
| Repo | Publiczne repozytorium GitHub |

### Java 21 — wymagane podejście

- DTO jako **Java Records** (immutable, kompaktowe)
- **Pattern matching** dla instanceof tam gdzie zasadne
- **Switch expressions** zamiast switch statement
- **Text blocks** do wieloliniowych stringów (SQL, JSON w testach)
- Brak Lombok na DTO — records zastępują gettery/settery/constructory

---

# 5. Zewnętrzne API

**Randommer.io — generowanie nazwy statku**
- Klucz API przechowywany w `application.properties` (nie w kodzie)
- Backend eksponuje endpoint `/api/ships/generate-name` → wywołuje randommer.io
- Frontend wywołuje tylko backend (klucz API nie jest widoczny po stronie klienta)
- Obsługa błędu: gdy API nie odpowie → 503 z komunikatem

**Słownik krajów**
- Statyczna lista w Angular service (`countries.data.ts`)
- Brak zależności od zewnętrznego API

---

# 6. Edge Cases

- Wpisy lokalizacji są immutable — brak endpointu PUT/PATCH dla location_reports
- Statek może mieć wiele wpisów lokalizacji (relacja @OneToMany)
- Walidacja po stronie backendu (Bean Validation) i frontendu (Reactive Forms)
- Generowanie nazwy może zwrócić błąd → UI pokazuje komunikat, formularz pozostaje aktywny
- Seed data dostępne od pierwszego uruchomienia (Liquibase changeset)

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
│   ├── SecurityConfig.java
│   └── CorsConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ShipController.java
│   └── LocationReportController.java
├── service/
│   ├── AuthService.java
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
│   ├── ShipRequest.java             # record
│   ├── ShipResponse.java            # record
│   ├── LocationReportRequest.java   # record
│   └── LocationReportResponse.java  # record
└── exception/
    ├── GlobalExceptionHandler.java
    └── ResourceNotFoundException.java

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

## Docker (`docker-compose.yml`)
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
volumes:
  postgres_data:
```

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
