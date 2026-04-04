-- V3__quotes_extraction.sql
-- Quotes extracted from documents and AI extraction results

-- ── Quotes (one per uploaded supplier quote document) ─────────
CREATE TABLE quotes (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id         UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    supplier_id         UUID REFERENCES suppliers(id),
    supplier_name_raw   VARCHAR(255),                  -- as extracted (before supplier match)
    quote_reference     VARCHAR(255),                  -- supplier's own ref number
    quote_date          DATE,
    valid_until         DATE,
    currency            VARCHAR(3) DEFAULT 'GBP',
    subtotal            DECIMAL(15,4),
    tax_amount          DECIMAL(15,4),
    total_amount        DECIMAL(15,4),
    payment_terms       VARCHAR(500),
    delivery_terms      VARCHAR(500),
    lead_time_days      INTEGER,
    notes               TEXT,
    extraction_status   extraction_status NOT NULL DEFAULT 'PENDING',
    extraction_confidence DECIMAL(5,4),               -- overall confidence score 0-1
    requires_review     BOOLEAN NOT NULL DEFAULT FALSE,-- true if confidence < threshold
    reviewed_by         UUID REFERENCES users(id),
    reviewed_at         TIMESTAMPTZ,
    raw_extraction      JSONB,                         -- full Claude response stored for audit
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Quote line items (individual products/services in a quote) ─
CREATE TABLE quote_line_items (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quote_id            UUID NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    line_number         INTEGER NOT NULL,
    description         TEXT NOT NULL,
    quantity            DECIMAL(15,4),
    unit                VARCHAR(100),                  -- each, kg, m2, hour etc.
    unit_price          DECIMAL(15,4),
    discount_pct        DECIMAL(5,4),
    line_total          DECIMAL(15,4),
    sku                 VARCHAR(255),
    -- Confidence per field (AI extraction quality)
    confidence_description  DECIMAL(5,4),
    confidence_quantity     DECIMAL(5,4),
    confidence_unit_price   DECIMAL(5,4),
    confidence_line_total   DECIMAL(5,4),
    -- Flag individual fields for review
    flagged_fields      JSONB DEFAULT '[]',            -- ["unit_price", "quantity"]
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Extraction jobs (async processing queue) ──────────────────
CREATE TABLE extraction_jobs (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id         UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    quote_id            UUID REFERENCES quotes(id),
    status              extraction_status NOT NULL DEFAULT 'PENDING',
    attempts            INTEGER NOT NULL DEFAULT 0,
    max_attempts        INTEGER NOT NULL DEFAULT 3,
    error_message       TEXT,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX idx_quotes_company_id ON quotes(company_id);
CREATE INDEX idx_quotes_document_id ON quotes(document_id);
CREATE INDEX idx_quotes_supplier_id ON quotes(supplier_id);
CREATE INDEX idx_quotes_extraction_status ON quotes(extraction_status);
CREATE INDEX idx_quotes_requires_review ON quotes(requires_review) WHERE requires_review = TRUE;
CREATE INDEX idx_quote_line_items_quote_id ON quote_line_items(quote_id);
CREATE INDEX idx_extraction_jobs_status ON extraction_jobs(status);
CREATE INDEX idx_extraction_jobs_document_id ON extraction_jobs(document_id);

CREATE TRIGGER update_quotes_updated_at
    BEFORE UPDATE ON quotes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_extraction_jobs_updated_at
    BEFORE UPDATE ON extraction_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
