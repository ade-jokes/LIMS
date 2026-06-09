package com.sante.lims.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import com.sante.lims.App;
import com.sante.lims.model.AuditLog;
import com.sante.lims.model.TestRequest;
import com.sante.lims.model.TestType;
import com.sante.lims.model.User;
import com.sante.lims.utils.AuditService;
import com.sante.lims.utils.DBConnection;
import com.sante.lims.utils.SecurityUtils;

public class AdminDashboardController {

    // Header info
    @FXML private Label adminNameLabel;
    @FXML private Label adminEmailLabel;

    // Tab 1: Custom Test Builder
    @FXML private TextField testNameField;
    @FXML private TextField testPriceField;
    @FXML private TextField testTatField;
    @FXML private ComboBox<String> testFormatBox;
    @FXML private Label testErrorLabel;
    @FXML private Button saveTestButton;
    @FXML private TableView<TestType> testTypeTable;
    @FXML private TableColumn<TestType, Integer> colTestId;
    @FXML private TableColumn<TestType, String> colTestName;
    @FXML private TableColumn<TestType, Double> colTestPrice;
    @FXML private TableColumn<TestType, Integer> colTestTat;
    @FXML private TableColumn<TestType, String> colTestFormat;

    // Tab 2: Request Queue
    @FXML private TableView<TestRequest> requestTable;
    @FXML private TableColumn<TestRequest, Integer> colReqId;
    @FXML private TableColumn<TestRequest, String> colReqPatient;
    @FXML private TableColumn<TestRequest, String> colReqTest;
    @FXML private TableColumn<TestRequest, String> colReqPayment;
    @FXML private TableColumn<TestRequest, String> colReqEvidence;
    @FXML private TableColumn<TestRequest, String> colReqSample;
    @FXML private TableColumn<TestRequest, Timestamp> colReqDate;

    // Tab 3: User Provisioning
    @FXML private TextField usrNameField;
    @FXML private TextField usrEmailField;
    @FXML private PasswordField usrPasswordField;
    @FXML private ComboBox<String> usrRoleBox;
    @FXML private Label usrErrorLabel;
    @FXML private Button provisionButton;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colUsrId;
    @FXML private TableColumn<User, String> colUsrName;
    @FXML private TableColumn<User, String> colUsrEmail;
    @FXML private TableColumn<User, String> colUsrRole;

    // Tab 4: Bank Details
    @FXML private TextField bankNameField;
    @FXML private TextField bankAccountNameField;
    @FXML private TextField bankAccountNumberField;
    @FXML private TextField bankSortCodeField;
    @FXML private TextField bankInstructionsField;
    @FXML private Label bankErrorLabel;

    // Tab 5: Audit Trail
    @FXML private TextField auditSearchField;
    @FXML private TableView<AuditLog> auditTable;
    @FXML private TableColumn<AuditLog, Integer> colAuditId;
    @FXML private TableColumn<AuditLog, String> colAuditUser;
    @FXML private TableColumn<AuditLog, String> colAuditAction;
    @FXML private TableColumn<AuditLog, String> colAuditDetails;
    @FXML private TableColumn<AuditLog, Timestamp> colAuditTime;

    private User adminUser;
    private ObservableList<TestType> testTypesList = FXCollections.observableArrayList();
    private ObservableList<TestRequest> requestsList = FXCollections.observableArrayList();
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private ObservableList<AuditLog> auditList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        adminUser = App.getCurrentUser();
        if (adminUser != null) {
            adminNameLabel.setText(adminUser.getName());
            adminEmailLabel.setText(adminUser.getEmail());
        }

        testFormatBox.setItems(FXCollections.observableArrayList("numeric", "text", "PDF", "image"));
        usrRoleBox.setItems(FXCollections.observableArrayList("LAB_ATTENDANT", "CUSTOMER"));

        colTestId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTestName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTestPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTestTat.setCellValueFactory(new PropertyValueFactory<>("tatHours"));
        colTestFormat.setCellValueFactory(new PropertyValueFactory<>("resultFormat"));

        colReqId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colReqPatient.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colReqTest.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colReqPayment.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colReqEvidence.setCellValueFactory(new PropertyValueFactory<>("paymentEvidence"));
        colReqSample.setCellValueFactory(new PropertyValueFactory<>("sampleStatus"));
        colReqDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        colUsrId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsrName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colUsrEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUsrRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        colAuditId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuditUser.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
        colAuditAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAuditDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colAuditTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        loadTestTypes();
        loadRequests();
        loadUsers();
        loadAuditLogs();
        loadBankDetails();

        testErrorLabel.setVisible(false);
        usrErrorLabel.setVisible(false);
        if (bankErrorLabel != null) bankErrorLabel.setVisible(false);
    }

    // ========== TAB 1: TEST TYPES CRUD ==========

    private void loadTestTypes() {
        testTypesList.clear();
        String sql = "SELECT * FROM test_types ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                testTypesList.add(new TestType(
                        rs.getInt("id"), rs.getString("name"), rs.getDouble("price"),
                        rs.getInt("tat_hours"), rs.getString("result_format"), rs.getTimestamp("created_at")));
            }
            testTypeTable.setItems(testTypesList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleSaveTest(ActionEvent event) {
        String name = testNameField.getText().trim();
        String priceStr = testPriceField.getText().trim();
        String tatStr = testTatField.getText().trim();
        String format = testFormatBox.getValue();

        if (name.isEmpty() || priceStr.isEmpty() || tatStr.isEmpty() || format == null) {
            showTestError("All fields are required."); return;
        }

        double price;
        try { price = Double.parseDouble(priceStr); if (price < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { showTestError("Price must be a valid positive number."); return; }

        int tat;
        try { tat = Integer.parseInt(tatStr); if (tat < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { showTestError("TAT must be a valid positive integer."); return; }

        String sql = "INSERT INTO test_types (name, price, tat_hours, result_format) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name); pstmt.setDouble(2, price);
            pstmt.setInt(3, tat); pstmt.setString(4, format);
            pstmt.executeUpdate();
            AuditService.log(adminUser.getId(), adminUser.getEmail(), "CREATE_TEST_TYPE",
                    "Defined new test type: " + name + " (TAT: " + tat + "h, Price: $" + price + ")");
            testNameField.clear(); testPriceField.clear(); testTatField.clear(); testFormatBox.setValue(null);
            testErrorLabel.setVisible(false);
            loadTestTypes(); loadAuditLogs();
            showInfo("Success", "Test type created successfully!");
        } catch (SQLException e) { e.printStackTrace(); showTestError("Failed to save test type."); }
    }

    @FXML
    void handleEditTestType(ActionEvent event) {
        TestType selected = testTypeTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select a test type to edit."); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Test Type");
        dialog.setHeaderText("Edit: " + selected.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameF = new TextField(selected.getName());
        TextField priceF = new TextField(String.valueOf(selected.getPrice()));
        TextField tatF = new TextField(String.valueOf(selected.getTatHours()));
        ComboBox<String> formatC = new ComboBox<>(FXCollections.observableArrayList("numeric", "text", "PDF", "image"));
        formatC.setValue(selected.getResultFormat());

        grid.add(new Label("Test Name:"), 0, 0); grid.add(nameF, 1, 0);
        grid.add(new Label("Price ($):"), 0, 1); grid.add(priceF, 1, 1);
        grid.add(new Label("TAT (Hours):"), 0, 2); grid.add(tatF, 1, 2);
        grid.add(new Label("Result Format:"), 0, 3); grid.add(formatC, 1, 3);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String newName = nameF.getText().trim();
            String newPriceStr = priceF.getText().trim();
            String newTatStr = tatF.getText().trim();
            String newFormat = formatC.getValue();
            if (newName.isEmpty() || newPriceStr.isEmpty() || newTatStr.isEmpty() || newFormat == null) {
                showAlert("Validation Error", "All fields are required."); return;
            }
            try {
                double newPrice = Double.parseDouble(newPriceStr);
                int newTat = Integer.parseInt(newTatStr);
                String sql = "UPDATE test_types SET name=?, price=?, tat_hours=?, result_format=? WHERE id=?";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, newName); pstmt.setDouble(2, newPrice);
                    pstmt.setInt(3, newTat); pstmt.setString(4, newFormat);
                    pstmt.setInt(5, selected.getId());
                    pstmt.executeUpdate();
                    AuditService.log(adminUser.getId(), adminUser.getEmail(), "UPDATE_TEST_TYPE",
                            "Updated test type ID " + selected.getId() + ": " + selected.getName() + " → " + newName);
                    loadTestTypes(); loadAuditLogs();
                    showInfo("Updated", "Test type updated successfully.");
                }
            } catch (NumberFormatException ex) { showAlert("Invalid Input", "Price and TAT must be valid numbers.");
            } catch (SQLException ex) { ex.printStackTrace(); showAlert("Error", "Failed to update test type."); }
        }
    }

    @FXML
    void handleDeleteTestType(ActionEvent event) {
        TestType selected = testTypeTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select a test type to delete."); return; }

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete test type: " + selected.getName() + "?");
        confirm.setContentText("This cannot be undone. Test requests using this type will be blocked from deletion by the database.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String sql = "DELETE FROM test_types WHERE id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();
                AuditService.log(adminUser.getId(), adminUser.getEmail(), "DELETE_TEST_TYPE",
                        "Deleted test type ID " + selected.getId() + ": " + selected.getName());
                loadTestTypes(); loadAuditLogs();
                showInfo("Deleted", "Test type deleted successfully.");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Cannot Delete", "This test type is used by existing requests and cannot be deleted.");
            }
        }
    }

    private void showTestError(String message) {
        testErrorLabel.setText(message); testErrorLabel.setVisible(true);
    }

    // ========== TAB 2: REQUESTS CRUD ==========

    private void loadRequests() {
        requestsList.clear();
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, "
                   + "tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format "
                   + "FROM test_requests tr JOIN users u ON tr.customer_id = u.id "
                   + "JOIN test_types tt ON tr.test_type_id = tt.id ORDER BY tr.id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TestRequest tr = new TestRequest();
                tr.setId(rs.getInt("id")); tr.setCustomerId(rs.getInt("customer_id"));
                tr.setCustomerName(rs.getString("customer_name")); tr.setCustomerEmail(rs.getString("customer_email"));
                tr.setTestTypeId(rs.getInt("test_type_id")); tr.setTestTypeName(rs.getString("test_name"));
                tr.setTestTypePrice(rs.getDouble("test_price")); tr.setTestTypeTatHours(rs.getInt("test_tat"));
                tr.setTestTypeResultFormat(rs.getString("test_format"));
                tr.setPaymentStatus(rs.getString("payment_status")); tr.setPaymentEvidence(rs.getString("payment_evidence"));
                tr.setSampleStatus(rs.getString("sample_status")); tr.setCreatedAt(rs.getTimestamp("created_at"));
                tr.setCollectedAt(rs.getTimestamp("collected_at")); tr.setProcessedAt(rs.getTimestamp("processed_at"));
                tr.setValidatedAt(rs.getTimestamp("validated_at")); tr.setDueAt(rs.getTimestamp("due_at"));
                requestsList.add(tr);
            }
            requestTable.setItems(requestsList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleMarkAsPaid(ActionEvent event) {
        TestRequest selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Request Selected", "Please select a test request to mark as Paid."); return; }
        if ("Paid".equals(selected.getPaymentStatus())) { showInfo("Already Paid", "This request is already marked as Paid."); return; }

        String sql = "UPDATE test_requests SET payment_status = 'Paid' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, selected.getId()); pstmt.executeUpdate();
            AuditService.log(adminUser.getId(), adminUser.getEmail(), "CONFIRM_PAYMENT",
                    "Confirmed payment for Request ID: " + selected.getId() + " (Patient: " + selected.getCustomerEmail() + ")");
            loadRequests(); loadAuditLogs();
            showInfo("Payment Confirmed", "Request has been marked as Paid successfully.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleDeleteRequest(ActionEvent event) {
        TestRequest selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select a request to delete."); return; }

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Request #" + selected.getId() + " for " + selected.getCustomerName() + "?");
        confirm.setContentText("This will permanently delete the request and any associated results.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            
            String deleteResultsSql = "DELETE FROM results WHERE request_id = ?";
            String deleteRequestSql = "DELETE FROM test_requests WHERE id = ?";
            
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false); // Start transaction block
                
                try (PreparedStatement psResults = conn.prepareStatement(deleteResultsSql);
                     PreparedStatement psRequest = conn.prepareStatement(deleteRequestSql)) {
                    
                    // 1. Delete dependent results first
                    psResults.setInt(1, selected.getId());
                    psResults.executeUpdate();
                    
                    // 2. Delete the parent request
                    psRequest.setInt(1, selected.getId());
                    psRequest.executeUpdate();
                    
                    conn.commit(); // Commit both actions together
                } catch (SQLException innerEx) {
                    conn.rollback(); // Undo everything if either fails
                    throw innerEx;
                }

                AuditService.log(adminUser.getId(), adminUser.getEmail(), "DELETE_REQUEST",
                        "Deleted Request ID " + selected.getId() + " for patient: " + selected.getCustomerEmail());
                
                loadRequests(); 
                loadAuditLogs();
                showInfo("Deleted", "Test request and associated results deleted successfully.");
                
            } catch (SQLException e) { 
                e.printStackTrace(); 
                showAlert("Error", "Failed to delete request: " + e.getMessage()); 
            }
        }
    }

    // ========== TAB 3: USERS CRUD ==========

    private void loadUsers() {
        usersList.clear();
        // Load only active users
        String sql = "SELECT * FROM users WHERE is_active = true ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usersList.add(new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                        rs.getString("password_hash"), rs.getString("role"), rs.getBoolean("must_change_password"),
                        rs.getBoolean("is_verified"), rs.getString("verification_code"), rs.getTimestamp("created_at")));
            }
            userTable.setItems(usersList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleProvisionUser(ActionEvent event) {
        String name = usrNameField.getText().trim();
        String email = usrEmailField.getText().trim();
        String password = usrPasswordField.getText();
        String role = usrRoleBox.getValue();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            showUsrError("All fields are required."); return;
        }
        if (password.length() < 6) { showUsrError("Password must be at least 6 characters."); return; }

        String hash = SecurityUtils.hashPassword(password);
        String sql = "INSERT INTO users (name, email, password_hash, role, must_change_password, is_verified) VALUES (?, ?, ?, ?, true, true)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name); pstmt.setString(2, email);
            pstmt.setString(3, hash); pstmt.setString(4, role);
            pstmt.executeUpdate();
            AuditService.log(adminUser.getId(), adminUser.getEmail(), "PROVISION_USER",
                    "Created staff/customer account: " + email + " (" + role + ")");
            usrNameField.clear(); usrEmailField.clear(); usrPasswordField.clear(); usrRoleBox.setValue(null);
            usrErrorLabel.setVisible(false);
            loadUsers(); loadAuditLogs();
            showInfo("Success", "User account created successfully! The user will be required to change their password on first login.");
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) showUsrError("Email address is already in use.");
            else { e.printStackTrace(); showUsrError("An error occurred. Please try again."); }
        }
    }

    @FXML
    void handleEditUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select a user to edit."); return; }
        if (selected.getRole().equals("SUPER_ADMIN")) { showAlert("Restricted", "Cannot edit a Super Admin account."); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit: " + selected.getEmail());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField nameF = new TextField(selected.getName());
        TextField emailF = new TextField(selected.getEmail());
        ComboBox<String> roleC = new ComboBox<>(FXCollections.observableArrayList("LAB_ATTENDANT", "CUSTOMER"));
        roleC.setValue(selected.getRole());
        PasswordField passF = new PasswordField();
        passF.setPromptText("Leave blank to keep current");

        grid.add(new Label("Full Name:"), 0, 0); grid.add(nameF, 1, 0);
        grid.add(new Label("Email:"), 0, 1); grid.add(emailF, 1, 1);
        grid.add(new Label("Role:"), 0, 2); grid.add(roleC, 1, 2);
        grid.add(new Label("New Password:"), 0, 3); grid.add(passF, 1, 3);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String newName = nameF.getText().trim();
            String newEmail = emailF.getText().trim();
            String newRole = roleC.getValue();
            String newPass = passF.getText();

            if (newName.isEmpty() || newEmail.isEmpty() || newRole == null) {
                showAlert("Validation Error", "Name, email and role are required."); return;
            }

            try {
                if (!newPass.isEmpty()) {
                    if (newPass.length() < 6) { showAlert("Validation Error", "Password must be at least 6 characters."); return; }
                    String hash = SecurityUtils.hashPassword(newPass);
                    String sql = "UPDATE users SET name=?, email=?, role=?, password_hash=?, must_change_password=true WHERE id=?";
                    try (Connection conn = DBConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, newName); pstmt.setString(2, newEmail);
                        pstmt.setString(3, newRole); pstmt.setString(4, hash); pstmt.setInt(5, selected.getId());
                        pstmt.executeUpdate();
                    }
                } else {
                    String sql = "UPDATE users SET name=?, email=?, role=? WHERE id=?";
                    try (Connection conn = DBConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, newName); pstmt.setString(2, newEmail);
                        pstmt.setString(3, newRole); pstmt.setInt(4, selected.getId());
                        pstmt.executeUpdate();
                    }
                }
                AuditService.log(adminUser.getId(), adminUser.getEmail(), "UPDATE_USER",
                        "Updated user ID " + selected.getId() + ": " + selected.getEmail() + " → " + newEmail + " (" + newRole + ")");
                loadUsers(); loadAuditLogs();
                showInfo("Updated", "User updated successfully.");
            } catch (SQLException ex) {
                if (ex.getSQLState().equals("23505")) showAlert("Duplicate Email", "That email is already in use.");
                else { ex.printStackTrace(); showAlert("Error", "Failed to update user."); }
            }
        }
    }

    @FXML
    void handleDeactivateUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select a user to deactivate."); return; }
        if (selected.getRole().equals("SUPER_ADMIN")) { showAlert("Restricted", "Cannot deactivate a Super Admin account."); return; }
        if (adminUser.getId() == selected.getId()) { showAlert("Restricted", "You cannot deactivate your own account."); return; }

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deactivation");
        confirm.setHeaderText("Deactivate user: " + selected.getEmail() + "?");
        confirm.setContentText("This will prevent the user from logging in, but preserve their audit history.");
        
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            // Soft Delete: Flip the flag!
            String deactivateSql = "UPDATE users SET is_active = false WHERE id = ?";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(deactivateSql)) {
                
                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();
                
                AuditService.log(adminUser.getId(), adminUser.getEmail(), "DEACTIVATE_USER",
                        "Deactivated user ID " + selected.getId() + ": " + selected.getEmail() + " (" + selected.getRole() + ")");
                
                loadUsers(); 
                loadAuditLogs();
                showInfo("Deactivated", "User account deactivated successfully.");
            } catch (SQLException e) { 
                e.printStackTrace(); 
                showAlert("Error", "Failed to deactivate user: " + e.getMessage()); 
            }
        }
    }

    private void showUsrError(String message) {
        usrErrorLabel.setText(message); usrErrorLabel.setVisible(true);
    }

    // ========== TAB 4: BANK DETAILS CRUD ==========

    private void loadBankDetails() {
        String sql = "SELECT * FROM bank_details ORDER BY id ASC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                if (bankNameField != null) bankNameField.setText(rs.getString("bank_name") != null ? rs.getString("bank_name") : "");
                if (bankAccountNameField != null) bankAccountNameField.setText(rs.getString("account_name") != null ? rs.getString("account_name") : "");
                if (bankAccountNumberField != null) bankAccountNumberField.setText(rs.getString("account_number") != null ? rs.getString("account_number") : "");
                if (bankSortCodeField != null) bankSortCodeField.setText(rs.getString("sort_code") != null ? rs.getString("sort_code") : "");
                if (bankInstructionsField != null) bankInstructionsField.setText(rs.getString("instructions") != null ? rs.getString("instructions") : "");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleSaveBankDetails(ActionEvent event) {
        if (bankNameField == null) return;
        String bName = bankNameField.getText().trim();
        String bAccName = bankAccountNameField.getText().trim();
        String bAccNum = bankAccountNumberField.getText().trim();
        String bSort = bankSortCodeField.getText().trim();
        String bInstr = bankInstructionsField.getText().trim();

        if (bName.isEmpty() || bAccName.isEmpty() || bAccNum.isEmpty()) {
            if (bankErrorLabel != null) { bankErrorLabel.setText("Bank name, account name and account number are required."); bankErrorLabel.setVisible(true); }
            return;
        }

        // Upsert: update if exists, otherwise insert
        String checkSql = "SELECT COUNT(*) FROM bank_details";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            rs.next();
            int count = rs.getInt(1);
            String sql;
            if (count > 0) {
                sql = "UPDATE bank_details SET bank_name=?, account_name=?, account_number=?, sort_code=?, instructions=?, updated_at=CURRENT_TIMESTAMP WHERE id=(SELECT id FROM bank_details ORDER BY id ASC LIMIT 1)";
            } else {
                sql = "INSERT INTO bank_details (bank_name, account_name, account_number, sort_code, instructions) VALUES (?, ?, ?, ?, ?)";
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, bName); pstmt.setString(2, bAccName);
                pstmt.setString(3, bAccNum); pstmt.setString(4, bSort.isEmpty() ? "N/A" : bSort);
                pstmt.setString(5, bInstr);
                pstmt.executeUpdate();
                AuditService.log(adminUser.getId(), adminUser.getEmail(), "UPDATE_BANK_DETAILS",
                        "Updated bank details: " + bName + " | " + bAccName + " | Acct: " + bAccNum);
                if (bankErrorLabel != null) bankErrorLabel.setVisible(false);
                loadAuditLogs();
                showInfo("Saved", "Bank details updated successfully.");
            }
        } catch (SQLException e) { e.printStackTrace(); showAlert("Error", "Failed to save bank details."); }
    }

    // ========== TAB 5: AUDIT TRAIL ==========

    @FXML
    void handleRefreshAudit(ActionEvent event) { loadAuditLogs(); }

    private void loadAuditLogs() {
        auditList.clear();
        String sql = "SELECT * FROM audit_trail ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                auditList.add(new AuditLog(rs.getInt("id"),
                        rs.getObject("user_id") != null ? rs.getInt("user_id") : null,
                        rs.getString("user_email"), rs.getString("action"),
                        rs.getString("details"), rs.getTimestamp("timestamp")));
            }
            FilteredList<AuditLog> filteredLogs = new FilteredList<>(auditList, p -> true);
            auditSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredLogs.setPredicate(log -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    String lower = newValue.toLowerCase();
                    if (log.getUserEmail() != null && log.getUserEmail().toLowerCase().contains(lower)) return true;
                    if (log.getAction() != null && log.getAction().toLowerCase().contains(lower)) return true;
                    if (log.getDetails() != null && log.getDetails().toLowerCase().contains(lower)) return true;
                    return false;
                });
            });
            auditTable.setItems(filteredLogs);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ========== GENERAL ==========

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.show();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.show();
    }

    @FXML
    void handleLogout(ActionEvent event) {
        AuditService.log(adminUser.getId(), adminUser.getEmail(), "LOGOUT", "User logged out.");
        App.setCurrentUser(null);
        App.switchScene("login.fxml", "Sante Diagnostics - Login", 800, 600);
    }
}