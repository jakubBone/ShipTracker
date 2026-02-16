# ShipTracker — Plan Swagger / OpenAPI

---

## Wstęp

Używamy `springdoc-openapi` — integruje OpenAPI 3 ze Spring Boot 3 i dostarcza wbudowany Swagger UI. 

Po implementacji dokumentacja dostępna pod:
- `http://localhost:8080/swagger-ui.html` — interaktywny UI
- `http://localhost:8080/v3/api-docs` — surowy JSON (OpenAPI spec)

---

## ETAP 1: Zależność Maven

### Krok 1.1 — Dodaj do `pom.xml`

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.3</version>
</dependency>
```

Wersja `2.x` wymagana dla Spring Boot 3.x — `1.x` działa tylko z Boot 2.x. Sama ta zależność wystarczy żeby Swagger UI było dostępne — springdoc skanuje kontrolery i generuje spec automatycznie przy starcie aplikacji.

### Definicja "done":
- `./mvnw compile` kończy się bez błędów (zależność pobrana i widoczna)

> Swagger UI nie będzie jeszcze dostępne — Spring Security blokuje te ścieżki do czasu Etapu 3.

---

## ETAP 2: Konfiguracja OpenAPI

### Krok 2.1 — Stwórz `OpenApiConfig.java`

Nowy plik: `src/main/java/com/shiptracker/config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ShipTracker API")
                .description("REST API for managing ships and their location reports")
                .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
            .components(new Components()
                .addSecuritySchemes("cookieAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")
                        .description("Session cookie obtained after login")));
    }
}
```

Dwie rzeczy do wyjaśnienia:

**Schemat `APIKEY in COOKIE`** — OpenAPI 3 nie ma dedykowanego typu dla sesji HTTP. Najbliższy odpowiednik to `apiKey` przesyłany w ciasteczku. Definiujemy go pod nazwą `cookieAuth` i podajemy nazwę ciasteczka — `JSESSIONID`. Swagger UI wyświetli kłódkę przy każdym endpoincie i pozwoli wpisać wartość sesji, żeby testować chronionych endpointów bez `curl -b`.

**`addSecurityItem`** — aplikuje schemat `cookieAuth` globalnie na wszystkie endpointy. Bez tego kłódka byłaby widoczna, ale żaden endpoint nie byłby oznaczony jako wymagający autoryzacji. Endpointy publiczne (login, logout) nadpiszemy indywidualnie adnotacją `@SecurityRequirements({})` w Etapie 4.

### Definicja "done":
- `./mvnw compile` kończy się bez błędów (bean `OpenAPI` zarejestrowany)

> Weryfikacja w Swagger UI (tytuł, kłódka) możliwa dopiero po Etapie 3.

---

## ETAP 3: Zezwolenie w Spring Security

### Krok 3.1 — Zaktualizuj `SecurityConfig.java`

springdoc rejestruje swoje zasoby pod ścieżkami `/swagger-ui/**` i `/v3/api-docs/**` — to domyślne, niezmienione ścieżki biblioteki. Aktualny `SecurityConfig` blokuje wszystko poza `/api/auth/login`, więc bez tej zmiany wejście na `/swagger-ui.html` zwróci 401.

Rozszerz `requestMatchers` w metodzie `filterChain`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/login").permitAll()
    .requestMatchers(
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**"
    ).permitAll()
    .anyRequest().authenticated())
```

### Definicja "done":
- `http://localhost:8080/swagger-ui.html` dostępny bez logowania
- `http://localhost:8080/api/ships` nadal zwraca 401 bez sesji

---

## ETAP 4: Adnotacje na kontrolerach

springdoc automatycznie wykrywa endpointy i generuje spec. 

`@SecurityRequirements({})` na metodach `login` i `logout` usuwa kłódkę przy tych endpointach — są publiczne i nie wymagają sesji.

### Krok 4.1 — `ShipController.java`

```
@Tag(name = "Ships")

getAll()       → @Operation("Get all ships")
                 @ApiResponse(200, "List of ships")
                 @ApiResponse(401, "Not authenticated")

getById()      → @Operation("Get ship by ID")
                 @ApiResponse(200, "Ship found")
                 @ApiResponse(401, "Not authenticated")
                 @ApiResponse(404, "Ship not found")

create()       → @Operation("Create a new ship")
                 @ApiResponse(201, "Ship created")
                 @ApiResponse(400, "Validation error")
                 @ApiResponse(401, "Not authenticated")

update()       → @Operation("Update ship")
                 @ApiResponse(200, "Ship updated")
                 @ApiResponse(400, "Validation error")
                 @ApiResponse(401, "Not authenticated")
                 @ApiResponse(404, "Ship not found")

generateName() → @Operation("Generate a random ship name")
                 @ApiResponse(200, "Generated name")
                 @ApiResponse(401, "Not authenticated")
                 @ApiResponse(503, "External API unavailable")
```

### Krok 4.2 — `LocationReportController.java`

```
@Tag(name = "Location Reports")

getByShip() → @Operation("Get location reports for a ship")
               @ApiResponse(200, "List of reports")
               @ApiResponse(401, "Not authenticated")
               @ApiResponse(404, "Ship not found")

create()    → @Operation("Add a location report for a ship")
               @ApiResponse(201, "Report created")
               @ApiResponse(400, "Validation error")
               @ApiResponse(401, "Not authenticated")
               @ApiResponse(404, "Ship not found")
```

### Krok 4.3 — `AuthController.java`

```
@Tag(name = "Authentication")

login()  → @Operation("Login and start session")
            @SecurityRequirements({})
            @ApiResponse(200, "Login successful, JSESSIONID cookie set")
            @ApiResponse(400, "Blank username or password")
            @ApiResponse(401, "Invalid credentials")

logout() → @Operation("Logout and invalidate session")
            @SecurityRequirements({})
            @ApiResponse(200, "Logged out successfully")

me()     → @Operation("Get currently logged-in username")
            @ApiResponse(200, "Username of authenticated user")
            @ApiResponse(401, "Not authenticated")
```

### Definicja "done":
- W Swagger UI widoczne są 3 grupy: „Ships", „Location Reports", „Authentication"
- Każdy endpoint ma summary i udokumentowane kody odpowiedzi
- Endpointy `login` i `logout` nie mają kłódki

---

## ETAP 5: Adnotacje @Schema na DTO

`@Schema` z przykładowymi wartościami wypełnia ciało requestu w Swagger UI przy „Try it out" — bez nich trzeba wpisywać dane ręcznie. Adnotację umieszczamy bezpośrednio na parametrach rekordu, przed adnotacjami walidacyjnymi.

`ShipResponse` i `LocationReportResponse` pomijamy — response DTO nie są edytowane przez użytkownika, a nazwy pól są wystarczająco czytelne.

### Krok 5.1 — `ShipRequest.java`

```java
public record ShipRequest(
    @Schema(description = "Ship name", example = "Atlantic Pioneer")
    @NotBlank String name,

    @Schema(description = "Launch date", example = "2015-06-20")
    @NotNull LocalDate launchDate,

    @Schema(description = "Ship type", example = "Cargo")
    @NotBlank String shipType,

    @Schema(description = "Tonnage in metric tons", example = "75000.00")
    @NotNull @Positive BigDecimal tonnage
) {}
```

### Krok 5.2 — `LocationReportRequest.java`

```java
public record LocationReportRequest(
    @Schema(description = "Date of the location report", example = "2024-03-15")
    @NotNull LocalDate reportDate,

    @Schema(description = "Country where the ship was located", example = "Poland")
    @NotBlank String country,

    @Schema(description = "Port name", example = "Gdańsk")
    @NotBlank String port
) {}
```

### Krok 5.3 — `LoginRequest.java`

```java
public record LoginRequest(
    @Schema(description = "Username", example = "admin")
    @NotBlank String username,

    @Schema(description = "Password", example = "admin123")
    @NotBlank String password
) {}
```

### Definicja "done":
- W sekcji „Schemas" w UI pola mają opisy i przykłady
- „Try it out" na endpointach z request body wypełnia dane automatycznie

---

## Podsumowanie zmian

| Plik | Zmiana |
|------|--------|
| `pom.xml` | +1 zależność: `springdoc-openapi-starter-webmvc-ui` |
| `SecurityConfig.java` | +3 ścieżki w `permitAll` |
| `OpenApiConfig.java` | Nowy plik — tytuł, wersja, schemat sesji cookie |
| `ShipController.java` | `@Tag` + `@Operation` + `@ApiResponse` na 5 endpointach |
| `LocationReportController.java` | `@Tag` + `@Operation` + `@ApiResponse` na 2 endpointach |
| `AuthController.java` | `@Tag` + `@Operation` + `@ApiResponse` + `@SecurityRequirements` na 3 endpointach |
| `ShipRequest.java` | `@Schema` na 4 polach |
| `LocationReportRequest.java` | `@Schema` na 3 polach |
| `LoginRequest.java` | `@Schema` na 2 polach |

Łącznie: **9 plików**, brak zmian w logice biznesowej.

---

## Weryfikacja końcowa

```bash
./mvnw spring-boot:run
```

1. Wejdź na `http://localhost:8080/swagger-ui.html`
2. Widoczne są 3 sekcje: Ships, Location Reports, Authentication
3. POST `/api/auth/login` → „Try it out" → Execute → 200 OK, cookie `JSESSIONID` w odpowiedzi
4. GET `/api/ships` bez autoryzacji → Execute → 401 Unauthorized
5. GET `/api/ships` z sesją → 200, lista statków