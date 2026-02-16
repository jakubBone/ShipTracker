# Plan Testów — Backend

## Trzy warstwy testów

```
        [  Integracyjne  ]   ← mało, wolne, testują całość
       [  Kontrolery (MVC) ]  ← MockMvc, testują HTTP layer
      [  Unit (Serwisy)     ]  ← Mockito, szybkie, bez Springa
```

---

## Warstwa 1 — Testy jednostkowe serwisów

### ShipServiceTest — 7 przypadków

| Test | Co sprawdza |
|---|---|
| `findAll_whenEmpty` | pusta lista z repo → serwis zwraca `[]` |
| `findAll_whenShipsExist` | mapowanie `Ship` → `ShipResponse` (pola, reportCount) |
| `findById_found` | szczęśliwa ścieżka — poprawny `ShipResponse` |
| `findById_notFound` | `Optional.empty()` → rzuca `ResourceNotFoundException` |
| `create` | zapisuje encję, zwraca zmapowany response |
| `update_found` | aktualizuje pola, zapisuje, zwraca response |
| `update_notFound` | rzuca `ResourceNotFoundException` |

### LocationReportServiceTest — 4 przypadki

| Test | Co sprawdza |
|---|---|
| `findByShipId_shipNotFound` | `existsById` = false → `ResourceNotFoundException` |
| `findByShipId_found` | lista raportów posortowana wg daty |
| `create_shipNotFound` | brak statku → wyjątek |
| `create_success` | tworzy raport, przypisuje do statku |

### NameGeneratorServiceTest — 3 przypadki

Wymaga drobnego refactoru produkcyjnego (patrz sekcja poniżej).

| Test | Co sprawdza |
|---|---|
| `generateName_success` | API zwraca tablicę → serwis zwraca `names[0]` |
| `generateName_emptyResponse` | API zwraca `[]` → `ExternalApiException` |
| `generateName_apiError` | `RestClientException` → `ExternalApiException` |

---

## Warstwa 2 — Testy kontrolerów

`@WebMvcTest` ładuje tylko warstwę HTTP — kontrolery, filtry, Spring Security. Nie ładuje JPA ani bazy. Serwisy mockujemy przez `@MockBean`.

MockMvc symuluje żądania HTTP bez uruchamiania prawdziwego serwera:
- `mockMvc.perform(get("/api/ships"))` — wysyła żądanie
- `.andExpect(status().isOk())` — sprawdza status HTTP
- `.andExpect(jsonPath("$.name").value("Atlantic"))` — sprawdza JSON

**Uwaga dot. SecurityConfig:** `@WebMvcTest` ładuje też `SecurityConfig`, który potrzebuje `UserRepository` do zbudowania `UserDetailsService`. W każdej klasie testowej kontrolera wymagany jest `@MockBean UserRepository`. Zalogowanego użytkownika symuluje `@WithMockUser`.

### ShipControllerTest — 14 przypadków

| Test | HTTP | Co sprawdza |
|---|---|---|
| `getAll_authenticated` | `GET /api/ships` | 200, lista JSON |
| `getAll_unauthenticated` | `GET /api/ships` | 401 (brak sesji) |
| `getById_found` | `GET /api/ships/1` | 200, poprawny obiekt |
| `getById_notFound` | `GET /api/ships/99` | 404 |
| `create_valid` | `POST /api/ships` | 201, zwrócony obiekt |
| `create_invalid` | `POST /api/ships` (brak pól) | 400, mapa błędów walidacji |
| `create_blankName` | `POST /api/ships` (`name: "   "`) | 400 — `@NotBlank` odrzuca samą spację |
| `create_zeroTonnage` | `POST /api/ships` (`tonnage: 0`) | 400 — `@Positive` odrzuca zero |
| `create_negativeTonnage` | `POST /api/ships` (`tonnage: -1`) | 400 — `@Positive` odrzuca ujemne |
| `update_valid` | `PUT /api/ships/1` | 200, zaktualizowany obiekt |
| `update_notFound` | `PUT /api/ships/99` | 404 |
| `generateName_success` | `GET /api/ships/generate-name` | 200, `{"name":"..."}` |
| `generateName_unauthenticated` | `GET /api/ships/generate-name` | 401 (brak sesji) |
| `generateName_apiError` | `GET /api/ships/generate-name` | 503 (ExternalApiException) |

### AuthControllerTest — 4 przypadki

Używa `@SpringBootTest` (pełny kontekst) + `@MockitoBean UserRepository`. Logika auth delegowana do `AuthService`, który jest ładowany automatycznie.

| Test | Co sprawdza |
|---|---|
| `login_valid` | `AuthenticationManager` zwraca auth → 200 |
| `login_invalid` | rzuca `BadCredentialsException` → 401 |
| `login_blankFields` | `@Valid` odrzuca → 400 |
| `logout` | 200, sesja unieważniona |

### LocationReportControllerTest — 5 przypadków

| Test | Co sprawdza |
|---|---|
| `getByShip_found` | 200, lista raportów |
| `getByShip_shipNotFound` | 404 |
| `create_valid` | 201, nowy raport |
| `create_invalid` | 400 (brak pól) |
| `create_shipNotFound` | 404 |

---

## Warstwa 3 — Test repozytorium

`H2 in-memory` - automatyczne tworzenie schematu przez Hibernate

`@DataJpaTest` ładuje tylko warstwę JPA. PostgreSQL zastępowany jest bazą H2 działającą w pamięci. Liquibase wyłączany. Schemat tworzony przez Hibernate z adnotacji JPA.

Testujemy wyłącznie niestandardowe zapytania (standardowe metody JPA nie wymagają testów).

### LocationReportRepositoryTest — 2 przypadki

| Test | Co sprawdza |
|---|---|
| `findByShipIdOrderByReportDateAsc_ordered` | raporty wracają chronologicznie |
| `findByShipIdOrderByReportDateAsc_empty` | brak raportów → pusta lista |

---

## Wymagane zmiany infrastrukturalne

| Zmiana | Cel |
|---|---|
| Dodanie `com.h2database:h2` (scope: test) do `pom.xml` | H2 dla `@DataJpaTest` |
| Nowy `src/test/resources/application.properties` | Wyłączenie Liquibase, konfiguracja H2 |
| Nowa klasa `AppConfig.java` | Bean `RestClient` dla DI |
| Modyfikacja `NameGeneratorService.java` | Przyjęcie `RestClient` z zewnątrz |

---

## Podsumowanie

| Plik | Typ | Przypadki |
|---|---|---|
| `ShipServiceTest` | Unit / Mockito | 7 |
| `LocationReportServiceTest` | Unit / Mockito | 4 |
| `NameGeneratorServiceTest` | Unit / Mockito | 3 |
| `AuthControllerTest` | Web slice / MockMvc | 4 |
| `ShipControllerTest` | Web slice / MockMvc | 14 |
| `LocationReportControllerTest` | Web slice / MockMvc | 5 |
| `LocationReportRepositoryTest` | JPA slice / H2 | 2 |
| `ShipTrackerBackendApplicationTests` | Integration / Spring context | 1 |
| **Łącznie** | | **40** |
