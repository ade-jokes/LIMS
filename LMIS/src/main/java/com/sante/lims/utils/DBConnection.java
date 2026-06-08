package com.sante.lims.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mindrot.jbcrypt.BCrypt;

public class DBConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/sante_lims";
    private static final String USER = "postgres";
    private static final String PASSWORD = "adminpassword";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id SERIAL PRIMARY KEY, "
                    + "name VARCHAR(100) NOT NULL, "
                    + "email VARCHAR(150) UNIQUE NOT NULL, "
                    + "password_hash VARCHAR(255) NOT NULL, "
                    + "role VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'LAB_ATTENDANT', 'CUSTOMER')), "
                    + "must_change_password BOOLEAN DEFAULT FALSE, "
                    + "is_verified BOOLEAN DEFAULT FALSE, "
                    + "verification_code VARCHAR(6), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            // 2. Create Test Types Table
            stmt.execute("CREATE TABLE IF NOT EXISTS test_types ("
                    + "id SERIAL PRIMARY KEY, "
                    + "name VARCHAR(100) NOT NULL, "
                    + "price DECIMAL(10,2) NOT NULL CHECK (price >= 0), "
                    + "tat_hours INT NOT NULL CHECK (tat_hours >= 0), "
                    + "result_format VARCHAR(20) NOT NULL CHECK (result_format IN ('numeric', 'text', 'PDF', 'image')), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            // 3. Create Test Requests Table
            stmt.execute("CREATE TABLE IF NOT EXISTS test_requests ("
                    + "id SERIAL PRIMARY KEY, "
                    + "customer_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, "
                    + "test_type_id INT NOT NULL REFERENCES test_types(id) ON DELETE RESTRICT, "
                    + "payment_status VARCHAR(15) NOT NULL DEFAULT 'Unpaid' CHECK (payment_status IN ('Paid', 'Unpaid')), "
                    + "payment_evidence VARCHAR(255), "
                    + "sample_status VARCHAR(30) NOT NULL DEFAULT 'Pending Collection' CHECK (sample_status IN ('Pending Collection', 'Collected', 'Processing', 'Validated')), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "collected_at TIMESTAMP, "
                    + "processed_at TIMESTAMP, "
                    + "validated_at TIMESTAMP, "
                    + "due_at TIMESTAMP NOT NULL"
                    + ")");

            // 4. Create Results Table
            stmt.execute("CREATE TABLE IF NOT EXISTS results ("
                    + "id SERIAL PRIMARY KEY, "
                    + "request_id INT UNIQUE NOT NULL REFERENCES test_requests(id) ON DELETE CASCADE, "
                    + "result_value TEXT, "
                    + "file_name VARCHAR(255), "
                    + "file_data BYTEA, "
                    + "is_validated BOOLEAN DEFAULT FALSE, "
                    + "validated_by INT REFERENCES users(id), "
                    + "validated_at TIMESTAMP"
                    + ")");

            // 5. Create Audit Trail Table
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_trail ("
                    + "id SERIAL PRIMARY KEY, "
                    + "user_id INT REFERENCES users(id) ON DELETE SET NULL, "
                    + "user_email VARCHAR(150), "
                    + "action VARCHAR(100) NOT NULL, "
                    + "details TEXT, "
                    + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            applySchemaMigrations(stmt);

            stmt.execute("CREATE OR REPLACE FUNCTION prevent_audit_trail_changes() "
                    + "RETURNS trigger AS $$ "
                    + "BEGIN "
                    + "RAISE EXCEPTION 'audit_trail is immutable and cannot be updated or deleted'; "
                    + "END; "
                    + "$$ LANGUAGE plpgsql");
            stmt.execute("DROP TRIGGER IF EXISTS audit_trail_no_update ON audit_trail");
            stmt.execute("CREATE TRIGGER audit_trail_no_update "
                    + "BEFORE UPDATE ON audit_trail "
                    + "FOR EACH ROW EXECUTE FUNCTION prevent_audit_trail_changes()");
            stmt.execute("DROP TRIGGER IF EXISTS audit_trail_no_delete ON audit_trail");
            stmt.execute("CREATE TRIGGER audit_trail_no_delete "
                    + "BEFORE DELETE ON audit_trail "
                    + "FOR EACH ROW EXECUTE FUNCTION prevent_audit_trail_changes()");

            System.out.println("Tables successfully initialized!");

            // Seed default users if table is empty
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("Seeding default users...");

                    String insertSql = "INSERT INTO users (name, email, password_hash, role, must_change_password, is_verified) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        
                        // Seed Super Admin
                        pstmt.setString(1, "Super Admin");
                        pstmt.setString(2, "admin@sante.com");
                        pstmt.setString(3, BCrypt.hashpw("admin123", BCrypt.gensalt()));
                        pstmt.setString(4, "SUPER_ADMIN");
                        pstmt.setBoolean(5, false);
                        pstmt.setBoolean(6, true);
                        pstmt.executeUpdate();

                        // Seed Lab Attendant
                        pstmt.setString(1, "Lab Attendant");
                        pstmt.setString(2, "attendant@sante.com");
                        pstmt.setString(3, BCrypt.hashpw("attendant123", BCrypt.gensalt()));
                        pstmt.setString(4, "LAB_ATTENDANT");
                        pstmt.setBoolean(5, true); // Force password change on first login
                        pstmt.setBoolean(6, true);
                        pstmt.executeUpdate();

                        // Seed Customer
                        pstmt.setString(1, "Jane Doe");
                        pstmt.setString(2, "customer@sante.com");
                        pstmt.setString(3, BCrypt.hashpw("customer123", BCrypt.gensalt()));
                        pstmt.setString(4, "CUSTOMER");
                        pstmt.setBoolean(5, false);
                        pstmt.setBoolean(6, true);
                        pstmt.executeUpdate();

                        System.out.println("Default users seeded successfully!");
                    }
                }
            }
            ensureDefaultSuperAdmin(conn);

        } catch (SQLException e) {
            System.err.println("Database initialization failed!");
            e.printStackTrace();
        }
    }

    private static void ensureDefaultSuperAdmin(Connection conn) throws SQLException {
        String countSql = "SELECT count(*) FROM users WHERE role = 'SUPER_ADMIN'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO users (name, email, password_hash, role, must_change_password, is_verified) "
                        + "VALUES (?, ?, ?, 'SUPER_ADMIN', false, true)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setString(1, "Super Admin");
                    pstmt.setString(2, "admin@sante.com");
                    pstmt.setString(3, BCrypt.hashpw("admin123", BCrypt.gensalt()));
                    pstmt.executeUpdate();
                    System.out.println("Recovered missing Super Admin: admin@sante.com / admin123");
                }
                return;
            }
        }

        String selectDefaultSql = "SELECT password_hash FROM users WHERE email = 'admin@sante.com' AND role = 'SUPER_ADMIN'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectDefaultSql)) {
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                if (hash == null || !hash.startsWith("$2")) {
                    String resetSql = "UPDATE users SET password_hash = ?, must_change_password = false, is_verified = true "
                            + "WHERE email = 'admin@sante.com' AND role = 'SUPER_ADMIN'";
                    try (PreparedStatement pstmt = conn.prepareStatement(resetSql)) {
                        pstmt.setString(1, BCrypt.hashpw("admin123", BCrypt.gensalt()));
                        pstmt.executeUpdate();
                        System.out.println("Reset invalid Super Admin password hash: admin@sante.com / admin123");
                    }
                }
            }
        }
    }

    private static void applySchemaMigrations(Statement stmt) throws SQLException {
        stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE");
        stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE");
        stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code VARCHAR(6)");
        stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        stmt.execute("ALTER TABLE test_types ADD COLUMN IF NOT EXISTS price DECIMAL(10,2) DEFAULT 0");
        stmt.execute("ALTER TABLE test_types ADD COLUMN IF NOT EXISTS tat_hours INT DEFAULT 24");
        stmt.execute("ALTER TABLE test_types ADD COLUMN IF NOT EXISTS result_format VARCHAR(20) DEFAULT 'text'");
        stmt.execute("ALTER TABLE test_types ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        stmt.execute("ALTER TABLE test_requests ADD COLUMN IF NOT EXISTS payment_status VARCHAR(15) DEFAULT 'Unpaid'");
        stmt.execute("ALTER TABLE test_requests ADD COLUMN IF NOT EXISTS payment_evidence VARCHAR(255)");
        stmt.execute("ALTER TABLE test_requests ADD COLUMN IF NOT EXISTS sample_status VARCHAR(30) DEFAULT 'Pending Collection'");
        stmt.execute("ALTER TABLE test_requests ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        stmt.execute("ALTER TABLE test_requests ADD COLUMN IF NOT EXISTS collected_at TIMESTAMP");
        stmt.execute("ALTER TABLE test_requests ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP");
        stmt.execute("ALTER TABLE test_requests ADD COLUMN IF NOT EXISTS validated_at TIMESTAMP");
        stmt.execute("ALTER TABLE test_requests ADD COLUMN IF NOT EXISTS due_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        stmt.execute("ALTER TABLE results ADD COLUMN IF NOT EXISTS result_value TEXT");
        stmt.execute("ALTER TABLE results ADD COLUMN IF NOT EXISTS file_name VARCHAR(255)");
        stmt.execute("ALTER TABLE results ADD COLUMN IF NOT EXISTS file_data BYTEA");
        stmt.execute("ALTER TABLE results ADD COLUMN IF NOT EXISTS is_validated BOOLEAN DEFAULT FALSE");
        stmt.execute("ALTER TABLE results ADD COLUMN IF NOT EXISTS validated_by INT");
        stmt.execute("ALTER TABLE results ADD COLUMN IF NOT EXISTS validated_at TIMESTAMP");

        stmt.execute("ALTER TABLE audit_trail ADD COLUMN IF NOT EXISTS user_id INT");
        stmt.execute("ALTER TABLE audit_trail ADD COLUMN IF NOT EXISTS user_email VARCHAR(150)");
        stmt.execute("ALTER TABLE audit_trail ADD COLUMN IF NOT EXISTS action VARCHAR(100)");
        stmt.execute("ALTER TABLE audit_trail ADD COLUMN IF NOT EXISTS details TEXT");
        stmt.execute("ALTER TABLE audit_trail ADD COLUMN IF NOT EXISTS timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
    }
}
