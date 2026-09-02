# AbhiAI Backend

Production-oriented Java backend for **AbhiAI**: an authenticated, multi-model AI assistant and social platform. The service keeps AI vendors behind internal provider contracts so the web and future mobile clients can remain unchanged as providers evolve or Abhena capabilities are introduced.

> **Status:** active development. Core authentication, conversations, multimodal foundations, model routing, and the social platform are implemented. Production hardening and Abhena research are tracked in the public [AbhiAI Development Roadmap](https://github.com/users/Ap-95671/projects/2).

## Highlights

- JWT authentication with BCrypt password hashing and ownership-aware authorization
- PostgreSQL persistence with 32 versioned Flyway migrations
- Stored conversations, messages, streaming responses, and generation cancellation
- Provider-independent model registry, availability catalog, manual selection, routing, and fallback
- Adapters for OpenAI-compatible providers, Gemini, Groq, Ollama, Anthropic, xAI, DeepSeek, Mistral, Cohere, and OpenRouter
- Image, PDF, and text attachments with extraction, contextual retrieval, and local embeddings
- Gemini-backed image generation behind an internal provider contract
- Profiles, follows, posts, feeds, replies, likes, reposts, bookmarks, notifications, and search
- Direct messages, groups, communities, stories, video, hashtags, mentions, polls, articles, analytics, and moderation
- Local or S3-compatible media storage for AWS S3, Cloudflare R2, and compatible services
- Structured validation and centralized exception handling
- Spring Boot Actuator health probes and a broad automated test suite

## Architecture

AbhiAI is currently a modular monolith. Package boundaries keep business capabilities separable without introducing premature distributed-system complexity.

```text
HTTP request
    │
    ▼
Controller ── DTO validation / authentication boundary
    │
    ▼
Service ───── business rules and transactions
    │
    ├── Repository ── JPA / PostgreSQL
    ├── AI provider contracts ── external models or future Abhena
    └── Storage contracts ── local or S3-compatible media
```

Key source areas:

```text
src/main/java/com/abhiai/abhiai_backend/
├── controller/     REST endpoints only
├── service/        business workflows and transactional rules
├── repository/     database access
├── entity/         persisted domain models
├── dto/            request and response contracts
├── security/       JWT authentication and authorization
├── ai/             model registry, routing, tools, and provider adapters
├── config/         application and infrastructure configuration
└── exception/      structured API errors
```

The frontend depends on stable AbhiAI API contracts, not vendor SDKs. A future Abhena implementation can therefore implement the same internal model-provider boundary.

## Technology

- Java 21
- Spring Boot 4.0.7
- Spring Security and JJWT
- Spring Data JPA / Hibernate
- PostgreSQL and Flyway
- Apache PDFBox
- AWS SDK for S3-compatible storage
- Maven Wrapper
- Docker

## Local development

### Prerequisites

- Java 21
- PostgreSQL 17 or a compatible PostgreSQL release
- Docker Desktop (optional, for the composed stack)

### 1. Create the database

```sql
CREATE DATABASE abhiai;
```

### 2. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env`, use a long random JWT secret, and add at least one AI provider key if chat generation is required. `.env` is ignored by Git and must never be committed.

For a direct Maven run, export the file into the current shell:

```bash
set -a
source .env
set +a
```

### 3. Run migrations and start the API

```bash
./mvnw spring-boot:run
```

Flyway applies migrations before Hibernate validates the schema. The API starts on `http://localhost:8080` and health is available at:

```text
http://localhost:8080/actuator/health
```

### Docker

From the parent AbhiAI workspace containing `docker-compose.yml`:

```bash
docker compose up -d --build
```

## Verification

```bash
./mvnw test
```

The audited main branch passes **185 backend tests**. New provider adapters should also satisfy provider contract tests as that production-quality gate is implemented.

## API areas

All application APIs are versioned under `/api/v1`.

| Area | Examples |
| --- | --- |
| Authentication | registration, login, JWT-protected sessions |
| AI chat | conversations, messages, streaming, cancellation, model catalog |
| Multimodal | attachments, document extraction, contextual retrieval, image generation |
| Social graph | profiles, follows, blocks, mutes, recommendations |
| Publishing | posts, replies, reposts, bookmarks, polls, articles, media |
| Community | feeds, search, notifications, messaging, groups, communities, stories |
| Safety | reporting, moderation, visibility and ownership policies |

See the controllers and DTOs for the exact current request/response contracts. Secrets, password hashes, and internal provider credentials are never part of response DTOs.

## AI provider configuration

Provider credentials are server-side environment variables. Configure only the providers you intend to expose.

| Provider | Key variable |
| --- | --- |
| OpenAI | `OPENAI_API_KEY` |
| Gemini | `GEMINI_API_KEY` |
| Groq | `GROQ_API_KEY` |
| Anthropic | `ANTHROPIC_API_KEY` |
| xAI | `XAI_API_KEY` |
| DeepSeek | `DEEPSEEK_API_KEY` |
| Mistral | `MISTRAL_API_KEY` |
| Cohere | `COHERE_API_KEY` |
| OpenRouter | `OPENROUTER_API_KEY` |
| Ollama | no cloud key; configure `OLLAMA_BASE_URL` |

Image generation uses the same server-side boundary. Configure
`CLOUDFLARE_ACCOUNT_ID` and `CLOUDFLARE_API_TOKEN` to use Cloudflare Workers AI
with `@cf/black-forest-labs/flux-1-schnell` as the default image model. The
Workers AI call is made directly by the backend; no Cloudflare credential is
sent to the browser. Gemini image generation remains available, while fallback
from Cloudflare to Gemini is disabled unless `GEMINI_IMAGE_FALLBACK_ENABLED=true`
is intentionally configured.

An empty or missing key keeps that provider unavailable. The model catalog must not advertise an unconfigured provider as ready.

## Media storage

Development can use `MEDIA_STORAGE_TYPE=local`. Production should use `MEDIA_STORAGE_TYPE=s3` with the bucket, region, endpoint, and credentials stored in the deployment platform's secret manager. The existing S3-compatible layer supports services such as Cloudflare R2 through custom endpoint and path-style settings.

## Security

- Never commit `.env`, database credentials, JWT secrets, provider keys, or storage keys.
- Use an environment-specific JWT secret of at least 32 random characters.
- Set `ALLOWED_ORIGINS` to exact trusted HTTPS origins in production.
- Production uses Flyway plus `ddl-auto=validate`; do not switch to automatic schema mutation.
- Treat uploaded files as untrusted and retain size/type limits.
- Rotate any credential immediately if it is ever exposed in Git history, logs, or screenshots.

To report a vulnerability, avoid public issue details and contact the repository owner privately first.

## Deployment

The repository includes Docker and Render configuration. A typical production topology is:

```text
Vercel web client ──HTTPS──▶ Render backend ──▶ managed PostgreSQL
                                      ├──────▶ AI providers
                                      └──────▶ S3 / R2 media storage
```

Production values belong in Render/Vercel environment settings, never in source control. The backend fails fast when required production database, JWT, CORS, provider, or enabled storage configuration is missing.

## Roadmap and contributing

- [Development Roadmap](https://github.com/users/Ap-95671/projects/2)
- [Backend issues](https://github.com/Ap-95671/abhiai-backend/issues)
- [Frontend repository](https://github.com/Ap-95671/abhiai-frontend)

Before opening a pull request:

1. Link the relevant roadmap issue.
2. Keep controllers thin and business rules in services.
3. Use DTOs at API boundaries and migrations for schema changes.
4. Add or update proportionate automated tests.
5. Run `./mvnw test` and confirm no secrets or generated files are staged.

## License

No open-source license has been selected yet. All rights are reserved by the project owner until a license file is added.
