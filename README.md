# Feature Flag Management Service (LaunchDarkly-lite)

A production-grade feature flag service with **Redis pub/sub**, **real-time WebSocket** updates, and **consistent user bucketing**.

## Architecture

```
React Dashboard (5175)
     ↓ REST          WebSocket ←──────────────────┐
Spring Boot (8090)                                 │
     ↓ DB writes    Redis pub (flag.updates) ──────┘
  PostgreSQL          Redis sub → WS broadcast
```

## Key Features

- **Consistent bucketing** — `hash(flagKey + userId) % 100` ensures the same user always gets the same experience
- **Targeted users** — specific user IDs always get the flag regardless of rollout %
- **Environment targeting** — per-flag on/off for production / staging / development
- **Real-time updates** — flag changes publish to Redis → Redis subscriber broadcasts to WebSocket → all dashboards update instantly
- **Evaluation logging** — every SDK call logged with reason code
- **SDK-style endpoint** — `POST /api/flags/evaluate` is the drop-in SDK endpoint

## Quick Start

```bash
docker-compose up -d
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```
Open http://localhost:5175

## Evaluation Reasons

| Reason             | Meaning                                        |
|--------------------|------------------------------------------------|
| `TARGETED_USER`    | userId in the targeted list → always true      |
| `FULL_ROLLOUT`     | rolloutPercentage = 100                        |
| `PERCENTAGE_ROLLOUT`| hash bucket < rollout %                       |
| `FLAG_DISABLED`    | Global enabled = false                         |
| `ENV_DISABLED`     | Disabled for this environment                  |
| `ZERO_ROLLOUT`     | rolloutPercentage = 0                          |
| `FLAG_NOT_FOUND`   | Flag key doesn't exist                         |

## API

| Method | Endpoint                  | Description                    |
|--------|---------------------------|--------------------------------|
| GET    | /api/flags                | List all flags                 |
| POST   | /api/flags                | Create flag                    |
| PUT    | /api/flags/{id}           | Update flag                    |
| PATCH  | /api/flags/{id}/toggle    | Toggle on/off                  |
| POST   | /api/flags/evaluate       | Evaluate flag for a user       |
| GET    | /api/flags/{key}/logs     | Evaluation history             |
| GET    | /api/stats                | Aggregate evaluation stats     |
