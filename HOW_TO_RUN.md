# 🚩 How to Run — Feature Flag Management Service

## What This Project Does
A LaunchDarkly-style feature flag service. You toggle a flag in the dashboard →
Redis broadcasts the change → WebSocket pushes the update to every connected browser
**in under 100ms** without anyone refreshing the page.

The flag evaluation endpoint (`/api/flags/evaluate`) is a drop-in SDK — your app calls
it to decide whether a feature is ON or OFF for a specific user.

```
You toggle flag in UI
        ↓
Spring Boot saves to PostgreSQL
        ↓
Publishes to Redis channel "flag.updates"
        ↓
Redis Subscriber picks it up → broadcasts over WebSocket
        ↓
Every connected browser tab updates instantly ⚡
```

---

## ✅ Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| Docker Desktop | Running |

> **Ports used by this project** (see `docker-compose.yml` and `backend/src/main/resources/application.yml`):
>
> | Service | Host port |
> |---------|-----------|
> | PostgreSQL | **5434** (container 5432) |
> | Redis | **6380** (container 6379) |
> | Backend (Spring Boot) | **8090** |
> | Frontend (Vite) | **5175** |
>
> The non-default PostgreSQL/Redis ports let this project run side-by-side with other local services without conflicts.

---

## 🚀 Step-by-Step: Run the Project

### STEP 0 — Clone the Repository
```bash
git clone https://github.com/LakshmanYaganti/Feature_Flags.git
cd Feature_Flags
```

### STEP 1 — Start PostgreSQL and Redis
**Terminal 1:**
```bash
docker-compose up -d
```
Wait for both containers to show "healthy":
```bash
docker ps   # should show flags-postgres and flags-redis as "Up"
```

---

### STEP 2 — Start the Backend
**Terminal 2:**
```bash
cd backend
mvn spring-boot:run
```
Wait for: `Started FeatureFlagApplication`

**What happens on startup:** Flyway automatically creates the `feature_flags` and
`flag_evaluation_logs` tables, then seeds 4 sample flags for you to play with
(`new-checkout-flow`, `dark-mode`, `ai-recommendations`, `beta-dashboard`).

The backend listens on **http://localhost:8090** and connects to PostgreSQL on
host port **5434** and Redis on host port **6380**.

---

### STEP 3 — Start the Frontend
**Terminal 3:**
```bash
cd frontend
npm install
npm run dev
```

---

### STEP 4 — Open the Dashboard
Go to: **http://localhost:5175**

---

## 🎯 Try It Out — Demo Walkthrough

#### Toggling flags in real-time:
1. Open **two browser tabs** both pointing to `http://localhost:5175`
2. Toggle any flag in one tab
3. Watch the **other tab update instantly** — this is the Redis pub/sub → WebSocket in action
4. The "Live Events (Redis → WS)" panel shows each toggle as it arrives

#### Testing the evaluation SDK:
1. Go to the **"SDK Evaluator"** panel on the right
2. Select flag: `new-checkout-flow`
3. User ID: `user-vip-001` → result: **ENABLED** (targeted user)
4. User ID: `user-vip-001` change to `user-random-999` → result depends on 50% rollout bucket
5. Try `beta-dashboard` with User ID `user-beta-001` → always **ENABLED** (targeted)
6. Try `ai-recommendations` → always **DISABLED** (flag globally off)

#### Creating your own flag:
1. Click "New Flag"
2. Flag Key: `my-new-feature` (lowercase, hyphens only)
3. Set rollout to 25% → save
4. Test in SDK Evaluator with different user IDs — notice the same user always gets the same result

---

## 📁 Open in VS Code
File → Open Folder → select `Feature_Flags`

**Key files to understand:**
- `FlagEvaluationService.java` — the bucketing algorithm (most important)
- `FeatureFlagService.java` — Redis pub/sub publish on every flag change
- `FlagUpdateSubscriber.java` — Redis listener that forwards to WebSocket
- `FlagCard.jsx` — the toggle UI component

---

## 🛑 How to Stop
```bash
# Ctrl+C in backend and frontend terminals
docker-compose down
```

---

## 🔧 Common Errors & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Cannot connect to Redis on port 6380` | Redis container not up | `docker-compose up -d` |
| `Cannot connect to PostgreSQL on port 5434` | Postgres container not up | `docker-compose up -d`, check `docker ps` |
| `Port 5175 already in use` | Another Vite app running | Change port in `vite.config.js` |
| `Port 8090 already in use` | Another backend running | Stop it, or change `server.port` in `application.yml` |
| Flags not updating in second tab | WebSocket not connected | Refresh the page once, check the "Live via Redis" badge turns green |

---

## 🔗 Key API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | http://localhost:8090/api/flags | List all flags |
| POST | http://localhost:8090/api/flags | Create a flag |
| PUT | http://localhost:8090/api/flags/{id} | Update a flag |
| PATCH | http://localhost:8090/api/flags/{id}/toggle | Toggle on/off |
| POST | http://localhost:8090/api/flags/evaluate | Evaluate for a user |
| GET | http://localhost:8090/api/flags/{key}/logs | Evaluation history |
| GET | http://localhost:8090/api/stats | Aggregate evaluation stats |

**Evaluate endpoint example (use Postman):**
```json
POST http://localhost:8090/api/flags/evaluate
{
  "flagKey": "new-checkout-flow",
  "userId": "user-abc",
  "environment": "production"
}
```
Response:
```json
{
  "flagKey": "new-checkout-flow",
  "enabled": true,
  "reason": "PERCENTAGE_ROLLOUT",
  "userId": "user-abc",
  "environment": "production"
}
```
