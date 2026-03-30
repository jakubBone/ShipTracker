# Backend Test Plan

## Three Test Layers

```
        [  Integration   ]   ← few, slow, test the whole stack
       [  Controllers (MVC) ]  ← MockMvc, test the HTTP layer
      [  Unit (Services)    ]  ← Mockito, fast, no Spring context
```

---

## Layer 1 — Service Unit Tests

### ShipServiceTest — 7 cases

| Test | What it verifies |
|---|---|
| `findAll_whenEmpty` | empty list from repo → service returns `[]` |
| `findAll_whenShipsExist` | mapping `Ship` → `ShipResponse` (fields, reportCount) |
| `findById_found` | happy path — correct `ShipResponse` |
| `findById_notFound` | `Optional.empty()` → throws `ResourceNotFoundException` |
| `create` | saves entity, returns mapped response |
| `update_found` | updates fields, saves, returns response |
| `update_notFound` | throws `ResourceNotFoundException` |

### LocationReportServiceTest — 4 cases

| Test | What it verifies |
|---|---|
| `findByShipId_shipNotFound` | `existsById` = false → `ResourceNotFoundException` |
| `findByShipId_found` | list of reports sorted by date |
| `create_shipNotFound` | ship not found → exception |
| `create_success` | creates report, assigns to ship |

### NameGeneratorServiceTest — 3 cases

Requires a minor production refactor (see section below).

| Test | What it verifies |
|---|---|
| `generateName_success` | API returns an array → service returns `names[0]` |
| `generateName_emptyResponse` | API returns `[]` → `ExternalApiException` |
| `generateName_apiError` | `RestClientException` → `ExternalApiException` |

---

## Layer 2 — Controller Tests

`@WebMvcTest` loads only the HTTP layer — controllers, filters, Spring Security. Does not load JPA or the database. Services are mocked via `@MockBean`.

MockMvc simulates HTTP requests without starting a real server:
- `mockMvc.perform(get("/api/ships"))` — sends a request
- `.andExpect(status().isOk())` — verifies the HTTP status
- `.andExpect(jsonPath("$.name").value("Atlantic"))` — verifies JSON

**Note on SecurityConfig:** `@WebMvcTest` also loads `SecurityConfig`, which needs `UserRepository` to build `UserDetailsService`. Each controller test class requires `@MockBean UserRepository`. A logged-in user is simulated using `@WithMockUser`.

### ShipControllerTest — 14 cases

| Test | HTTP | What it verifies |
|---|---|---|
| `getAll_authenticated` | `GET /api/ships` | 200, JSON list |
| `getAll_unauthenticated` | `GET /api/ships` | 401 (no session) |
| `getById_found` | `GET /api/ships/1` | 200, correct object |
| `getById_notFound` | `GET /api/ships/99` | 404 |
| `create_valid` | `POST /api/ships` | 201, returned object |
| `create_invalid` | `POST /api/ships` (missing fields) | 400, validation error map |
| `create_blankName` | `POST /api/ships` (`name: "   "`) | 400 — `@NotBlank` rejects whitespace-only |
| `create_zeroTonnage` | `POST /api/ships` (`tonnage: 0`) | 400 — `@Positive` rejects zero |
| `create_negativeTonnage` | `POST /api/ships` (`tonnage: -1`) | 400 — `@Positive` rejects negatives |
| `update_valid` | `PUT /api/ships/1` | 200, updated object |
| `update_notFound` | `PUT /api/ships/99` | 404 |
| `generateName_success` | `GET /api/ships/generate-name` | 200, `{"name":"..."}` |
| `generateName_unauthenticated` | `GET /api/ships/generate-name` | 401 (no session) |
| `generateName_apiError` | `GET /api/ships/generate-name` | 503 (ExternalApiException) |

### AuthControllerTest — 4 cases

Uses `@SpringBootTest` (full context) + `@MockitoBean UserRepository`. Auth logic is delegated to `AuthService`, which is loaded automatically.

| Test | What it verifies |
|---|---|
| `login_valid` | `AuthenticationManager` returns auth → 200 |
| `login_invalid` | throws `BadCredentialsException` → 401 |
| `login_blankFields` | `@Valid` rejects → 400 |
| `logout` | 200, session invalidated |

### LocationReportControllerTest — 5 cases

| Test | What it verifies |
|---|---|
| `getByShip_found` | 200, list of reports |
| `getByShip_shipNotFound` | 404 |
| `create_valid` | 201, new report |
| `create_invalid` | 400 (missing fields) |
| `create_shipNotFound` | 404 |

---

## Layer 3 — Repository Test

`H2 in-memory` — schema created automatically by Hibernate

`@DataJpaTest` loads only the JPA layer. PostgreSQL is replaced by an H2 in-memory database. Liquibase is disabled. Schema is created by Hibernate from JPA annotations.

Only custom queries are tested (standard JPA methods do not require tests).

### LocationReportRepositoryTest — 2 cases

| Test | What it verifies |
|---|---|
| `findByShipIdOrderByReportDateAsc_ordered` | reports returned in chronological order |
| `findByShipIdOrderByReportDateAsc_empty` | no reports → empty list |

---

## Required Infrastructure Changes

| Change | Purpose |
|---|---|
| Add `com.h2database:h2` (scope: test) to `pom.xml` | H2 for `@DataJpaTest` |
| New `src/test/resources/application.properties` | Disable Liquibase, configure H2 |
| New class `AppConfig.java` | `RestClient` bean for DI |
| Modify `NameGeneratorService.java` | Accept `RestClient` from outside |

---

## Summary

| File | Type | Cases |
|---|---|---|
| `ShipServiceTest` | Unit / Mockito | 7 |
| `LocationReportServiceTest` | Unit / Mockito | 4 |
| `NameGeneratorServiceTest` | Unit / Mockito | 3 |
| `AuthControllerTest` | Web slice / MockMvc | 4 |
| `ShipControllerTest` | Web slice / MockMvc | 14 |
| `LocationReportControllerTest` | Web slice / MockMvc | 5 |
| `LocationReportRepositoryTest` | JPA slice / H2 | 2 |
| `ShipTrackerBackendApplicationTests` | Integration / Spring context | 1 |
| **Total** | | **40** |
