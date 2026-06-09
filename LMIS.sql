-- ============================================================
--  Sante Diagnostics LIMS – Database Schema
--  Run once against a fresh PostgreSQL database.
--  Column names, CHECK values and casing all match the Java
--  source code in DBConnection.java / the controller layer.
-- ============================================================

-- 1. USERS
CREATE TABLE IF NOT EXISTS users (
    id                   SERIAL PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    email                VARCHAR(150) UNIQUE NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    role                 VARCHAR(20)  NOT NULL
                             CHECK (role IN ('SUPER_ADMIN', 'LAB_ATTENDANT', 'CUSTOMER')),
    must_change_password BOOLEAN      DEFAULT FALSE,   -- Java: must_change_password
    is_verified          BOOLEAN      DEFAULT FALSE,   -- Java: is_verified
    verification_code    VARCHAR(6),
    created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 2. TEST TYPES
--    result_format values are lowercase to match Java inserts ('numeric','text','PDF','image')
CREATE TABLE IF NOT EXISTS test_types (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    price         DECIMAL(10,2)  NOT NULL CHECK (price >= 0),
    tat_hours     INT            NOT NULL CHECK (tat_hours >= 0),
    result_format VARCHAR(20)    NOT NULL
                      CHECK (result_format IN ('numeric', 'text', 'PDF', 'image')),
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

-- 3. TEST REQUESTS
--    payment_status / sample_status casing matches Java ('Paid'/'Unpaid', 'Pending Collection' etc.)
CREATE TABLE IF NOT EXISTS test_requests (
    id               SERIAL PRIMARY KEY,
    customer_id      INT          NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    test_type_id     INT          NOT NULL REFERENCES test_types(id) ON DELETE RESTRICT,
    payment_status   VARCHAR(15)  NOT NULL DEFAULT 'Unpaid'
                         CHECK (payment_status IN ('Paid', 'Unpaid')),
    payment_evidence VARCHAR(255),
    sample_status    VARCHAR(30)  NOT NULL DEFAULT 'Pending Collection'
                         CHECK (sample_status IN
                             ('Pending Collection', 'Collected', 'Processing', 'Validated')),
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    collected_at     TIMESTAMP,
    processed_at     TIMESTAMP,
    validated_at     TIMESTAMP,
    due_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. RESULTS
CREATE TABLE IF NOT EXISTS results (
    id           SERIAL PRIMARY KEY,
    request_id   INT     UNIQUE NOT NULL REFERENCES test_requests(id) ON DELETE CASCADE,
    result_value TEXT,
    file_name    VARCHAR(255),
    file_data    BYTEA,
    is_validated BOOLEAN DEFAULT FALSE,
    validated_by INT     REFERENCES users(id),
    validated_at TIMESTAMP
);

-- 5. AUDIT TRAIL  (immutable — triggers added by DBConnection.initializeDatabase())
CREATE TABLE IF NOT EXISTS audit_trail (
    id         SERIAL PRIMARY KEY,
    user_id    INT          REFERENCES users(id) ON DELETE SET NULL,
    user_email VARCHAR(150),
    action     VARCHAR(100) NOT NULL,
    details    TEXT,
    timestamp  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Immutable triggers
CREATE OR REPLACE FUNCTION prevent_audit_trail_changes()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_trail is immutable and cannot be updated or deleted';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS audit_trail_no_update ON audit_trail;
CREATE TRIGGER audit_trail_no_update
    BEFORE UPDATE ON audit_trail
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_trail_changes();

DROP TRIGGER IF EXISTS audit_trail_no_delete ON audit_trail;
CREATE TRIGGER audit_trail_no_delete
    BEFORE DELETE ON audit_trail
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_trail_changes();

-- 6. BANK DETAILS
CREATE TABLE IF NOT EXISTS bank_details (
    id             SERIAL PRIMARY KEY,
    bank_name      VARCHAR(150),
    account_name   VARCHAR(150),
    account_number VARCHAR(50),
    sort_code      VARCHAR(30),
    instructions   TEXT,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default bank details (safe to re-run)
INSERT INTO bank_details (bank_name, account_name, account_number, sort_code, instructions)
SELECT 'First Bank Nigeria',
       'Sante Diagnostics Ltd',
       '3012345678',
       'N/A',
       'Please use your Test Request ID as the payment reference. Send proof of payment to accounts@santediagnostics.com'
WHERE NOT EXISTS (SELECT 1 FROM bank_details);

-- ============================================================
--  Default seed users
--  Passwords are BCrypt hashes – the app re-seeds via
--  DBConnection.initializeDatabase() if the table is empty,
--  so these INSERTs are a manual fallback only.
--  Login: admin@sante.com / admin123
--         attendant@sante.com / attendant123  (force-change on first login)
--         customer@sante.com  / customer123
-- ============================================================
INSERT INTO users (name, email, password_hash, role, must_change_password, is_verified)
SELECT 'Super Admin', 'admin@sante.com',
       '$2a$10$placeholder_replaced_by_app_seed_on_first_run',
       'SUPER_ADMIN', FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@sante.com');