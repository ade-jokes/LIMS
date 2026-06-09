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
import javafx.scene.control.PasswordField;
import com.sante.lims.App;
import com.sante.lims.model.User;
import com.sante.lims.utils.AuditService;
import com.sante.lims.utils.DBConnection;
import com.sante.lims.utils.SecurityUtils;

public class PasswordChangeController {

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button changeButton;

    private User currentUser;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        currentUser = App.getCurrentUser();
        if (currentUser == null) {
            Platform.runLater(this::handleCancel);
        }
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        String newPassword = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (newPassword.isEmpty() || confirm.isEmpty()) {
            showError("Both fields are required.");
            return;
        }

        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters long.");
            return;
        }

        if (!newPassword.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        String hashedPassword = SecurityUtils.hashPassword(newPassword);
        String updateSql = "UPDATE users SET password_hash = ?, must_change_password = false WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, currentUser.getId());
            pstmt.executeUpdate();

            // Update session user state
            currentUser.setPasswordHash(hashedPassword);
            currentUser.setMustChangePassword(false);

            AuditService.log(currentUser.getId(), currentUser.getEmail(), "PASSWORD_CHANGE_FORCE", "Successfully updated temporary password.");

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Password Updated");
            alert.setHeaderText(null);
            alert.setContentText("Your password has been changed successfully.");
            alert.showAndWait();

            // Route user to their dashboard
            routeToDashboard(currentUser);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("An error occurred while updating password. Please try again.");
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
    void handleCancel(ActionEvent event) {
        handleCancel();
    }

    private void handleCancel() {
        App.setCurrentUser(null);
        App.switchScene("login.fxml", "Sante Diagnostics - Login", 800, 600);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
