<div align="center">

# 🏋️ FitTrack — AI-Powered Fitness Microservices Platform

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black" />
  <img src="https://img.shields.io/badge/Keycloak-OAuth2-4D4D4D?style=for-the-badge&logo=keycloak&logoColor=white" />
  <img src="https://img.shields.io/badge/RabbitMQ-Messaging-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" />
  <img src="https://img.shields.io/badge/MongoDB-NoSQL-47A248?style=for-the-badge&logo=mongodb&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-User%20DB-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
</p>

<p align="center">
  A production-grade, cloud-native fitness tracking application built on a <strong>Spring Boot microservices architecture</strong>. Track your workouts, get intelligent AI-generated recommendations, and manage everything through a sleek React frontend — all secured with Keycloak SSO.
</p>

</div>

---

## 📋 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Services](#-services)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
- [Service Ports](#-service-ports)
- [API Reference](#-api-reference)
- [How It Works](#-how-it-works)

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                     React Frontend                       │
│                  (Vite + Material UI)                    │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP (port 8080)
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  API Gateway (Spring Cloud)              │
│         JWT Validation · Routing · User Sync Filter      │
└──────┬────────────────┬────────────────┬────────────────┘
       │                │                │
       ▼                ▼                ▼
┌────────────┐  ┌──────────────┐  ┌────────────────┐
│  User      │  │  Activity    │  │   AI Service   │
│  Service   │  │  Service     │  │  (Recommend.)  │
│ (Port 8081)│  │ (Port 8082)  │  │  (Port 8083)   │
│ PostgreSQL │  │   MongoDB    │  │    MongoDB     │
└────────────┘  └──────┬───────┘  └───────▲────────┘
                       │                  │
                       │   RabbitMQ       │
                       └──────────────────┘
                    (fitness-exchange / activity.queue)

     ┌──────────────────────────────────────────┐
     │  Eureka Server (Service Discovery :8761) │
     │  Config Server (Centralized Config :8888)│
     │  Keycloak (Identity Provider      :8181) │
     └──────────────────────────────────────────┘
```

---

## 🧩 Services

### 1. 🔍 Eureka Server (`/eureka`)
- **Service Discovery** for all microservices
- All services register themselves on startup
- API Gateway uses Eureka for load-balanced routing (`lb://SERVICE-NAME`)

### 2. ⚙️ Config Server (`/configserver`)
- **Centralized configuration** management using Spring Cloud Config
- Serves YAML configurations to all services at startup
- Change config once, all services pick it up on restart

### 3. 🚪 API Gateway (`/gateway`)
- Single entry point for all client requests
- **JWT token validation** via Keycloak's JWK Set URI
- **`KeycloakUserSyncFilter`** — extracts user info from the JWT and auto-syncs it to `userservice` on every request
- Routes requests to the appropriate downstream service using Eureka

### 4. 👤 User Service (`/userservice`)
- Manages user profiles stored in **PostgreSQL**
- Handles Keycloak ID synchronization
- Exposes a `/api/users/{userId}/validate` endpoint used by other services

### 5. 🏃 Activity Service (`/activityservice`)
- Tracks fitness activities (runs, cycling, swimming, etc.) in **MongoDB**
- Validates users by calling `userservice` before saving
- Publishes activity events to **RabbitMQ** for async AI processing

### 6. 🤖 AI Service (`/aiservice`)
- Consumes activity events from **RabbitMQ**
- Generates personalized workout recommendations using an **LLM**
- Stores recommendations in **MongoDB**
- Exposes REST endpoints to retrieve recommendations by user or activity ID

### 7. 💻 Frontend (`/fitness-app-frontend`)
- Built with **React + Vite** and styled with **Material UI**
- Integrates with **Keycloak JS** for SSO authentication
- Displays activity history and AI-generated recommendations

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Spring Boot 4.1.0, Spring Cloud |
| **Service Discovery** | Netflix Eureka |
| **API Gateway** | Spring Cloud Gateway (WebFlux) |
| **Authentication** | Keycloak (OAuth2 / OpenID Connect) |
| **Async Messaging** | RabbitMQ |
| **User Data Store** | PostgreSQL (via Spring Data JPA) |
| **Activity & AI Store** | MongoDB (via Spring Data MongoDB) |
| **AI / LLM** | Configurable (Hugging Face / Ollama) |
| **Frontend** | React 18, Vite, Material UI, Axios |
| **Build Tool** | Maven (Maven Wrapper) |

---

## ✅ Prerequisites

Ensure the following are installed and running before starting:

- **Java 21+**
- **Maven** (or use the included `mvnw` wrappers)
- **Node.js 18+** and npm
- **Docker** (recommended for infrastructure)
- **PostgreSQL** — running on `localhost:5432`
- **MongoDB** — running on `localhost:27017`
- **RabbitMQ** — running on `localhost:5672` (default guest/guest)
- **Keycloak** — running on `localhost:8181`

> **Quick Infrastructure Setup with Docker:**
> ```bash
> docker run -d --name postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres
> docker run -d --name mongo -p 27017:27017 mongo
> docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
> docker run -d --name keycloak -p 8181:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:latest start-dev
> ```

---

## 🐳 Docker Compose (Recommended)

Run the **entire stack** — all services + infrastructure — with a single command.

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### Setup
```bash
# 1. Copy the environment file and add your Hugging Face token
cp .env.example .env
# Edit .env and set HUGGINGFACE_API_KEY=hf_your_token_here

# 2. Build and start all containers
docker compose up --build

# 3. Check all containers are healthy
docker compose ps
```

| Service | URL |
|---|---|
| React Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| RabbitMQ Management | http://localhost:15672 |
| Keycloak Admin | http://localhost:8181 |

> **Note on Keycloak**: After first boot, open the Keycloak Admin UI at `http://localhost:8181` (admin/admin) and recreate your realm and client configuration.

```bash
# Stop all containers
docker compose down

# Stop and remove all data volumes (full reset)
docker compose down -v
```

---

## 🚀 Getting Started (Local Development)

Start each service **in order**. Each service depends on the ones before it.

### Step 1 — Eureka Server
```bash
cd eureka
./mvnw spring-boot:run
```

### Step 2 — Config Server
```bash
cd configserver
./mvnw spring-boot:run
```

### Step 3 — API Gateway
```bash
cd gateway
./mvnw spring-boot:run
```

### Step 4 — User Service
```bash
cd userservice
./mvnw spring-boot:run
```

### Step 5 — Activity Service
```bash
cd activityservice
./mvnw spring-boot:run
```

### Step 6 — AI Service

> Configure your LLM provider in `configserver/src/main/resources/config/ai-service.yml` before starting.
> Set your API token in the `huggingface.api.key` field (or configure Ollama for local inference).

```bash
cd aiservice
./mvnw spring-boot:run
```

### Step 7 — Frontend
```bash
cd fitness-app-frontend
npm install
npm run dev
```

Open your browser at **`http://localhost:5173`**

---

## 🔌 Service Ports

| Service | Port |
|---|---|
| React Frontend | `5173` |
| API Gateway | `8080` |
| User Service | `8081` |
| Activity Service | `8082` |
| AI Service | `8083` |
| Eureka Dashboard | `8761` |
| Config Server | `8888` |
| Keycloak | `8181` |
| RabbitMQ Management | `15672` |

---

## 📡 API Reference

All requests go through the **API Gateway at `http://localhost:8080`**. Include the Keycloak JWT in the `Authorization: Bearer <token>` header.

### Activities
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/activities` | Log a new fitness activity |
| `GET` | `/api/activities` | Get all activities for the logged-in user |
| `GET` | `/api/activities/{id}` | Get a specific activity by ID |

### AI Recommendations
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/recommendations/activity/{activityId}` | Get AI recommendation for a specific activity |
| `GET` | `/api/recommendations/user/{userId}` | Get all recommendations for a user |

### Users
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/users/{userId}/validate` | Validate if a user exists (internal use) |

---

## ⚡ How It Works

### Authentication Flow
1. User logs in via the React frontend using **Keycloak JS**.
2. Keycloak issues a **JWT access token**.
3. Frontend stores the token and sends it with every API request.
4. The **API Gateway validates** the JWT signature against Keycloak's JWK Set.
5. A custom `KeycloakUserSyncFilter` extracts the user's `sub` (Keycloak ID), `email`, and `name` from the token and upserts the user record in `userservice`.

### Activity & AI Recommendation Flow
```
User submits activity via frontend
          │
          ▼
API Gateway → Activity Service
          │
          ├── 1. Validates user exists (calls User Service)
          ├── 2. Saves activity to MongoDB
          └── 3. Publishes event to RabbitMQ (fitness-exchange)
                        │
                        ▼
               AI Service (RabbitMQ Consumer)
                        │
                        ├── 4. Receives activity event
                        ├── 5. Builds a structured prompt
                        ├── 6. Calls LLM API to generate recommendation
                        └── 7. Saves recommendation to MongoDB

User opens Activity Detail page
          │
          ▼
Frontend → GET /api/recommendations/activity/{id}
          │
          ▼
  Returns saved AI Recommendation ✅
```

---

<div align="center">
  <p>Built with ☕ Java, ⚛️ React, and a lot of debugging.</p>
</div>
