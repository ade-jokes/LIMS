package com.sante.lims.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditService {

    /**
     * Logs an action to the audit trail.
     * @param userId
     * @param userEmail
     * @param action
     * @param details
     */
    public static void log(Integer userId, String userEmail, String action, String details) {
        String sql = "INSERT INTO audit_trail (user_id, user_email, action, details) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (userId != null) {
                pstmt.setInt(1, userId);
            } else {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            }
            pstmt.setString(2, userEmail);
            pstmt.setString(3, action);
            pstmt.setString(4, details);
            pstmt.executeUpdate();
            System.out.println("Audit Logged: " + action + " - " + details + " (" + userEmail + ")");
        } catch (SQLException e) {
            System.err.println("Failed to write audit log!");
        }
    }

    /**
     * Logs an anonymous action (e.g. failed login attempts, registration before login, etc.).
     * @param action
     * @param details
     */
    public static void logAnonymous(String action, String details) {
        log(null, "ANONYMOUS", action, details);
    }
}
