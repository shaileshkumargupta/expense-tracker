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
*Requests 1-10 will return `200 OK`. Request 11 will return `429 Too Many Requests` with a structured `ErrorResponse`:*
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 60 seconds.",
  "path": "/api/v1/ai/chat",
  "timeStamp": "2026-10-09T18:49:15"
}
```

---

## 💻 Frontend Client Integration (React + Vite)

The backend is configured with global Cross-Origin Resource Sharing (CORS) out of the box:
- **Allowed Origins:** `http://localhost:5173` (Vite dev server), `http://localhost:3000` (Create React App), configurable via `cors.allowed-origins`.
- **Allowed Methods:** `GET, POST, PUT, DELETE, OPTIONS, PATCH`
- **Allowed Headers:** `Authorization, Content-Type, X-Requested-With, Accept`
- **Exposed Headers:** `Authorization`
- **Credentials:** `true` (enables cookies and auth headers)
- **Preflight Support:** HTTP `OPTIONS` requests bypass authentication filters automatically.

---

## 🐳 Containerized Production Build (Dockerfile)

A multi-stage, production-optimized Dockerfile is included:
- **Stage 1 (Builder):** Uses `maven:3.9-eclipse-temurin-21-alpine` to pre-fetch dependencies and compile the executable JAR.
- **Stage 2 (Runtime):** Uses lightweight `eclipse-temurin:21-jre-alpine` running under a dedicated non-root `spring` system user.

```bash
# Build the production image
docker build -t expense-tracker:latest .

# Run container linked to host network or Docker network
docker run -d \
  --name expense-tracker-api \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/expense_tracker" \
  -e REDIS_HOST="localhost" \
  -e GEMINI_API_KEY="your_gemini_api_key_here" \
  expense-tracker:latest
```

---

## 🔒 Security Best Practices
- **No Hardcoded Credentials**: API keys and database passwords are parameterized via environment variables.
- **Fail-Safe Token Verification**: Unauthenticated calls receive an explicit `401 Unauthorized` JSON contract rather than ambiguous 403 pages.
- **Polymorphic Protection**: Redis serializers do not enforce `@class` default typing globally, preventing remote code execution (RCE) deserialization vectors.
