-- V4__comparisons.sql
-- The core MVP feature: comparing multiple supplier quotes side by side

-- ── Comparisons (grouping quotes for side-by-side analysis) ───
CREATE TABLE comparisons (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    created_by          UUID NOT NULL REFERENCES users(id),
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    category            VARCHAR(255),                  -- office supplies, IT hardware etc.
    status              VARCHAR(50) NOT NULL DEFAULT 'DRAFT',   -- DRAFT, ACTIVE, DECIDED, ARCHIVED
    decision_quote_id   UUID REFERENCES quotes(id),    -- which supplier won
    decision_reason     TEXT,
    decided_at          TIMESTAMPTZ,
    decided_by          UUID REFERENCES users(id),
    ai_summary          TEXT,                          -- Claude's plain English comparison summary
    ai_recommendation   TEXT,                          -- Claude's recommendation with reasoning
    total_potential_saving DECIMAL(15,4),              -- vs most expensive option
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Comparison quotes (which quotes are in each comparison) ───
CREATE TABLE comparison_quotes (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    comparison_id       UUID NOT NULL REFERENCES comparisons(id) ON DELETE CASCADE,
    quote_id            UUID NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    added_by            UUID REFERENCES users(id),
    sort_order          INTEGER DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(comparison_id, quote_id)
);

-- ── Line item matches (matching equivalent items across quotes) ─
-- e.g. "10x Steel Beams" in quote A matches "Steel Beam x10" in quote B
CREATE TABLE comparison_line_matches (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    comparison_id       UUID NOT NULL REFERENCES comparisons(id) ON DELETE CASCADE,
    match_group         INTEGER NOT NULL,               -- items with same group are equivalent
    quote_line_item_id  UUID NOT NULL REFERENCES quote_line_items(id) ON DELETE CASCADE,
    match_confidence    DECIMAL(5,4),                  -- how confident we are these match
    is_manual_match     BOOLEAN DEFAULT FALSE,          -- user confirmed/created this match
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(comparison_id, quote_line_item_id)
);

-- ── Missing items (items in some quotes but not others) ────────
CREATE TABLE comparison_missing_items (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    comparison_id       UUID NOT NULL REFERENCES comparisons(id) ON DELETE CASCADE,
    quote_id            UUID NOT NULL REFERENCES quotes(id),
    match_group         INTEGER NOT NULL,               -- the match group this item is missing from
    description         TEXT NOT NULL,                 -- what's missing
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Comparison comments / notes ────────────────────────────────
CREATE TABLE comparison_comments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    comparison_id       UUID NOT NULL REFERENCES comparisons(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id),
    content             TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX idx_comparisons_company_id ON comparisons(company_id);
CREATE INDEX idx_comparisons_created_by ON comparisons(created_by);
CREATE INDEX idx_comparisons_status ON comparisons(status);
CREATE INDEX idx_comparison_quotes_comparison_id ON comparison_quotes(comparison_id);
CREATE INDEX idx_comparison_quotes_quote_id ON comparison_quotes(quote_id);
CREATE INDEX idx_line_matches_comparison_id ON comparison_line_matches(comparison_id);
CREATE INDEX idx_line_matches_group ON comparison_line_matches(comparison_id, match_group);

CREATE TRIGGER update_comparisons_updated_at
    BEFORE UPDATE ON comparisons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
