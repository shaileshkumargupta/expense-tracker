# Autonomous Financial Copilot & Expense Intelligence Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.2-red.svg)](https://redis.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20%2F%20Swagger-green.svg)](http://localhost:8080/swagger-ui.html)
[![AI](https://img.shields.io/badge/Google%20Gemini-v1beta%20Tools-blueviolet.svg)](https://ai.google.dev/)

An enterprise-ready, production-grade financial copilot engineered with **Spring Boot 3**, **Redis 7.2**, **MySQL 8**, and **Google Gemini AI Function Calling**. The platform empowers users to manage budgets, record multi-category transactions, query real-time analytics, and interact with a contextual AI agent capable of deterministic ledger tool execution.

---

## 🏗️ System Architecture

```text
                               +-------------------------------------------------+
                               |           Client (Web / Mobile / Curl)          |
                               +-------------------------------------------------+
                                                        |
                                                        |  HTTPS / Bearer JWT
                                                        v
                               +-------------------------------------------------+
                               |       Spring Security Filter Chain              |
                               |  - JwtFilter (Extracts & Validates Principal)   |
                               +-------------------------------------------------+
                                                        |
                                                        v
                               +-------------------------------------------------+
                               |      AiRateLimiterFilter (OncePerRequest)       |
                               |  - Sliding-Window Redis Rate Limiter            |
                               |  - 10 requests / min / user (HTTP 429 Fail-Safe)|
                               +-------------------------------------------------+
                                                        |
                         +------------------------------+------------------------------+
                         |                                                             |
                         v                                                             v
        +---------------------------------+                           +---------------------------------+
        |         REST Controllers        |                           |       GraphQL Controllers       |
        |  - AiAssistantController        |                           |  - ExpenseGraphQLController     |
        |  - Expense / Income Controllers |                           |  - Query / Mutation Resolvers   |
        |  - Budget / Report Controllers  |                           +---------------------------------+
        +---------------------------------+                                            |
                         |                                                             |
                         +------------------------------+------------------------------+
                                                        |
                                                        v
                               +-------------------------------------------------+
                               |             Business Services Layer             |
                               |  - AiAssistantService & FinancialToolsCallback  |
                               |  - ExpenseService & IncomeService               |
                               |  - BudgetService & ReportService                |
                               |  - RecurringExpenseScheduler (@Scheduled)       |
                               +-------------------------------------------------+
                                       |                |                |
             +-------------------------+                |                +-------------------------+
             |                                          |                                          |
             v                                          v                                          v
+--------------------------+       +--------------------------+       +------------------------------------+
|  Redis 7.2 In-Memory DB  |       |     MySQL 8 Relational   |       |       Google Gemini API (v1beta)   |
|  - Cache-Aside Reports   |       |     ACID Ledger DB       |       |  - Model: gemini-3.5-flash-lite    |
|  - 10-Min Cache Eviction |       |  - Users & Categories    |       |  - 10 Deterministic Tools:         |
|  - 30-Min Session Memory |       |  - Expenses & Incomes    |       |    * recordExpense, getAllExpenses |
|  - Sliding-Window Counter|       |  - Budgets & Recurrings  |       |    * recordIncome, setBudget, etc. |
+--------------------------+       +--------------------------+       +------------------------------------+
```

---

## ⚡ Core Engineering Features

### 1. Deterministic AI Ledger Tools (Google Gemini Function Calling)
- Integrates Google Gemini (`gemini-3.5-flash-lite`) via REST communication (`RestClient`).
- Model invokes **10 registered financial tools** (`FinancialToolsCallback`) spanning:
  - **Expense**: `recordExpense`, `getAllExpenses` (with month/year filtering & category breakdown).
  - **Income**: `recordIncome`, `getAllIncomes`.
  - **Budget**: `setBudget`, `checkBudgetStatus`.
  - **Category**: `getAllCategories`, `addCategory`.
  - **Overview**: `getDashboardOverview`, `getMonthlySummary`.
- Implements an automated local regex NLP fallback if external LLM quotas or connectivity issues occur.

### 2. Redis Cache-Aside Architecture
- Configured via `@EnableCaching` with standardized, non-deprecated `RedisSerializer.json()` and UTF-8 key serialization.
- `@Cacheable` applied on heavy analytics:
  - `ReportService.getMonthlyReport` (TTL: 10 minutes, isolated per user).
  - `DashboardService.getDashBoard` (TTL: 10 minutes).
- `@CacheEvict` automatically invalidates cached analytics upon new transaction mutations (`addExpense`, `addIncome`, `deleteExpense`).

### 3. Sliding-Window Rate Limiting (`AiRateLimiterFilter`)
- Protects AI endpoints (`/api/v1/ai/**`) from abuse with **10 requests per minute** per authenticated user ID (or client IP).
- Returns **HTTP 429 Too Many Requests** with informative JSON payload and `Retry-After: 60` headers.
- Employs a fail-safe strategy: Redis outages allow requests through gracefully without breaking core services.

### 4. Stateful Multi-Turn Session Memory
- Retains conversational context across the last 20 messages (10 conversation turns).
- Managed through Redis Lists (`chat:history:{email}`) with a 30-minute sliding expiration on each interaction.
- Supports conversational resets via `DELETE /api/v1/ai/history`.

### 5. Dual API Paradigms: REST & GraphQL
- Full RESTful surface with hypermedia-ready JSON payloads.
- Declarative GraphQL schema (`schema.graphqls`, `expense.graphqls`) enabling precise, client-tailored data fetching without over-fetching.

### 6. Automated Background Scheduling
- Background runner (`@Scheduled`) periodically processes automated recurring expenses (e.g., monthly rent, utilities, subscriptions).

---

## 📖 Interactive Documentation (OpenAPI 3.0 / Swagger UI)

The API is fully documented using **Springdoc OpenAPI 3.0** with global JWT Bearer Authentication.

- **Swagger UI Console**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Spec**: `http://localhost:8080/v3/api-docs`

> **Authorizing in Swagger UI**: Click the green **Authorize 🔓** button in the top right, paste `Bearer <YOUR_JWT_TOKEN>`, and all requests will automatically include the authenticated header.

---

## 🚀 Getting Started & Quick Run

### Prerequisites
- **Java 21** (`openjdk-21`)
- **Docker & Docker Compose**
- **Maven** (bundled `./mvnw`)

### Step 1: Start Infrastructure (MySQL & Redis)
Use the included `docker-compose.yml` to launch isolated MySQL 8 and Redis 7.2 containers:

```bash
docker compose up -d
```

Verify services are healthy:
```bash
docker compose ps
```

### Step 2: Configure Environment Variables
You can pass your Google Gemini API key via environment variable or update `src/main/resources/application.properties`:

```bash
export GEMINI_API_KEY="your_actual_gemini_api_key_here"
```

*Default application properties:*
```properties
# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/expense_tracker
spring.datasource.username=root
spring.datasource.password=toor

# Gemini
gemini.api.key=${GEMINI_API_KEY:your_gemini_api_key_here}
gemini.model=gemini-3.5-flash-lite
```

### Step 3: Run the Application
Launch the Spring Boot service:

```bash
JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64 ./mvnw spring-boot:run
```

The application boots on port `8080`.

---

## 🧪 Testing & Verification Guide

### 1. Register & Obtain JWT Token
```bash
# Register User
curl -X POST 'http://localhost:8080/users/registerUser' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "shai",
    "emailId": "shai@gmail.com",
    "password": "Password123!"
  }'

# Login to retrieve Bearer Token
curl -X POST 'http://localhost:8080/users/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "emailId": "shai@gmail.com",
    "password": "Password123!"
  }'
```
*Save the returned `token` from the JSON response.*

---

### 2. Test AI Financial Assistant (`/api/v1/ai/chat`)

#### Natural Language Expense Recording:
```bash
curl -X POST 'http://localhost:8080/api/v1/ai/chat' \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"message": "I spent 40 rupees on chocolates after lunch"}'
```

#### Querying Monthly Summary:
```bash
curl -X POST 'http://localhost:8080/api/v1/ai/chat' \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"message": "Can you give me all expenses of this month?"}'
```

#### Multi-Turn Follow-Up (Demonstrating Redis Memory):
```bash
curl -X POST 'http://localhost:8080/api/v1/ai/chat' \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"message": "How much of my budget is remaining after that?"}'
```

---

### 3. Verify Rate Limiter (HTTP 429)
Send 11 rapid requests within 60 seconds:
```bash
for i in {1..11}; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST 'http://localhost:8080/api/v1/ai/chat' \
    -H 'Authorization: Bearer <TOKEN>' \
    -H 'Content-Type: application/json' \
    -d '{"message": "ping"}'
done
```
*Requests 1-10 will return `200 OK`. Request 11 will return `429 Too Many Requests` with a structured `ErrorResponse`.*

---

## 🔒 Security Best Practices
- **No Hardcoded Credentials**: API keys and database passwords are parameterized via environment variables.
- **Fail-Safe Token Verification**: Unauthenticated calls receive an explicit `401 Unauthorized` JSON contract rather than ambiguous 403 pages.
- **Polymorphic Protection**: Redis serializers do not enforce `@class` default typing globally, preventing remote code execution (RCE) deserialization vectors.
