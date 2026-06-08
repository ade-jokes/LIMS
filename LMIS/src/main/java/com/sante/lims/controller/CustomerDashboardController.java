package com.sante.lims.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import com.sante.lims.App;
import com.sante.lims.model.TestRequest;
import com.sante.lims.model.TestResult;
import com.sante.lims.model.TestType;
import com.sante.lims.model.User;
import com.sante.lims.utils.AuditService;
import com.sante.lims.utils.DBConnection;

public class CustomerDashboardController {

    // Top Header
    @FXML private Label patientNameLabel;
    @FXML private Label patientEmailLabel;

    // Tab 1: Book Test
    @FXML private TableView<TestType> testMenuTable;
    @FXML private TableColumn<TestType, Integer> colMenuId;
    @FXML private TableColumn<TestType, String> colMenuName;
    @FXML private TableColumn<TestType, Double> colMenuPrice;
    @FXML private TableColumn<TestType, Integer> colMenuTat;

    // Checkout Details
    @FXML private Label checkoutTestNameLabel;
    @FXML private Label checkoutPriceLabel;
    @FXML private TextField paymentRefField;
    @FXML private Label checkoutErrorLabel;
    @FXML private Button placeOrderButton;

    // Tab 2: Patient Dashboard & Countdown
    @FXML private TableView<TestRequest> activeRequestsTable;
    @FXML private TableColumn<TestRequest, Integer> colActId;
    @FXML private TableColumn<TestRequest, String> colActName;
    @FXML private TableColumn<TestRequest, String> colActPayment;
    @FXML private TableColumn<TestRequest, String> colActSample;
    @FXML private TableColumn<TestRequest, Timestamp> colActOrdered;
    @FXML private TableColumn<TestRequest, String> colActTimer;

    // Tab 3: Result Vault
    @FXML private TableView<TestRequest> resultsTable;
    @FXML private TableColumn<TestRequest, Integer> colResId;
    @FXML private TableColumn<TestRequest, String> colResName;
    @FXML private TableColumn<TestRequest, String> colResFormat;
    @FXML private TableColumn<TestRequest, Timestamp> colResValidated;

    // Result Viewer Card
    @FXML private VBox resultViewerCard;
    @FXML private Label viewTestNameLabel;
    @FXML private Label viewDateLabel;
    
    @FXML private VBox viewContentBox;
    @FXML private Label viewResultValLabel;

    @FXML private VBox viewImageContainer;
    @FXML private ImageView viewImageView;

    @FXML private VBox viewPdfBox;
    @FXML private Button downloadAttachmentButton;

    private User patientUser;
    private ObservableList<TestType> testTypesMenu = FXCollections.observableArrayList();
    private ObservableList<TestRequest> activeRequests = FXCollections.observableArrayList();
    private ObservableList<TestRequest> completedRequests = FXCollections.observableArrayList();

    private Timeline countdownTimeline;
    private TestType selectedTestType;
    private TestRequest selectedRequestForResult;
    private TestResult currentLoadedResult;

    @FXML
    public void initialize() {
        patientUser = App.getCurrentUser();
        if (patientUser != null) {
            patientNameLabel.setText(patientUser.getName());
            patientEmailLabel.setText(patientUser.getEmail());
        }

        // 1. Setup Table Columns
        setupTableColumns();

        // 2. Load Table Data
        loadTestMenu();
        loadActiveRequests();
        loadCompletedRequests();

        // 3. Register Selection Listeners
        testMenuTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showCheckoutDetails(newVal);
            }
        });

        activeRequestsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // No action needed on click, just displays status and countdown
        });

        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadAndDisplayResult(newVal);
            } else {
                hideResultViewer();
            }
        });

        // 4. Start Live Countdown Timer Timeline (ticks every 1 second)
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            activeRequestsTable.refresh();
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();

        // Initial UI resets
        resetCheckoutForm();
        hideResultViewer();
    }

    private void setupTableColumns() {
        // Test Menu Table
        colMenuId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMenuName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMenuPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colMenuTat.setCellValueFactory(new PropertyValueFactory<>("tatHours"));

        // Active Requests Table
        colActId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colActName.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colActPayment.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colActSample.setCellValueFactory(new PropertyValueFactory<>("sampleStatus"));
        colActOrdered.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        // Dynamic Live Countdown column value factory
        colActTimer.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCountdownText()));

        // Completed Results Table
        colResId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colResName.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colResFormat.setCellValueFactory(new PropertyValueFactory<>("testTypeResultFormat"));
        colResValidated.setCellValueFactory(new PropertyValueFactory<>("validatedAt"));
    }

    // --- Tab 1: Book Test Actions ---

    private void loadTestMenu() {
        testTypesMenu.clear();
        String sql = "SELECT * FROM test_types ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                testTypesMenu.add(new TestType(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("tat_hours"),
                        rs.getString("result_format"),
                        rs.getTimestamp("created_at")
                ));
            }
            testMenuTable.setItems(testTypesMenu);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showCheckoutDetails(TestType testType) {
        selectedTestType = testType;
        checkoutTestNameLabel.setText(testType.getName());
        checkoutPriceLabel.setText(String.format("$%.2f", testType.getPrice()));
        checkoutErrorLabel.setVisible(false);
    }

    private void resetCheckoutForm() {
        selectedTestType = null;
        checkoutTestNameLabel.setText("No test selected");
        checkoutPriceLabel.setText("-$0.00");
        paymentRefField.clear();
        checkoutErrorLabel.setVisible(false);
    }

    @FXML
    void handlePlaceOrder(ActionEvent event) {
        if (selectedTestType == null) {
            showCheckoutError("Please select a test from the menu first.");
            return;
        }

        String evidence = paymentRefField.getText().trim();
        if (evidence.isEmpty()) {
            showCheckoutError("Please enter your bank transfer payment reference.");
            return;
        }

        // Insert request
        String sql = "INSERT INTO test_requests (customer_id, test_type_id, payment_status, payment_evidence, sample_status, due_at) "
                   + "VALUES (?, ?, 'Unpaid', ?, 'Pending Collection', ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, patientUser.getId());
            pstmt.setInt(2, selectedTestType.getId());
            pstmt.setString(3, evidence);

            // Calculate standard Turnaround due time
            long tatMillis = selectedTestType.getTatHours() * 3600L * 1000L;
            Timestamp dueTime = new Timestamp(System.currentTimeMillis() + tatMillis);
            pstmt.setTimestamp(4, dueTime);

            pstmt.executeUpdate();

            AuditService.log(patientUser.getId(), patientUser.getEmail(), "BOOK_TEST",
                             "Booked diagnostics test: " + selectedTestType.getName() + " (Evidence: " + evidence + ")");

            showAlert("Booking Placed", "Your test booking has been placed successfully!\n"
                    + "Once laboratory staff confirms your bank transfer reference, your sample collection will be initiated.");

            resetCheckoutForm();
            loadActiveRequests();

        } catch (SQLException e) {
            e.printStackTrace();
            showCheckoutError("Failed to book test. Try again later.");
        }
    }

    private void showCheckoutError(String msg) {
        checkoutErrorLabel.setText(msg);
        checkoutErrorLabel.setVisible(true);
    }

    // --- Tab 2: Active Requests ---

    private void loadActiveRequests() {
        activeRequests.clear();
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format "
                   + "FROM test_requests tr "
                   + "JOIN users u ON tr.customer_id = u.id "
                   + "JOIN test_types tt ON tr.test_type_id = tt.id "
                   + "WHERE tr.customer_id = ? AND tr.sample_status != 'Validated' "
                   + "ORDER BY tr.id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, patientUser.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activeRequests.add(mapResultSetToRequest(rs));
                }
            }
            activeRequestsTable.setItems(activeRequests);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Tab 3: Result Vault ---

    private void loadCompletedRequests() {
        completedRequests.clear();
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format "
                   + "FROM test_requests tr "
                   + "JOIN users u ON tr.customer_id = u.id "
                   + "JOIN test_types tt ON tr.test_type_id = tt.id "
                   + "WHERE tr.customer_id = ? AND tr.sample_status = 'Validated' "
                   + "ORDER BY tr.validated_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, patientUser.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    completedRequests.add(mapResultSetToRequest(rs));
                }
            }
            resultsTable.setItems(completedRequests);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAndDisplayResult(TestRequest req) {
        selectedRequestForResult = req;
        viewTestNameLabel.setText(req.getTestTypeName());
        viewDateLabel.setText(req.getValidatedAt() != null ? req.getValidatedAt().toString() : "N/A");

        // Clear sub-panes
        viewContentBox.setVisible(false);
        viewContentBox.setManaged(false);
        viewImageContainer.setVisible(false);
        viewImageContainer.setManaged(false);
        viewPdfBox.setVisible(false);
        viewPdfBox.setManaged(false);
        downloadAttachmentButton.setVisible(false);

        // Fetch result data
        String sql = "SELECT r.*, u.name as validator_name "
                   + "FROM results r "
                   + "LEFT JOIN users u ON r.validated_by = u.id "
                   + "WHERE r.request_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, req.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentLoadedResult = new TestResult();
                    currentLoadedResult.setId(rs.getInt("id"));
                    currentLoadedResult.setRequestId(rs.getInt("request_id"));
                    currentLoadedResult.setResultValue(rs.getString("result_value"));
                    currentLoadedResult.setFileName(rs.getString("file_name"));
                    currentLoadedResult.setFileData(rs.getBytes("file_data"));
                    currentLoadedResult.setValidated(rs.getBoolean("is_validated"));
                    currentLoadedResult.setValidatedByName(rs.getString("validator_name"));
                    currentLoadedResult.setValidatedAt(rs.getTimestamp("validated_at"));

                    String format = req.getTestTypeResultFormat();
                    
                    if ("numeric".equals(format) || "text".equals(format)) {
                        viewContentBox.setVisible(true);
                        viewContentBox.setManaged(true);
                        viewResultValLabel.setText(currentLoadedResult.getResultValue());
                    } else if ("image".equals(format)) {
                        viewImageContainer.setVisible(true);
                        viewImageContainer.setManaged(true);
                        downloadAttachmentButton.setVisible(true);

                        if (currentLoadedResult.getFileData() != null) {
                            try {
                                Image img = new Image(new ByteArrayInputStream(currentLoadedResult.getFileData()));
                                viewImageView.setImage(img);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.err.println("Failed to display image.");
                            }
                        }
                    } else if ("PDF".equals(format)) {
                        viewPdfBox.setVisible(true);
                        viewPdfBox.setManaged(true);
                        downloadAttachmentButton.setVisible(true);
                    }
                }
            }

            resultViewerCard.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to retrieve results from database.");
        }
    }

    private void hideResultViewer() {
        resultViewerCard.setVisible(false);
        selectedRequestForResult = null;
        currentLoadedResult = null;
    }

    @FXML
    void handleDownloadAttachment(ActionEvent event) {
        if (currentLoadedResult == null || currentLoadedResult.getFileData() == null) {
            showAlert("No Attachment", "No attachment data is loaded for this result.");
            return;
        }

        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Save Diagnostic Attachment");
        saveChooser.setInitialFileName(currentLoadedResult.getFileName());

        String format = selectedRequestForResult.getTestTypeResultFormat();
        if ("PDF".equals(format)) {
            saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        } else if ("image".equals(format)) {
            saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"));
        }

        File file = saveChooser.showSaveDialog(App.getPrimaryStage());
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(currentLoadedResult.getFileData());
                fos.flush();
                
                AuditService.log(patientUser.getId(), patientUser.getEmail(), "DOWNLOAD_REPORT", 
                                 "Downloaded result file: " + currentLoadedResult.getFileName() + " for Request ID: " + selectedRequestForResult.getId());
                
                showAlert("File Saved", "Report file downloaded successfully to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to save file locally.");
            }
        }
    }

    // --- General Actions ---

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void handleLogout(ActionEvent event) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        AuditService.log(patientUser.getId(), patientUser.getEmail(), "LOGOUT", "User logged out.");
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
