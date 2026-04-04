-- V6__convert_enums_to_varchar.sql
-- Convert PostgreSQL native enum columns to VARCHAR so Hibernate's
-- @Enumerated(EnumType.STRING) works without special JDBC type handling.

-- Drop enum-typed defaults first (they hold references to the enum types)
ALTER TABLE companies       ALTER COLUMN subscription     DROP DEFAULT;
ALTER TABLE users           ALTER COLUMN role              DROP DEFAULT;
ALTER TABLE documents       ALTER COLUMN status            DROP DEFAULT;
ALTER TABLE quotes          ALTER COLUMN extraction_status DROP DEFAULT;
ALTER TABLE extraction_jobs ALTER COLUMN status            DROP DEFAULT;

-- Convert columns from native enum to VARCHAR
ALTER TABLE companies       ALTER COLUMN subscription     TYPE VARCHAR(50) USING subscription::VARCHAR;
ALTER TABLE users           ALTER COLUMN role              TYPE VARCHAR(50) USING role::VARCHAR;
ALTER TABLE documents       ALTER COLUMN status            TYPE VARCHAR(50) USING status::VARCHAR;
ALTER TABLE quotes          ALTER COLUMN extraction_status TYPE VARCHAR(50) USING extraction_status::VARCHAR;
ALTER TABLE extraction_jobs ALTER COLUMN status            TYPE VARCHAR(50) USING status::VARCHAR;

-- Re-add defaults as plain strings
ALTER TABLE companies       ALTER COLUMN subscription     SET DEFAULT 'FREE';
ALTER TABLE users           ALTER COLUMN role              SET DEFAULT 'MEMBER';
ALTER TABLE documents       ALTER COLUMN status            SET DEFAULT 'UPLOADED';
ALTER TABLE quotes          ALTER COLUMN extraction_status SET DEFAULT 'PENDING';
ALTER TABLE extraction_jobs ALTER COLUMN status            SET DEFAULT 'PENDING';

-- Add CHECK constraints to preserve data integrity
ALTER TABLE companies       ADD CONSTRAINT chk_companies_subscription     CHECK (subscription IN ('FREE', 'STARTER', 'PRO', 'ENTERPRISE'));
ALTER TABLE users           ADD CONSTRAINT chk_users_role                 CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'));
ALTER TABLE documents       ADD CONSTRAINT chk_documents_status           CHECK (status IN ('UPLOADED', 'PROCESSING', 'EXTRACTED', 'REVIEW_REQUIRED', 'COMPLETED', 'FAILED'));
ALTER TABLE quotes          ADD CONSTRAINT chk_quotes_extraction_status   CHECK (extraction_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'));
ALTER TABLE extraction_jobs ADD CONSTRAINT chk_extraction_jobs_status     CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'));

-- Now safe to drop the enum types
DROP TYPE subscription_tier;
DROP TYPE user_role;
DROP TYPE document_status;
DROP TYPE extraction_status;
