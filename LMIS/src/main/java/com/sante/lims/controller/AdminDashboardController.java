package com.sante.lims.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
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

    // Tab 4: Audit Trail
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

        // Setup formats
        testFormatBox.setItems(FXCollections.observableArrayList("numeric", "text", "PDF", "image"));
        usrRoleBox.setItems(FXCollections.observableArrayList("LAB_ATTENDANT", "CUSTOMER"));

        // Setup Test Type columns
        colTestId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTestName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTestPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTestTat.setCellValueFactory(new PropertyValueFactory<>("tatHours"));
        colTestFormat.setCellValueFactory(new PropertyValueFactory<>("resultFormat"));

        // Setup Request columns
        colReqId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colReqPatient.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colReqTest.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colReqPayment.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colReqEvidence.setCellValueFactory(new PropertyValueFactory<>("paymentEvidence"));
        colReqSample.setCellValueFactory(new PropertyValueFactory<>("sampleStatus"));
        colReqDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Setup User columns
        colUsrId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsrName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colUsrEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUsrRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Setup Audit columns
        colAuditId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuditUser.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
        colAuditAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAuditDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colAuditTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Load data
        loadTestTypes();
        loadRequests();
        loadUsers();
        loadAuditLogs();
        
        testErrorLabel.setVisible(false);
        usrErrorLabel.setVisible(false);
    }

    // --- Tab 1: Test Builder Actions ---

    private void loadTestTypes() {
        testTypesList.clear();
        String sql = "SELECT * FROM test_types ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                testTypesList.add(new TestType(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("tat_hours"),
                        rs.getString("result_format"),
                        rs.getTimestamp("created_at")
                ));
            }
            testTypeTable.setItems(testTypesList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSaveTest(ActionEvent event) {
        String name = testNameField.getText().trim();
        String priceStr = testPriceField.getText().trim();
        String tatStr = testTatField.getText().trim();
        String format = testFormatBox.getValue();

        if (name.isEmpty() || priceStr.isEmpty() || tatStr.isEmpty() || format == null) {
            showTestError("All fields are required.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showTestError("Price must be a valid positive number.");
            return;
        }

        int tat;
        try {
            tat = Integer.parseInt(tatStr);
            if (tat < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showTestError("TAT must be a valid positive integer.");
            return;
        }

        String sql = "INSERT INTO test_types (name, price, tat_hours, result_format) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, tat);
            pstmt.setString(4, format);
            pstmt.executeUpdate();

            AuditService.log(adminUser.getId(), adminUser.getEmail(), "CREATE_TEST_TYPE", "Defined new test type: " + name + " (TAT: " + tat + "h, Price: $" + price + ")");
            
            // Clear inputs
            testNameField.clear();
            testPriceField.clear();
            testTatField.clear();
            testFormatBox.setValue(null);
            testErrorLabel.setVisible(false);

            loadTestTypes();

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Test type defined successfully!");
            alert.show();

        } catch (SQLException e) {
            e.printStackTrace();
            showTestError("Failed to save test type. Please try again.");
        }
    }

    private void showTestError(String message) {
        testErrorLabel.setText(message);
        testErrorLabel.setVisible(true);
    }

    // --- Tab 2: Request Queue Actions ---

    private void loadRequests() {
        requestsList.clear();
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format "
                   + "FROM test_requests tr "
                   + "JOIN users u ON tr.customer_id = u.id "
                   + "JOIN test_types tt ON tr.test_type_id = tt.id "
                   + "ORDER BY tr.id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TestRequest tr = new TestRequest();
                tr.setId(rs.getInt("id"));
                tr.setCustomerId(rs.getInt("customer_id"));
                tr.setCustomerName(rs.getString("customer_name"));
                tr.setCustomerEmail(rs.getString("customer_email"));
                tr.setTestTypeId(rs.getInt("test_type_id"));
                tr.setTestTypeName(rs.getString("test_name"));
                tr.setTestTypePrice(rs.getDouble("test_price"));
                tr.setTestTypeTatHours(rs.getInt("test_tat"));
                tr.setTestTypeResultFormat(rs.getString("test_format"));
                tr.setPaymentStatus(rs.getString("payment_status"));
                tr.setPaymentEvidence(rs.getString("payment_evidence"));
                tr.setSampleStatus(rs.getString("sample_status"));
                tr.setCreatedAt(rs.getTimestamp("created_at"));
                tr.setCollectedAt(rs.getTimestamp("collected_at"));
                tr.setProcessedAt(rs.getTimestamp("processed_at"));
                tr.setValidatedAt(rs.getTimestamp("validated_at"));
                tr.setDueAt(rs.getTimestamp("due_at"));
                
                requestsList.add(tr);
            }
            requestTable.setItems(requestsList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleMarkAsPaid(ActionEvent event) {
        TestRequest selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("No Request Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a test request from the table to mark as Paid.");
            alert.show();
            return;
        }

        if ("Paid".equals(selected.getPaymentStatus())) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Already Paid");
            alert.setHeaderText(null);
            alert.setContentText("This request is already marked as Paid.");
            alert.show();
            return;
        }

        String sql = "UPDATE test_requests SET payment_status = 'Paid' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();

            AuditService.log(adminUser.getId(), adminUser.getEmail(), "CONFIRM_PAYMENT", 
                             "Confirmed payment for Request ID: " + selected.getId() + " (Patient: " + selected.getCustomerEmail() + ")");

            loadRequests();

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Payment Confirmed");
            alert.setHeaderText(null);
            alert.setContentText("Request has been marked as Paid successfully.");
            alert.show();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Tab 3: User Provisioning Actions ---

    private void loadUsers() {
        usersList.clear();
        String sql = "SELECT * FROM users ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usersList.add(new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getBoolean("must_change_password"),
                        rs.getBoolean("is_verified"),
                        rs.getString("verification_code"),
                        rs.getTimestamp("created_at")
                ));
            }
            userTable.setItems(usersList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleProvisionUser(ActionEvent event) {
        String name = usrNameField.getText().trim();
        String email = usrEmailField.getText().trim();
        String password = usrPasswordField.getText();
        String role = usrRoleBox.getValue();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            showUsrError("All fields are required.");
            return;
        }

        if (password.length() < 6) {
            showUsrError("Password must be at least 6 characters.");
            return;
        }

        String hash = SecurityUtils.hashPassword(password);
        String sql = "INSERT INTO users (name, email, password_hash, role, must_change_password, is_verified) VALUES (?, ?, ?, ?, true, true)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, hash);
            pstmt.setString(4, role);
            pstmt.executeUpdate();

            AuditService.log(adminUser.getId(), adminUser.getEmail(), "PROVISION_USER", 
                             "Created staff/customer account: " + email + " (" + role + ")");

            usrNameField.clear();
            usrEmailField.clear();
            usrPasswordField.clear();
            usrRoleBox.setValue(null);
            usrErrorLabel.setVisible(false);

            loadUsers();

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("User account created successfully! The user will be required to change their password on first login.");
            alert.show();

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                showUsrError("Email address is already in use.");
            } else {
                e.printStackTrace();
                showUsrError("An error occurred. Please try again.");
            }
        }
    }

    private void showUsrError(String message) {
        usrErrorLabel.setText(message);
        usrErrorLabel.setVisible(true);
    }

    // --- Tab 4: Audit Trail Actions ---

    private void loadAuditLogs() {
        auditList.clear();
        String sql = "SELECT * FROM audit_trail ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                auditList.add(new AuditLog(
                        rs.getInt("id"),
                        rs.getObject("user_id") != null ? rs.getInt("user_id") : null,
                        rs.getString("user_email"),
                        rs.getString("action"),
                        rs.getString("details"),
                        rs.getTimestamp("timestamp")
                ));
            }

            // Set up search filter binding
            FilteredList<AuditLog> filteredLogs = new FilteredList<>(auditList, p -> true);
            auditSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredLogs.setPredicate(log -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lower = newValue.toLowerCase();
                    if (log.getUserEmail() != null && log.getUserEmail().toLowerCase().contains(lower)) {
                        return true;
                    }
                    if (log.getAction() != null && log.getAction().toLowerCase().contains(lower)) {
                        return true;
                    }
                    if (log.getDetails() != null && log.getDetails().toLowerCase().contains(lower)) {
                        return true;
                    }
                    return false;
                });
            });
            auditTable.setItems(filteredLogs);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- General Action ---

    @FXML
    void handleLogout(ActionEvent event) {
        AuditService.log(adminUser.getId(), adminUser.getEmail(), "LOGOUT", "User logged out.");
        App.setCurrentUser(null);
        App.switchScene("login.fxml", "Sante Diagnostics - Login", 800, 600);
    }
}
