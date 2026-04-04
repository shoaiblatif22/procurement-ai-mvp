-- V5__seed_demo_data.sql
-- Demo data for showcasing to potential users
-- Password for all demo users: Demo1234! (bcrypt hashed)

INSERT INTO companies (id, name, domain, industry, size, subscription, monthly_doc_limit)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Apex Construction Ltd',
    'apexconstruction.co.uk',
    'Construction',
    'MID_MARKET',
    'PRO',
    500
);

INSERT INTO users (id, company_id, email, password_hash, first_name, last_name, role, email_verified)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'demo@apexconstruction.co.uk',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY2j5ZbEiMFUB.O',
    'Sarah',
    'Thompson',
    'OWNER',
    TRUE
);

INSERT INTO suppliers (id, company_id, name, email, city, postcode, is_preferred, is_active)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
     'BuildRight Materials Ltd', 'sales@buildright.co.uk', 'Manchester', 'M1 1AB', TRUE, TRUE),
    ('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
     'Northern Steel Supplies', 'quotes@northernsteel.co.uk', 'Leeds', 'LS1 2CD', FALSE, TRUE),
    ('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001',
     'UK Construction Wholesale', 'procurement@ukcw.co.uk', 'Birmingham', 'B1 3EF', FALSE, TRUE);
