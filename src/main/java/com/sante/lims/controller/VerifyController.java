package com.sante.lims.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.sante.lims.App;
import com.sante.lims.model.User;
import com.sante.lims.utils.AuditService;
import com.sante.lims.utils.DBConnection;
import com.sante.lims.utils.EmailService;

public class VerifyController {

    @FXML
    private TextField codeField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button verifyButton;

    private User currentUser;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        currentUser = App.getCurrentUser();
        if (currentUser == null) {
            Platform.runLater(this::handleBackToLogin);
        }
    }

    @FXML
    void handleVerify(ActionEvent event) {
        String enteredCode = codeField.getText().trim();
        if (enteredCode.isEmpty()) {
            showError("Please enter the verification code.");
            return;
        }

        if (currentUser.getVerificationCode() != null && currentUser.getVerificationCode().equals(enteredCode)) {
            String updateSql = "UPDATE users SET is_verified = true, verification_code = NULL WHERE id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

                pstmt.setInt(1, currentUser.getId());
                pstmt.executeUpdate();

                // Update session state
                currentUser.setVerified(true);
                currentUser.setVerificationCode(null);

                AuditService.log(currentUser.getId(), currentUser.getEmail(), "EMAIL_VERIFY_SUCCESS", "Patient email successfully verified.");

                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Verification Successful");
                alert.setHeaderText(null);
                alert.setContentText("Your email has been verified! Welcome to Sante Diagnostics.");
                alert.showAndWait();

                // Route to customer dashboard
                App.switchScene("customer_dashboard.fxml", "Sante Diagnostics - Patient Hub", 1024, 768);

            } catch (SQLException e) {
                e.printStackTrace();
                showError("A database error occurred. Please try again.");
            }
        } else {
            AuditService.log(currentUser.getId(), currentUser.getEmail(), "EMAIL_VERIFY_FAILED", "Entered invalid verification code: " + enteredCode);
            showError("Invalid verification code. Please check your email and try again.");
        }
    }

    @FXML
    void handleResendCode(ActionEvent event) {
        int codeInt = (int)(Math.random() * 900000) + 100000;
        String newCode = String.valueOf(codeInt);

        String updateSql = "UPDATE users SET verification_code = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            pstmt.setString(1, newCode);
            pstmt.setInt(2, currentUser.getId());
            pstmt.executeUpdate();

            // Update session state
            currentUser.setVerificationCode(newCode);

            AuditService.log(currentUser.getId(), currentUser.getEmail(), "EMAIL_VERIFY_RESEND", "Requested a new verification code.");

            // Send new email
            EmailService.setLastVerificationCode(newCode);
            String emailSubject = "New Verification Code - Sante Diagnostics";
            String emailBody = "Dear " + currentUser.getName() + ",\n\n"
                    + "You requested a new verification code for your Sante Diagnostics account.\n"
                    + "Please enter the following code:\n\n"
                    + "Verification Code: " + newCode + "\n\n"
                    + "Best regards,\n"
                    + "Sante Diagnostics Team";

            EmailService.sendEmail(currentUser.getEmail(), emailSubject, emailBody);

            // UI dialog if SMTP disabled
            if (!EmailService.isSmtpConfigured()) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("New Code [DEV MODE]");
                alert.setHeaderText("New Code Logged (SMTP Disabled)");
                alert.setContentText("A new verification code was logged. Code: " + newCode);
                alert.showAndWait();
            } else {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Code Sent");
                alert.setHeaderText(null);
                alert.setContentText("A new verification code has been sent to your email.");
                alert.showAndWait();
            }

            errorLabel.setVisible(false);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to generate a new verification code. Please try again.");
        }
    }

    @FXML
    void handleBackToLogin(ActionEvent event) {
        handleBackToLogin();
    }

    private void handleBackToLogin() {
        App.setCurrentUser(null);
        App.switchScene("login.fxml", "Sante Diagnostics - Login", 800, 600);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
