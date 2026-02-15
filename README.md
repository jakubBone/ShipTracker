# 🚢 Ship Tracker 

![Ship Tracker](ship-tracker-frontend/public/logo.jpg)

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21-red.svg)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)

A web application for managing ships and reporting their voyages.

## ✨ Features

- **Ship management** - add, edit, and browse the fleet with a data table
- **Voyage reporting** - log location entries (date, country, port) per ship; entries are immutable once saved
- **Voyage timeline** - chronological view of a ship's position history
- **Name generator** - generate a ship name via external API with one click
- **Session-based auth** - protected routes, login/logout flow

---

## 🚀 Quick Start

**Requirements:** Docker

Get a free API key at [randommer.io](https://randommer.io), copy `.env.example` to `.env` and set:

```
RANDOMMER_API_KEY=your_key_here
```

Then:

```bash
docker compose up
```

Open **http://localhost:4200** - login: `admin` / `admin123`

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.4, Java 21 |
| Security | Spring Security (session-based) |
| ORM | Spring Data JPA, Hibernate |
| Database | PostgreSQL 16 (Docker) |
| Migrations | Liquibase |
| Frontend | Angular, Angular Material |

---

## 🤖 Development Approach

> Built with [Claude Code](https://claude.ai/code): AI-assisted pair programming, spec-first workflow
> 
> Note: The planning docs in `spec/` are in Polish, which was the working language for this project.

Backend is my stronger side. Frontend is an area I'm currently developing.

This project was a challenge to step outside my comfort zone.
I used Claude Code (Sonnet 4.5) to speed up development while actively reviewing and controlling its output.

| Step | What happened                                                                                                                                                                                                                                 |
|------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 📋 **Spec first** | I wrote the requirements: scope, business rules, edge cases, acceptance criteria. I had it reviewed by two models (Claude + Gemini), evaluated both responses, merged the insights, and updated the spec myself. That became `spec/Brief.md`. |
| 📌 **Implementation plan** | I drafted `spec/Plan_Implementacji.md` with Claude Code and reviewed it with Gemini, then finalised: staged plan, definition of done per stage, test coverage requirements in `spec/Plan_Testow_Backend.md`.                                 |
| 👨‍💻 **Pair programming** | I used Claude Code to implement large parts of the code. I read and reviewed every generated file before moving on - that's how I caught missing test cases in the first backend pass and fixed them.                                         |
| 📝 **Living spec** | When better approaches came up during development, even if they differed from the plan, I updated the spec accordingly.                                                                                                                       |
| 📚 **Learn, don't delegate** | When I didn't fully understand something, I asked for explanations and examples, and saved the reasoning locally as learning notes, so I'd actually learn it.                                                                                 |

---

## 📁 Project Structure

```
ship-tracker/
├── docker-compose.yml
├── spec/                          # Planning docs (Brief, Implementation Plan, Test Plan)
├── ship-tracker-backend/
│   └── src/main/java/com/shiptracker/
│       ├── config/                # Security, app configuration
│       ├── controller/            # REST controllers (Auth, Ships, Reports)
│       ├── service/               # Business logic
│       ├── repository/            # Spring Data JPA repositories
│       ├── entity/                # JPA entities (Ship, LocationReport, User)
│       ├── dto/                   # Request/Response records
│       └── exception/             # Global exception handler
└── ship-tracker-frontend/
    └── src/app/
        ├── core/                  # Services, models, guards, interceptors
        └── features/
            ├── auth/              # Login page
            └── ships/             # Ship list, ship form, ship detail
                └── ship-detail/   # Location report form, voyage timeline
```
