-- V1__core_schema.sql
-- Companies (procurement teams using the platform) and Users

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- fuzzy text search on supplier names

-- ── Subscription tiers ────────────────────────────────────────
CREATE TYPE subscription_tier AS ENUM ('FREE', 'STARTER', 'PRO', 'ENTERPRISE');
CREATE TYPE user_role AS ENUM ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER');
CREATE TYPE document_status AS ENUM ('UPLOADED', 'PROCESSING', 'EXTRACTED', 'REVIEW_REQUIRED', 'COMPLETED', 'FAILED');
CREATE TYPE extraction_status AS ENUM ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED');

-- ── Companies ─────────────────────────────────────────────────
CREATE TABLE companies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    domain          VARCHAR(255) UNIQUE,               -- email domain for auto-join
    industry        VARCHAR(100),                      -- construction, nhs, housing etc.
    size            VARCHAR(50),                       -- SME, MID_MARKET, ENTERPRISE
    subscription    subscription_tier NOT NULL DEFAULT 'FREE',
    subscription_expires_at TIMESTAMPTZ,
    stripe_customer_id      VARCHAR(255) UNIQUE,
    monthly_doc_limit       INTEGER DEFAULT 20,        -- FREE: 20 docs/month
    docs_used_this_month    INTEGER DEFAULT 0,
    billing_cycle_reset_at  TIMESTAMPTZ DEFAULT NOW() + INTERVAL '1 month',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Users ─────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    role            user_role NOT NULL DEFAULT 'MEMBER',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Audit log (every important action logged) ─────────────────
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    company_id      UUID REFERENCES companies(id),
    user_id         UUID REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,             -- DOCUMENT_UPLOADED, COMPARISON_CREATED etc.
    entity_type     VARCHAR(100),
    entity_id       UUID,
    metadata        JSONB,
    ip_address      INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX idx_users_company_id ON users(company_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_audit_log_company_id ON audit_log(company_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);

-- ── Updated_at trigger ────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_companies_updated_at
    BEFORE UPDATE ON companies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
