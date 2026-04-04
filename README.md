# Procurement AI — MVP

AI-powered quote comparison for procurement teams. Upload supplier PDFs, get a side-by-side comparison in seconds.

## What it does (v1 scope)

- Upload supplier quotes (PDF, image, Excel)
- OCR via AWS Textract → structured extraction via Claude AI
- Side-by-side comparison with cheapest/most expensive highlighted
- Confidence scores per field — flags low-confidence extractions for human review
- Plain-English AI summary + recommendation
- Supplier registry with fuzzy name matching

## Quick start

### Prerequisites
- Docker + Docker Compose
- A Claude API key (https://console.anthropic.com)

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env and add your CLAUDE_API_KEY
```

### 2. Start with LocalStack (no real AWS needed for dev)

```bash
make up-local
```

This starts:
- Spring Boot app on `http://localhost:8081`
- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`
- LocalStack (S3 + Textract emulation) on `localhost:4566`

Flyway runs automatically and seeds demo data on first start.

### 3. Verify it's running

```bash
curl http://localhost:8081/api/v1/suppliers
# {"status":"UP"}
```

### Demo credentials

```
Email:    demo@apexconstruction.co.uk
Password: Demo1234!
Company:  Apex Construction Ltd (construction vertical)
```

---

## API endpoints

### Documents
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/documents/upload` | Upload a quote PDF (multipart) |
| `GET` | `/api/v1/documents/{id}` | Get document + extraction status |
| `POST` | `/api/v1/documents/{id}/approve` | Mark extraction as human-verified |

### Comparisons
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/comparisons` | Create a new comparison |
| `POST` | `/api/v1/comparisons/{id}/quotes` | Add a quote to comparison |
| `GET` | `/api/v1/comparisons/{id}/matrix` | Full side-by-side matrix |
| `GET` | `/api/v1/comparisons/{id}/summary` | AI summary + recommendation |

### Suppliers
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/suppliers` | List suppliers (supports `?search=`) |
| `POST` | `/api/v1/suppliers` | Create supplier |

---

## Example: upload a quote and get a comparison

```bash
# 1. Upload quote from Supplier A
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -F "file=@supplier_a_quote.pdf"
# Returns: { "documentId": "...", "status": "PROCESSING" }

# 2. Upload quote from Supplier B
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -F "file=@supplier_b_quote.pdf"

# 3. Create a comparison
curl -X POST http://localhost:8081/api/v1/comparisons \
  -H "Content-Type: application/json" \
  -d '{"title": "Steel order Q1 2025", "category": "Construction materials"}'

# 4. Add both quotes to it
curl -X POST http://localhost:8081/api/v1/comparisons/{id}/quotes \
  -d '{"quoteId": "..."}'

# 5. Get the matrix
curl http://localhost:8081/api/v1/comparisons/{id}/matrix

# 6. Get AI summary
curl http://localhost:8081/api/v1/comparisons/{id}/summary
```

---

## Database

### Useful commands

```bash
make shell-db        # Open psql
make db-tables       # List all tables
make db-migrations   # Show Flyway history
make db-demo         # Show seeded demo data
```

### Schema overview

```
companies          — procurement teams using the platform
users              — team members (OWNER / ADMIN / MEMBER / VIEWER)
suppliers          — supplier registry with fuzzy name search
documents          — uploaded files (stored in S3)
ocr_results        — raw Textract output per document
quotes             — structured data extracted from documents
quote_line_items   — individual line items with per-field confidence scores
extraction_jobs    — async processing queue
comparisons        — grouping quotes for side-by-side analysis
comparison_quotes  — which quotes are in each comparison
comparison_line_matches — matched equivalent items across quotes
audit_log          — every important action logged
```

---

## Architecture

```
React Native / Web Client
        │  HTTPS
        ▼
Spring Boot API (port 8080)
        │
        ├── AWS S3 (document storage)
        ├── AWS Textract (OCR)
        ├── Claude API (structured extraction + AI summary)
        ├── PostgreSQL (all structured data)
        └── Redis (sessions + cache)
```

---

## Project structure

```
src/main/java/com/procurementai/
├── ProcurementAiApplication.java
├── config/
│   ├── AppConfig.java          — WebClient, S3, Textract beans
│   ├── SecurityConfig.java     — JWT auth (placeholder for demo)
│   └── GlobalExceptionHandler.java
├── controller/
│   └── Controllers.java        — Document, Comparison, Supplier endpoints
├── model/
│   └── Models.java             — JPA entities + enums
├── repository/
│   └── Repositories.java       — Spring Data JPA repositories
├── service/
│   ├── DocumentProcessingService.java  — upload + OCR pipeline
│   └── ComparisonService.java          — matrix building + AI summary
└── integration/
    └── ClaudeExtractionService.java    — Claude API + prompt engineering

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__core_schema.sql     — companies, users, audit
    ├── V2__suppliers_documents.sql
    ├── V3__quotes_extraction.sql
    ├── V4__comparisons.sql
    └── V5__seed_demo_data.sql
```

---

## Next steps after MVP validation

- [ ] JWT authentication (replace demo `permitAll`)
- [ ] React Native mobile client
- [ ] Stripe billing integration
- [ ] Contract intelligence module (renewal alerts)
- [ ] Supplier onboarding compliance checker
- [ ] ERP integration (Microsoft Dynamics first)
- [ ] ISO 27001 gap analysis
