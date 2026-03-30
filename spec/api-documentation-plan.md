# ShipTracker — Swagger / OpenAPI Plan

---

## Introduction

We use `springdoc-openapi` — it integrates OpenAPI 3 with Spring Boot 3 and provides a built-in Swagger UI.

After implementation, the documentation is available at:
- `http://localhost:8080/swagger-ui.html` — interactive UI
- `http://localhost:8080/v3/api-docs` — raw JSON (OpenAPI spec)

---

## STAGE 1: Maven Dependency

### Step 1.1 — Add to `pom.xml`

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.3</version>
</dependency>
```

Version `2.x` is required for Spring Boot 3.x — `1.x` only works with Boot 2.x. This single dependency is enough for Swagger UI to be available — springdoc scans controllers and generates the spec automatically at application startup.

### Definition of Done:
- `./mvnw compile` completes without errors (dependency downloaded and visible)

> Swagger UI will not yet be accessible — Spring Security blocks these paths until Stage 3.

---

## STAGE 2: OpenAPI Configuration

### Step 2.1 — Create `OpenApiConfig.java`

New file: `src/main/java/com/shiptracker/config/OpenApiConfig.java`

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

Two things to clarify:

**`APIKEY in COOKIE` scheme** — OpenAPI 3 has no dedicated type for HTTP sessions. The closest equivalent is an `apiKey` sent in a cookie. We define it under the name `cookieAuth` and provide the cookie name — `JSESSIONID`. Swagger UI will display a padlock on each endpoint and allow entering a session value to test protected endpoints without `curl -b`.

**`addSecurityItem`** — applies the `cookieAuth` scheme globally to all endpoints. Without this, the padlock would be visible but no endpoint would be marked as requiring authorisation. Public endpoints (login, logout) will be overridden individually with the `@SecurityRequirements({})` annotation in Stage 4.

### Definition of Done:
- `./mvnw compile` completes without errors (`OpenAPI` bean registered)

> Verification in Swagger UI (title, padlock) is only possible after Stage 3.

---

## STAGE 3: Allow Access in Spring Security

### Step 3.1 — Update `SecurityConfig.java`

springdoc registers its resources under paths `/swagger-ui/**` and `/v3/api-docs/**` — these are the library's defaults, unchanged. The current `SecurityConfig` blocks everything except `/api/auth/login`, so without this change, accessing `/swagger-ui.html` will return 401.

Extend the `requestMatchers` in the `filterChain` method:

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

### Definition of Done:
- `http://localhost:8080/swagger-ui.html` accessible without logging in
- `http://localhost:8080/api/ships` still returns 401 without a session

---

## STAGE 4: Controller Annotations

springdoc automatically detects endpoints and generates the spec.

`@SecurityRequirements({})` on the `login` and `logout` methods removes the padlock from those endpoints — they are public and do not require a session.

### Step 4.1 — `ShipController.java`

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

### Step 4.2 — `LocationReportController.java`

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

### Step 4.3 — `AuthController.java`

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

### Definition of Done:
- Swagger UI shows 3 groups: "Ships", "Location Reports", "Authentication"
- Each endpoint has a summary and documented response codes
- The `login` and `logout` endpoints have no padlock

---

## STAGE 5: `@Schema` Annotations on DTOs

`@Schema` with example values fills in the request body in Swagger UI when using "Try it out" — without them, data must be entered manually. The annotation is placed directly on record parameters, before validation annotations.

`ShipResponse` and `LocationReportResponse` are skipped — response DTOs are not edited by the user, and the field names are self-explanatory.

### Step 5.1 — `ShipRequest.java`

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

### Step 5.2 — `LocationReportRequest.java`

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

### Step 5.3 — `LoginRequest.java`

```java
public record LoginRequest(
    @Schema(description = "Username", example = "admin")
    @NotBlank String username,

    @Schema(description = "Password", example = "admin123")
    @NotBlank String password
) {}
```

### Definition of Done:
- In the "Schemas" section of the UI, fields have descriptions and examples
- "Try it out" on endpoints with a request body auto-fills data

---

## Summary of Changes

| File | Change |
|------|--------|
| `pom.xml` | +1 dependency: `springdoc-openapi-starter-webmvc-ui` |
| `SecurityConfig.java` | +3 paths in `permitAll` |
| `OpenApiConfig.java` | New file — title, version, cookie session scheme |
| `ShipController.java` | `@Tag` + `@Operation` + `@ApiResponse` on 5 endpoints |
| `LocationReportController.java` | `@Tag` + `@Operation` + `@ApiResponse` on 2 endpoints |
| `AuthController.java` | `@Tag` + `@Operation` + `@ApiResponse` + `@SecurityRequirements` on 3 endpoints |
| `ShipRequest.java` | `@Schema` on 4 fields |
| `LocationReportRequest.java` | `@Schema` on 3 fields |
| `LoginRequest.java` | `@Schema` on 2 fields |

Total: **9 files**, no changes to business logic.

---

## Final Verification

```bash
./mvnw spring-boot:run
```

1. Go to `http://localhost:8080/swagger-ui.html`
2. Three sections are visible: Ships, Location Reports, Authentication
3. POST `/api/auth/login` → "Try it out" → Execute → 200 OK, `JSESSIONID` cookie in the response
4. GET `/api/ships` without authorisation → Execute → 401 Unauthorized
5. GET `/api/ships` with a session → 200, list of ships
