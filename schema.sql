-- Sante Diagnostics LIMS Database Schema
-- Run this script once to initialise the database

-- USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(200) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('SUPER_ADMIN','LAB_ATTENDANT','CUSTOMER')),
    email_verified BOOLEAN DEFAULT FALSE,
    verification_token VARCHAR(100),
    force_password_change BOOLEAN DEFAULT FALSE,
    created_by INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- TEST TYPES TABLE
CREATE TABLE IF NOT EXISTS test_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100),
    price NUMERIC(10,2) NOT NULL,
    tat_hours INT NOT NULL,
    result_format VARCHAR(20) NOT NULL CHECK (result_format IN ('NUMERIC','TEXT','PDF','IMAGE')),
    description TEXT,
    created_by INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- TEST REQUESTS TABLE
CREATE TABLE IF NOT EXISTS test_requests (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES users(id),
    test_type_id INT NOT NULL REFERENCES test_types(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','PAID','SAMPLE_COLLECTED','PROCESSING','VALIDATING','COMPLETED','CANCELLED')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID','PAID')),
    payment_marked_by INT REFERENCES users(id),
    payment_marked_at TIMESTAMP,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expected_ready_at TIMESTAMP,
    notes TEXT
);

-- SAMPLES TABLE
CREATE TABLE IF NOT EXISTS samples (
    id SERIAL PRIMARY KEY,
    request_id INT NOT NULL REFERENCES test_requests(id),
    barcode VARCHAR(50) UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_COLLECTION'
        CHECK (status IN ('AWAITING_COLLECTION','COLLECTED','IN_TRANSIT','RECEIVED','PROCESSING','VALIDATED','REJECTED')),
    collected_at TIMESTAMP,
    received_at TIMESTAMP,
    processed_at TIMESTAMP,
    validated_at TIMESTAMP,
    attendant_id INT REFERENCES users(id),
    notes TEXT
);

-- RESULTS TABLE
CREATE TABLE IF NOT EXISTS results (
    id SERIAL PRIMARY KEY,
    request_id INT NOT NULL REFERENCES test_requests(id),
    result_value TEXT,
    result_file_path VARCHAR(500),
    result_file_type VARCHAR(10),
    is_validated BOOLEAN DEFAULT FALSE,
    validated_by INT REFERENCES users(id),
    validated_at TIMESTAMP,
    is_visible_to_customer BOOLEAN DEFAULT FALSE,
    uploaded_by INT REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

-- AUDIT LOGS TABLE (immutable)
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id),
    action VARCHAR(200) NOT NULL,
    entity_type VARCHAR(50),
    entity_id INT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- EMAIL NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS email_notifications (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id),
    email VARCHAR(200),
    subject VARCHAR(300),
    body TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SENT'
);

-- BANK DETAILS TABLE
CREATE TABLE IF NOT EXISTS bank_details (
    id SERIAL PRIMARY KEY,
    bank_name VARCHAR(150),
    account_name VARCHAR(150),
    account_number VARCHAR(50),
    sort_code VARCHAR(30),
    instructions TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default bank details
INSERT INTO bank_details (bank_name, account_name, account_number, sort_code, instructions)
VALUES ('First Bank Nigeria', 'Sante Diagnostics Ltd', '3012345678', 'N/A',
        'Please use your Test Request ID as the payment reference. Send proof of payment to accounts@santediagnostics.com')
ON CONFLICT DO NOTHING;

-- Default Super Admin (password: Admin@1234 – must be changed on first login)
-- BCrypt hash for Admin@1234
INSERT INTO users (name, email, password_hash, role, email_verified, force_password_change)
VALUES ('Super Admin', 'admin@santediagnostics.com',
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', -- placeholder; app seeds correct hash
        'SUPER_ADMIN', TRUE, FALSE)
ON CONFLICT (email) DO NOTHING;