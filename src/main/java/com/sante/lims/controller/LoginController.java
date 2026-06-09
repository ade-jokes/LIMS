package com.sante.lims.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.sante.lims.App;
import com.sante.lims.model.User;
import com.sante.lims.utils.AuditService;
import com.sante.lims.utils.DBConnection;
import com.sante.lims.utils.SecurityUtils;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hashed = rs.getString("password_hash");
                    if (SecurityUtils.checkPassword(password, hashed)) {
                        // Create user object
                        User user = new User(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("email"),
                                hashed,
                                rs.getString("role"),
                                rs.getBoolean("must_change_password"),
                                rs.getBoolean("is_verified"),
                                rs.getString("verification_code"),
                                rs.getTimestamp("created_at")
                        );

                        // Set session user
                        App.setCurrentUser(user);

                        // Audit log login success
                        AuditService.log(user.getId(), user.getEmail(), "LOGIN_SUCCESS", "User logged in successfully.");

                        // 1. Force Password Change check
                        if (user.isMustChangePassword()) {
                            App.switchScene("password_change.fxml", "Force Password Change", 500, 400);
                            return;
                        }

                        // 2. Email Verification check (Customer only)
                        if ("CUSTOMER".equals(user.getRole()) && !user.isVerified()) {
                            App.switchScene("verify.fxml", "Verify Your Email", 500, 400);
                            return;
                        }

                        // 3. Route to dashboard
                        routeToDashboard(user);

                    } else {
                        // Audit log login failure
                        AuditService.logAnonymous("LOGIN_FAILURE", "Failed login attempt for email: " + email + " (Incorrect Password)");
                        showError("Invalid email or password.");
                    }
                } else {
                    AuditService.logAnonymous("LOGIN_FAILURE", "Failed login attempt for email: " + email + " (User Not Found)");
                    showError("Invalid email or password.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("A database error occurred. Please try again.");
        }
    }

    private void routeToDashboard(User user) {
        if ("SUPER_ADMIN".equals(user.getRole())) {
            App.switchScene("admin_dashboard.fxml", "Sante Diagnostics - Super Admin Console", 1024, 768);
        } else if ("LAB_ATTENDANT".equals(user.getRole())) {
            App.switchScene("attendant_dashboard.fxml", "Sante Diagnostics - Lab Attendant Console", 1024, 768);
        } else if ("CUSTOMER".equals(user.getRole())) {
            App.switchScene("customer_dashboard.fxml", "Sante Diagnostics - Patient Hub", 1024, 768);
        }
    }

    @FXML
    void handleGoToRegister(ActionEvent event) {
        App.switchScene("register.fxml", "Sante Diagnostics - Register Account", 800, 600);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
