# Ship Tracker

A web application for managing ships and reporting their voyages.

> Planning documentation in `/spec` is in Polish (working language).

## Requirements

- Java 21
- Maven
- Docker
- Node.js 20+
- Angular CLI

## Configuration

The ship name generator uses the [randommer.io](https://randommer.io) API.

Copy `.env.example` to `.env` and set your API key (free registration):
```bash
cp .env.example .env
```

```
RANDOMMER_API_KEY=your_key_here
```

Without the key the application starts normally, but the "Generate name" button returns a 503 error.

## Running the application

### 1. Database
```bash
docker compose up -d
```

### 2. Backend
```bash
cd ship-tracker-backend
./mvnw spring-boot:run
```

### 3. Frontend
```bash
cd ship-tracker-frontend
npm install
ng serve
```

Application available at: http://localhost:4200

## Login credentials

| Username | Password |
|----------|----------|
| admin    | admin123 |

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.4, Java 21 |
| Security | Spring Security (session-based) |
| ORM | Spring Data JPA, Hibernate |
| Database | PostgreSQL 16 (Docker) |
| Migrations | Liquibase |
| Frontend | Angular, Angular Material |
