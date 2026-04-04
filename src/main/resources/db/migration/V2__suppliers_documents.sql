-- V2__suppliers_documents.sql
-- Supplier registry and uploaded documents

-- ── Suppliers ─────────────────────────────────────────────────
CREATE TABLE suppliers (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    trading_name        VARCHAR(255),
    email               VARCHAR(255),
    phone               VARCHAR(50),
    website             VARCHAR(500),
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    city                VARCHAR(100),
    postcode            VARCHAR(20),
    country             VARCHAR(100) DEFAULT 'United Kingdom',
    -- Compliance fields
    companies_house_no  VARCHAR(20),
    vat_number          VARCHAR(30),
    insurance_expiry    DATE,
    accreditations      JSONB DEFAULT '[]',            -- [{type, issuer, expiry}]
    is_preferred        BOOLEAN DEFAULT FALSE,
    is_active           BOOLEAN DEFAULT TRUE,
    notes               TEXT,
    metadata            JSONB DEFAULT '{}',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Documents (raw uploaded files) ────────────────────────────
CREATE TABLE documents (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    uploaded_by         UUID NOT NULL REFERENCES users(id),
    supplier_id         UUID REFERENCES suppliers(id),
    original_filename   VARCHAR(500) NOT NULL,
    storage_key         VARCHAR(1000) NOT NULL,        -- S3 key
    content_type        VARCHAR(100) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    page_count          INTEGER,
    status              document_status NOT NULL DEFAULT 'UPLOADED',
    processing_error    TEXT,
    checksum_sha256     VARCHAR(64),                   -- dedup + integrity
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── OCR Results (raw text from Textract) ──────────────────────
CREATE TABLE ocr_results (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id         UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    raw_text            TEXT,
    page_data           JSONB,                         -- per-page blocks from Textract
    textract_job_id     VARCHAR(255),
    confidence_avg      DECIMAL(5,4),
    processed_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(document_id)
);

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX idx_suppliers_company_id ON suppliers(company_id);
CREATE INDEX idx_suppliers_name_trgm ON suppliers USING gin(name gin_trgm_ops);
CREATE INDEX idx_documents_company_id ON documents(company_id);
CREATE INDEX idx_documents_supplier_id ON documents(supplier_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at DESC);

CREATE TRIGGER update_suppliers_updated_at
    BEFORE UPDATE ON suppliers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
