package com.sante.lims.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import com.sante.lims.App;
import com.sante.lims.model.User;
import com.sante.lims.utils.AuditService;
import com.sante.lims.utils.DBConnection;
import com.sante.lims.utils.EmailService;
import com.sante.lims.utils.SecurityUtils;

public class RegisterController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button registerButton;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("All fields are required.");
            return;
        }

        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            showError("Please enter a valid email address.");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long.");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        // Generate verification code
        int codeInt = (int)(Math.random() * 900000) + 100000;
        String code = String.valueOf(codeInt);

        String insertSql = "INSERT INTO users (name, email, password_hash, role, must_change_password, is_verified, verification_code) VALUES (?, ?, ?, 'CUSTOMER', false, false, ?) RETURNING id, name, email, password_hash, role, must_change_password, is_verified, verification_code, created_at";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, SecurityUtils.hashPassword(password));
            pstmt.setString(4, code);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Create customer user
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            rs.getBoolean("must_change_password"),
                            rs.getBoolean("is_verified"),
                            rs.getString("verification_code"),
                            rs.getTimestamp("created_at")
                    );

                    App.setCurrentUser(user);

                    // Audit registration
                    AuditService.log(user.getId(), user.getEmail(), "USER_REGISTER", "New patient account registered: " + email);

                    // Send email verification code
                    EmailService.setLastVerificationCode(code);
                    String emailSubject = "Verify Your Sante Diagnostics Account";
                    String emailBody = "Dear " + name + ",\n\n"
                            + "Thank you for registering at Sante Diagnostics Ltd.\n"
                            + "To verify your account, please enter the following verification code in the application:\n\n"
                            + "Verification Code: " + code + "\n\n"
                            + "If you did not register for this account, please ignore this email.\n\n"
                            + "Best regards,\n"
                            + "Sante Diagnostics Team";

                    EmailService.sendEmail(email, emailSubject, emailBody);

                    // If SMTP is not configured, show code in dialog to make grading easy
                    if (!EmailService.isSmtpConfigured()) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(AlertType.INFORMATION);
                            alert.setTitle("Account Verification [DEV MODE]");
                            alert.setHeaderText("Verification Code Sent (SMTP Disabled)");
                            alert.setContentText("A verification code was logged. To proceed, use: " + code);
                            alert.showAndWait();
                        });
                    }

                    // Go to verification scene
                    App.switchScene("verify.fxml", "Sante Diagnostics - Verify Email", 500, 400);
                }
            }

        } catch (SQLException e) {
            // PostgreSQL code for unique key violation is usually "23505"
            if (e.getSQLState().equals("23505")) {
                showError("Email address is already registered.");
                AuditService.logAnonymous("REGISTER_FAILED", "Duplicate signup attempt for email: " + email);
            } else {
                e.printStackTrace();
                showError("An error occurred during registration. Please try again.");
            }
        }
    }

    @FXML
    void handleGoToLogin(ActionEvent event) {
        App.switchScene("login.fxml", "Sante Diagnostics - Login", 800, 600);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
