package com.sante.lims.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.sante.lims.App;
import com.sante.lims.model.TestRequest;
import com.sante.lims.model.User;
import com.sante.lims.utils.AuditService;
import com.sante.lims.utils.DBConnection;
import com.sante.lims.utils.EmailService;
import com.sante.lims.utils.SecurityUtils;

public class AttendantDashboardController {

    // Head Info
    @FXML private Label attendantNameLabel;
    @FXML private Label attendantEmailLabel;

    // Tab 1: Payments
    @FXML private TableView<TestRequest> paymentTable;
    @FXML private TableColumn<TestRequest, Integer> colPayId;
    @FXML private TableColumn<TestRequest, String> colPayPatient;
    @FXML private TableColumn<TestRequest, String> colPayTest;
    @FXML private TableColumn<TestRequest, Double> colPayPrice;
    @FXML private TableColumn<TestRequest, String> colPayStatus;
    @FXML private TableColumn<TestRequest, String> colPayEvidence;
    @FXML private TableColumn<TestRequest, Timestamp> colPayDate;

    // Tab 2: Sample Tracking
    @FXML private TableView<TestRequest> sampleTable;
    @FXML private TableColumn<TestRequest, Integer> colSampleId;
    @FXML private TableColumn<TestRequest, String> colSamplePatient;
    @FXML private TableColumn<TestRequest, String> colSampleTest;
    @FXML private TableColumn<TestRequest, String> colSamplePay;
    @FXML private TableColumn<TestRequest, String> colSampleStatus;
    @FXML private TableColumn<TestRequest, Timestamp> colSampleCollectedAt;
    @FXML private TableColumn<TestRequest, Timestamp> colSampleProcessedAt;
    @FXML private TableColumn<TestRequest, Timestamp> colSampleValidatedAt;
    @FXML private Button quickValidateButton;

    // Tab 3: Result Entry
    @FXML private TableView<TestRequest> processingTable;
    @FXML private TableColumn<TestRequest, Integer> colProcId;
    @FXML private TableColumn<TestRequest, String> colProcPatient;
    @FXML private TableColumn<TestRequest, String> colProcTest;
    @FXML private TableColumn<TestRequest, String> colProcFormat;
    @FXML private TableColumn<TestRequest, String> colProcStatus;

    // Result Entry Panel Details
    @FXML private VBox resultEntryCard;
    @FXML private Label selectedReqIdLabel;
    @FXML private Label selectedFormatLabel;
    
    @FXML private VBox numericPane;
    @FXML private TextField numericField;

    @FXML private VBox textPane;
    @FXML private TextArea textResultArea;

    @FXML private VBox filePane;
    @FXML private Label fileLabel;
    @FXML private Button chooseFileButton;
    @FXML private Label fileNameLabel;

    @FXML private Label resultErrorLabel;
    @FXML private Button saveResultButton;
    @FXML private Button validateResultButton;

    // Tab 4: Customer Provisioning
    @FXML private TextField customerNameField;
    @FXML private TextField customerEmailField;
    @FXML private PasswordField customerPasswordField;
    @FXML private Label customerProvisionErrorLabel;

    private User attendantUser;
    private ObservableList<TestRequest> payRequests = FXCollections.observableArrayList();
    private ObservableList<TestRequest> sampleRequests = FXCollections.observableArrayList();
    private ObservableList<TestRequest> procRequests = FXCollections.observableArrayList();

    private FileChooser fileChooser = new FileChooser();
    private byte[] selectedFileBytes;
    private String selectedFileName;

    @FXML
    public void initialize() {
        attendantUser = App.getCurrentUser();
        if (attendantUser != null) {
            attendantNameLabel.setText(attendantUser.getName());
            attendantEmailLabel.setText(attendantUser.getEmail());
        }

        // Initialize table columns
        setupTableColumns();

        // Load data
        loadPaymentData();
        loadSampleData();
        loadProcessingData();

        // Listen for table row clicks in Result Entry
        processingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showResultEntryForm(newVal);
            } else {
                hideResultEntryForm();
            }
        });

        hideResultEntryForm();
        resultErrorLabel.setVisible(false);
        customerProvisionErrorLabel.setVisible(false);
    }

    private void setupTableColumns() {
        // Tab 1 columns
        colPayId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPayPatient.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colPayTest.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colPayPrice.setCellValueFactory(new PropertyValueFactory<>("testTypePrice"));
        colPayStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colPayEvidence.setCellValueFactory(new PropertyValueFactory<>("paymentEvidence"));
        colPayDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Tab 2 columns
        colSampleId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSamplePatient.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colSampleTest.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colSamplePay.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colSampleStatus.setCellValueFactory(new PropertyValueFactory<>("sampleStatus"));
        colSampleCollectedAt.setCellValueFactory(new PropertyValueFactory<>("collectedAt"));
        colSampleProcessedAt.setCellValueFactory(new PropertyValueFactory<>("processedAt"));
        colSampleValidatedAt.setCellValueFactory(new PropertyValueFactory<>("validatedAt"));

        // Tab 3 columns
        colProcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProcPatient.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colProcTest.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colProcFormat.setCellValueFactory(new PropertyValueFactory<>("testTypeResultFormat"));
        colProcStatus.setCellValueFactory(new PropertyValueFactory<>("sampleStatus"));
    }

    // --- Tab 1: Payment Verification ---

    private void loadPaymentData() {
        payRequests.clear();
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format "
                   + "FROM test_requests tr "
                   + "JOIN users u ON tr.customer_id = u.id "
                   + "JOIN test_types tt ON tr.test_type_id = tt.id "
                   + "WHERE tr.payment_status = 'Unpaid' "
                   + "ORDER BY tr.id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                payRequests.add(mapResultSetToRequest(rs));
            }
            paymentTable.setItems(payRequests);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleMarkAsPaid(ActionEvent event) {
        TestRequest selected = paymentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a test request to confirm payment.");
            return;
        }

        String sql = "UPDATE test_requests SET payment_status = 'Paid' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();

            AuditService.log(attendantUser.getId(), attendantUser.getEmail(), "CONFIRM_PAYMENT",
                             "Confirmed payment for Request ID: " + selected.getId() + " (Patient: " + selected.getCustomerEmail() + ")");

            loadPaymentData();
            loadSampleData();
            loadProcessingData();
            showAlert("Payment Confirmed", "Request marked as Paid successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update payment. Try again.");
        }
    }

    // --- Tab 2: Sample Lifecycle ---

    private void loadSampleData() {
        sampleRequests.clear();
        // Show ALL paid samples across every stage so the full lifecycle is visible
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format "
                   + "FROM test_requests tr "
                   + "JOIN users u ON tr.customer_id = u.id "
                   + "JOIN test_types tt ON tr.test_type_id = tt.id "
                   + "WHERE tr.payment_status = 'Paid' "
                   + "ORDER BY tr.id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sampleRequests.add(mapResultSetToRequest(rs));
            }
            sampleTable.setItems(sampleRequests);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleMarkAsCollected(ActionEvent event) {
        TestRequest selected = sampleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a request from the tracking board.");
            return;
        }

        if (!"Pending Collection".equals(selected.getSampleStatus())) {
            showAlert("Invalid Stage", "Sample has already been collected for this request.");
            return;
        }

        String sql = "UPDATE test_requests SET sample_status = 'Collected', collected_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();

            AuditService.log(attendantUser.getId(), attendantUser.getEmail(), "SAMPLE_COLLECTED",
                             "Marked sample as Collected for Request ID: " + selected.getId());

            loadSampleData();
            loadProcessingData();
            showAlert("Success", "Sample status updated to Collected!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleMarkAsProcessing(ActionEvent event) {
        TestRequest selected = sampleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a request from the tracking board.");
            return;
        }

        if (!"Collected".equals(selected.getSampleStatus())) {
            showAlert("Invalid Stage", "Sample must be in 'Collected' status before processing.");
            return;
        }

        String sql = "UPDATE test_requests SET sample_status = 'Processing', processed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();

            AuditService.log(attendantUser.getId(), attendantUser.getEmail(), "SAMPLE_PROCESSING",
                             "Sent sample to Laboratory Processing for Request ID: " + selected.getId());

            loadSampleData();
            loadProcessingData();
            showAlert("Success", "Sample is now in Processing status!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleQuickValidate(ActionEvent event) {
        TestRequest selected = sampleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a request from the tracking board.");
            return;
        }

        if ("Validated".equals(selected.getSampleStatus())) {
            showAlert("Already Validated", "This sample has already been validated and the result released.");
            return;
        }

        if (!"Processing".equals(selected.getSampleStatus())) {
            showAlert("Invalid Stage", "Sample must be in 'Processing' status before it can be validated.\n"
                    + "Current status: " + selected.getSampleStatus());
            return;
        }

        // Mark sample as Validated and create/update result record as validated
        String updateReqSql = "UPDATE test_requests SET sample_status = 'Validated', validated_at = CURRENT_TIMESTAMP WHERE id = ?";
        String upsertResultSql = "INSERT INTO results (request_id, is_validated, validated_by, validated_at) "
                               + "VALUES (?, true, ?, CURRENT_TIMESTAMP) "
                               + "ON CONFLICT (request_id) DO UPDATE SET "
                               + "is_validated = true, validated_by = EXCLUDED.validated_by, validated_at = EXCLUDED.validated_at";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement(updateReqSql);
                 PreparedStatement p2 = conn.prepareStatement(upsertResultSql)) {

                p1.setInt(1, selected.getId());
                p1.executeUpdate();

                p2.setInt(1, selected.getId());
                p2.setInt(2, attendantUser.getId());
                p2.executeUpdate();

                conn.commit();
            } catch (SQLException inner) {
                conn.rollback();
                throw inner;
            }

            AuditService.log(attendantUser.getId(), attendantUser.getEmail(), "VALIDATE_RESULT",
                             "Validated sample and released result for Request ID: " + selected.getId()
                             + " (" + selected.getTestTypeName() + ")");

            sendResultReadyNotification(selected);

            loadSampleData();
            loadProcessingData();
            showAlert("Sample Validated", "Sample validated and result released to patient dashboard.");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to validate sample. Please try again.");
        }
    }

    // --- Tab 3: Result Entry & Verification ---

    private void loadProcessingData() {
        procRequests.clear();
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format "
                   + "FROM test_requests tr "
                   + "JOIN users u ON tr.customer_id = u.id "
                   + "JOIN test_types tt ON tr.test_type_id = tt.id "
                   + "WHERE tr.payment_status = 'Paid' AND tr.sample_status IN ('Processing', 'Collected') "
                   + "ORDER BY tr.id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                procRequests.add(mapResultSetToRequest(rs));
            }
            processingTable.setItems(procRequests);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showResultEntryForm(TestRequest req) {
        selectedReqIdLabel.setText(String.valueOf(req.getId()));
        selectedFormatLabel.setText(req.getTestTypeResultFormat());

        // Clear existing fields
        numericField.clear();
        textResultArea.clear();
        selectedFileBytes = null;
        selectedFileName = null;
        fileNameLabel.setText("No file selected");
        resultErrorLabel.setVisible(false);

        // Hide all entry panels initially
        numericPane.setVisible(false);
        numericPane.setManaged(false);
        textPane.setVisible(false);
        textPane.setManaged(false);
        filePane.setVisible(false);
        filePane.setManaged(false);

        // Show matching input block
        String format = req.getTestTypeResultFormat();
        if ("numeric".equals(format)) {
            numericPane.setVisible(true);
            numericPane.setManaged(true);
        } else if ("text".equals(format)) {
            textPane.setVisible(true);
            textPane.setManaged(true);
        } else if ("PDF".equals(format) || "image".equals(format)) {
            filePane.setVisible(true);
            filePane.setManaged(true);
            fileLabel.setText("Upload Attachment (" + format + ")");
        }

        // Load existing draft result if present
        loadExistingDraftResult(req.getId(), format);

        resultEntryCard.setVisible(true);
    }

    private void loadExistingDraftResult(int requestId, String format) {
        String sql = "SELECT * FROM results WHERE request_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("result_value");
                    if ("numeric".equals(format)) {
                        numericField.setText(value);
                    } else if ("text".equals(format)) {
                        textResultArea.setText(value);
                    } else if ("PDF".equals(format) || "image".equals(format)) {
                        selectedFileName = rs.getString("file_name");
                        selectedFileBytes = rs.getBytes("file_data");
                        if (selectedFileName != null) {
                            fileNameLabel.setText(selectedFileName + " (Already Drafted)");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void hideResultEntryForm() {
        resultEntryCard.setVisible(false);
        selectedReqIdLabel.setText("None");
        selectedFormatLabel.setText("None");
    }

    @FXML
    void handleChooseFile(ActionEvent event) {
        TestRequest selected = processingTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        fileChooser.setTitle("Select Attachment Report");
        fileChooser.getExtensionFilters().clear();

        if ("PDF".equals(selected.getTestTypeResultFormat())) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        } else if ("image".equals(selected.getTestTypeResultFormat())) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"));
        }

        File file = fileChooser.showOpenDialog(App.getPrimaryStage());
        if (file != null) {
            try {
                selectedFileName = file.getName();
                fileNameLabel.setText(selectedFileName);
                selectedFileBytes = Files.readAllBytes(file.toPath());
                resultErrorLabel.setVisible(false);
            } catch (IOException e) {
                e.printStackTrace();
                showResultError("Failed to read selected file.");
            }
        }
    }

    @FXML
    void handleSaveResult(ActionEvent event) {
        saveResult(false); // Save draft
    }

    @FXML
    void handleValidateResult(ActionEvent event) {
        saveResult(true);  // Save and Validate
    }

    private void saveResult(boolean validate) {
        TestRequest selected = processingTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String format = selected.getTestTypeResultFormat();
        String resultVal = null;

        if (null != format) // Perform validations depending on layout
        switch (format) {
            case "numeric":
                resultVal = numericField.getText().trim();
                if (resultVal.isEmpty()) {
                    showResultError("Numeric result field is empty.");
                    return;
                }   try {
                    Double.valueOf(resultVal);
                } catch (NumberFormatException e) {
                    showResultError("Result must be a valid number.");
                    return;
                }   break;
            case "text":
                resultVal = textResultArea.getText().trim();
                if (resultVal.isEmpty()) {
                    showResultError("Result comment text area is empty.");
                    return;
                }   break;
            case "PDF":
            case "image":
                if (selectedFileBytes == null) {
                    showResultError("Please attach a " + format + " file.");
                    return;
                }   break;
            default:
                break;
        }

        // Upsert results table
        String upsertSql = "INSERT INTO results (request_id, result_value, file_name, file_data, is_validated, validated_by, validated_at) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                         + "ON CONFLICT (request_id) "
                         + "DO UPDATE SET result_value = EXCLUDED.result_value, "
                         + "              file_name = EXCLUDED.file_name, "
                         + "              file_data = EXCLUDED.file_data, "
                         + "              is_validated = EXCLUDED.is_validated, "
                         + "              validated_by = EXCLUDED.validated_by, "
                         + "              validated_at = EXCLUDED.validated_at";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {

            pstmt.setInt(1, selected.getId());
            
            if (resultVal != null) {
                pstmt.setString(2, resultVal);
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }

            if (selectedFileName != null) {
                pstmt.setString(3, selectedFileName);
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }

            if (selectedFileBytes != null) {
                pstmt.setBytes(4, selectedFileBytes);
            } else {
                pstmt.setNull(4, Types.BINARY);
            }

            pstmt.setBoolean(5, validate);

            if (validate) {
                pstmt.setInt(6, attendantUser.getId());
                pstmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            } else {
                pstmt.setNull(6, Types.INTEGER);
                pstmt.setNull(7, Types.TIMESTAMP);
            }

            pstmt.executeUpdate();

            if (validate) {
                // Also update the test request status to Validated
                String reqUpdateSql = "UPDATE test_requests SET sample_status = 'Validated', validated_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement reqPstmt = conn.prepareStatement(reqUpdateSql)) {
                    reqPstmt.setInt(1, selected.getId());
                    reqPstmt.executeUpdate();
                }

                AuditService.log(attendantUser.getId(), attendantUser.getEmail(), "VALIDATE_RESULT",
                                 "Validated and published results for Request ID: " + selected.getId() + " (" + selected.getTestTypeName() + ")");

                // Trigger email notification to customer
                sendResultReadyNotification(selected);

                showAlert("Result Validated", "Diagnostic results validated and released to patient dashboard successfully!");
                hideResultEntryForm();
            } else {
                AuditService.log(attendantUser.getId(), attendantUser.getEmail(), "SAVE_DRAFT_RESULT",
                                 "Saved draft result for Request ID: " + selected.getId());
                showAlert("Draft Saved", "Draft results saved successfully.");
            }

            // Reload tables
            loadSampleData();
            loadProcessingData();

        } catch (SQLException e) {
            e.printStackTrace();
            showResultError("Database error occurred while saving results.");
        }
    }

    private void sendResultReadyNotification(TestRequest req) {
        String patientEmail = req.getCustomerEmail();
        String patientName = req.getCustomerName();
        String testName = req.getTestTypeName();

        String subject = "Your Test Result is Ready - Sante Diagnostics";
        String body = "Dear " + patientName + ",\n\n"
                    + "Your diagnostic test result for \"" + testName + "\" has been processed and validated by our laboratory staff.\n"
                    + "It is now available for viewing on your private Patient Hub dashboard.\n\n"
                    + "To view or download your report, please sign in to the application.\n\n"
                    + "Best regards,\n"
                    + "Sante Diagnostics Ltd";

        EmailService.sendEmail(patientEmail, subject, body);
    }

    private void showResultError(String msg) {
        resultErrorLabel.setText(msg);
        resultErrorLabel.setVisible(true);
    }

    // --- Tab 4: Customer Provisioning ---

    @FXML
    void handleCreateCustomer(ActionEvent event) {
        String name = customerNameField.getText().trim();
        String email = customerEmailField.getText().trim();
        String password = customerPasswordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showCustomerProvisionError("Name, email, and temporary password are required.");
            return;
        }

        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            showCustomerProvisionError("Please enter a valid email address.");
            return;
        }

        if (password.length() < 6) {
            showCustomerProvisionError("Temporary password must be at least 6 characters.");
            return;
        }

        String sql = "INSERT INTO users (name, email, password_hash, role, must_change_password, is_verified) "
                   + "VALUES (?, ?, ?, 'CUSTOMER', true, true)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, SecurityUtils.hashPassword(password));
            pstmt.executeUpdate();

            AuditService.log(attendantUser.getId(), attendantUser.getEmail(), "PROVISION_CUSTOMER",
                    "Created customer account: " + email + " with first-login password change required.");

            customerNameField.clear();
            customerEmailField.clear();
            customerPasswordField.clear();
            customerProvisionErrorLabel.setVisible(false);

            showAlert("Customer Created", "Customer account created. The patient must change the temporary password on first login.");

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                showCustomerProvisionError("Email address is already in use.");
            } else {
                e.printStackTrace();
                showCustomerProvisionError("Failed to create customer account. Please try again.");
            }
        }
    }

    private void showCustomerProvisionError(String msg) {
        customerProvisionErrorLabel.setText(msg);
        customerProvisionErrorLabel.setVisible(true);
    }

    // --- General Actions ---

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    @FXML
    void handleLogout(ActionEvent event) {
        AuditService.log(attendantUser.getId(), attendantUser.getEmail(), "LOGOUT", "User logged out.");
        App.setCurrentUser(null);
        App.switchScene("login.fxml", "Sante Diagnostics - Login", 800, 600);
    }

    private TestRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
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
        return tr;
    }
}