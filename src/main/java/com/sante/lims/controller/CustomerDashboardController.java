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
import java.util.Optional;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.print.PrinterJob;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import com.sante.lims.App;
import com.sante.lims.model.TestRequest;
import com.sante.lims.model.TestResult;
import com.sante.lims.model.TestType;
import com.sante.lims.model.User;
import com.sante.lims.utils.AuditService;
import com.sante.lims.utils.DBConnection;
import com.sante.lims.utils.SecurityUtils;

public class CustomerDashboardController {

    @FXML private Label patientNameLabel;
    @FXML private Label patientEmailLabel;

    // Tab 1
    @FXML private TableView<TestType> testMenuTable;
    @FXML private TableColumn<TestType, Integer> colMenuId;
    @FXML private TableColumn<TestType, String> colMenuName;
    @FXML private TableColumn<TestType, Double> colMenuPrice;
    @FXML private TableColumn<TestType, Integer> colMenuTat;
    @FXML private Label checkoutTestNameLabel;
    @FXML private Label checkoutPriceLabel;
    @FXML private TextField paymentRefField;
    @FXML private Label checkoutErrorLabel;
    @FXML private Button placeOrderButton;
    @FXML private Label bankNameLabel;
    @FXML private Label bankAccountNameLabel;
    @FXML private Label bankAccountNumberLabel;
    @FXML private Label bankInstructionsLabel;

    // Tab 2
    @FXML private TableView<TestRequest> activeRequestsTable;
    @FXML private TableColumn<TestRequest, Integer> colActId;
    @FXML private TableColumn<TestRequest, String> colActName;
    @FXML private TableColumn<TestRequest, String> colActPayment;
    @FXML private TableColumn<TestRequest, String> colActSample;
    @FXML private TableColumn<TestRequest, Timestamp> colActOrdered;
    @FXML private TableColumn<TestRequest, String> colActTimer;

    // Tab 3
    @FXML private TableView<TestRequest> resultsTable;
    @FXML private TableColumn<TestRequest, Integer> colResId;
    @FXML private TableColumn<TestRequest, String> colResName;
    @FXML private TableColumn<TestRequest, String> colResFormat;
    @FXML private TableColumn<TestRequest, Timestamp> colResValidated;
    @FXML private VBox resultViewerCard;
    @FXML private Label viewTestNameLabel;
    @FXML private Label viewDateLabel;
    @FXML private Label viewPatientNameLabel;
    @FXML private Label viewPatientEmailLabel;
    @FXML private Label viewFormatLabel;
    @FXML private Label viewOrderedLabel;
    @FXML private Label viewValidatorLabel;
    @FXML private VBox viewContentBox;
    @FXML private Label viewResultValLabel;
    @FXML private VBox viewImageContainer;
    @FXML private ImageView viewImageView;
    @FXML private VBox viewPdfBox;
    @FXML private Button downloadAttachmentButton;

    // Tab 4: My Profile
    @FXML private TextField profileNameField;
    @FXML private TextField profileEmailField;
    @FXML private Label profileErrorLabel;
    @FXML private PasswordField currentPassField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private Label passErrorLabel;

    private User patientUser;
    private ObservableList<TestType> testTypesMenu = FXCollections.observableArrayList();
    private ObservableList<TestRequest> activeRequests = FXCollections.observableArrayList();
    private ObservableList<TestRequest> completedRequests = FXCollections.observableArrayList();
    private Timeline countdownTimeline;
    private TestType selectedTestType;
    private TestRequest selectedRequestForResult;
    private TestResult currentLoadedResult;

    private String cachedBankName        = "Bank: First Bank Nigeria";
    private String cachedBankAccountName = "Account Name: Sante Diagnostics Ltd";
    private String cachedBankAccountNumber = "Account Number: N/A";
    private String cachedBankInstructions  = "Use your Test Request ID as the payment reference.";

    @FXML
    public void initialize() {
        patientUser = App.getCurrentUser();
        if (patientUser != null) {
            patientNameLabel.setText(patientUser.getName());
            patientEmailLabel.setText(patientUser.getEmail());
        }
        setupTableColumns();
        loadTestMenu();
        loadActiveRequests();
        loadCompletedRequests();
        loadBankDetails();

        testMenuTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) showCheckoutDetails(newVal);
        });
        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) loadAndDisplayResult(newVal);
            else hideResultViewer();
        });

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> activeRequestsTable.refresh()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();

        resetCheckoutForm();
        hideResultViewer();

        // Pre-fill profile tab
        if (profileNameField != null && patientUser != null) {
            profileNameField.setText(patientUser.getName());
            profileEmailField.setText(patientUser.getEmail());
        }
        if (profileErrorLabel != null) profileErrorLabel.setVisible(false);
        if (passErrorLabel != null) passErrorLabel.setVisible(false);
    }

    private void setupTableColumns() {
        colMenuId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMenuName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMenuPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colMenuTat.setCellValueFactory(new PropertyValueFactory<>("tatHours"));
        colActId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colActName.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colActPayment.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colActSample.setCellValueFactory(new PropertyValueFactory<>("sampleStatus"));
        colActOrdered.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colActTimer.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCountdownText()));
        colResId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colResName.setCellValueFactory(new PropertyValueFactory<>("testTypeName"));
        colResFormat.setCellValueFactory(new PropertyValueFactory<>("testTypeResultFormat"));
        colResValidated.setCellValueFactory(new PropertyValueFactory<>("validatedAt"));
    }

    private void loadBankDetails() {
        String sql = "SELECT bank_name, account_name, account_number, sort_code, instructions FROM bank_details ORDER BY id ASC LIMIT 1";
        try (Connection conn = DBConnection.getConnection(); java.sql.Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                cachedBankName = "Bank: " + rs.getString("bank_name");
                cachedBankAccountName = "Account Name: " + rs.getString("account_name");
                String num = rs.getString("account_number");
                String sort = rs.getString("sort_code");
                cachedBankAccountNumber = "Account No: " + num + ((sort != null && !sort.isEmpty() && !"N/A".equals(sort)) ? "  |  Sort Code: " + sort : "");
                String instr = rs.getString("instructions");
                if (instr != null && !instr.isEmpty()) cachedBankInstructions = instr;
            }
        } catch (SQLException e) { System.err.println("[CustomerDashboard] Could not load bank_details."); }
        if (bankNameLabel != null) bankNameLabel.setText(cachedBankName);
        if (bankAccountNameLabel != null) bankAccountNameLabel.setText(cachedBankAccountName);
        if (bankAccountNumberLabel != null) bankAccountNumberLabel.setText(cachedBankAccountNumber);
        if (bankInstructionsLabel != null) bankInstructionsLabel.setText("* " + cachedBankInstructions);
    }

    private void loadTestMenu() {
        testTypesMenu.clear();
        String sql = "SELECT * FROM test_types ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                testTypesMenu.add(new TestType(rs.getInt("id"), rs.getString("name"), rs.getDouble("price"), rs.getInt("tat_hours"), rs.getString("result_format"), rs.getTimestamp("created_at")));
            }
            testMenuTable.setItems(testTypesMenu);
        } catch (SQLException e) { e.printStackTrace(); }
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
        if (selectedTestType == null) { showCheckoutError("Please select a test from the menu first."); return; }
        String evidence = paymentRefField.getText().trim();
        if (evidence.isEmpty()) { showCheckoutError("Please enter your bank transfer payment reference."); return; }
        String sql = "INSERT INTO test_requests (customer_id, test_type_id, payment_status, payment_evidence, sample_status, due_at) VALUES (?, ?, 'Unpaid', ?, 'Pending Collection', ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientUser.getId()); pstmt.setInt(2, selectedTestType.getId()); pstmt.setString(3, evidence);
            long tatMillis = selectedTestType.getTatHours() * 3600L * 1000L;
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis() + tatMillis));
            pstmt.executeUpdate();
            AuditService.log(patientUser.getId(), patientUser.getEmail(), "BOOK_TEST", "Booked: " + selectedTestType.getName() + " (Evidence: " + evidence + ")");
            Alert bankAlert = new Alert(AlertType.INFORMATION);
            bankAlert.setTitle("Booking Confirmed — Payment Required");
            bankAlert.setHeaderText("Your test booking has been placed successfully!");
            bankAlert.setContentText("Please complete your bank transfer to:\n\n─────────────────────────────\n" + cachedBankName + "\n" + cachedBankAccountName + "\n" + cachedBankAccountNumber + "\n─────────────────────────────\n\nPayment Reference: " + evidence + "\n\n" + cachedBankInstructions + "\n\nYour sample collection begins once our staff confirms your payment.");
            bankAlert.getDialogPane().setMinWidth(480); bankAlert.showAndWait();
            resetCheckoutForm(); loadActiveRequests();
        } catch (SQLException e) { e.printStackTrace(); showCheckoutError("Failed to book test. Try again later."); }
    }

    private void showCheckoutError(String msg) { checkoutErrorLabel.setText(msg); checkoutErrorLabel.setVisible(true); }

    // ========== CANCEL BOOKING (DELETE for unpaid) ==========
    @FXML
    void handleCancelBooking(ActionEvent event) {
        TestRequest selected = activeRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select a booking to cancel."); return; }
        if ("Paid".equals(selected.getPaymentStatus())) {
            showAlert("Cannot Cancel", "Paid bookings cannot be cancelled by the customer. Please contact the lab."); return;
        }
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Booking");
        confirm.setHeaderText("Cancel booking for: " + selected.getTestTypeName() + "?");
        confirm.setContentText("Only unpaid bookings can be cancelled. This action cannot be undone.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String sql = "DELETE FROM test_requests WHERE id = ? AND customer_id = ? AND payment_status = 'Unpaid'";
            try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, selected.getId()); pstmt.setInt(2, patientUser.getId());
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    AuditService.log(patientUser.getId(), patientUser.getEmail(), "CANCEL_BOOKING", "Cancelled unpaid booking ID " + selected.getId() + " for: " + selected.getTestTypeName());
                    loadActiveRequests();
                    showInfo("Cancelled", "Booking cancelled successfully.");
                } else {
                    showAlert("Failed", "Could not cancel. The booking may have already been paid or processed.");
                }
            } catch (SQLException e) { e.printStackTrace(); showAlert("Error", "Failed to cancel booking."); }
        }
    }

    private void loadActiveRequests() {
        activeRequests.clear();
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format FROM test_requests tr JOIN users u ON tr.customer_id = u.id JOIN test_types tt ON tr.test_type_id = tt.id WHERE tr.customer_id = ? AND tr.sample_status != 'Validated' ORDER BY tr.id DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientUser.getId());
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) activeRequests.add(mapResultSetToRequest(rs)); }
            activeRequestsTable.setItems(activeRequests);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadCompletedRequests() {
        completedRequests.clear();
        String sql = "SELECT tr.*, u.name as customer_name, u.email as customer_email, tt.name as test_name, tt.price as test_price, tt.tat_hours as test_tat, tt.result_format as test_format FROM test_requests tr JOIN users u ON tr.customer_id = u.id JOIN test_types tt ON tr.test_type_id = tt.id WHERE tr.customer_id = ? AND tr.sample_status = 'Validated' ORDER BY tr.validated_at DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientUser.getId());
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) completedRequests.add(mapResultSetToRequest(rs)); }
            resultsTable.setItems(completedRequests);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadAndDisplayResult(TestRequest req) {
        selectedRequestForResult = req;
        viewTestNameLabel.setText(req.getTestTypeName());
        viewDateLabel.setText(req.getValidatedAt() != null ? req.getValidatedAt().toString() : "N/A");
        viewPatientNameLabel.setText(patientUser.getName());
        viewPatientEmailLabel.setText(patientUser.getEmail());
        viewFormatLabel.setText(req.getTestTypeResultFormat() != null ? req.getTestTypeResultFormat().toUpperCase() : "—");
        viewOrderedLabel.setText(req.getCreatedAt() != null ? req.getCreatedAt().toString() : "—");
        viewContentBox.setVisible(false); viewContentBox.setManaged(false);
        viewImageContainer.setVisible(false); viewImageContainer.setManaged(false);
        viewPdfBox.setVisible(false); viewPdfBox.setManaged(false);
        downloadAttachmentButton.setVisible(false);
        if (viewValidatorLabel != null) viewValidatorLabel.setText("—");

        String sql = "SELECT r.*, u.name as validator_name FROM results r LEFT JOIN users u ON r.validated_by = u.id WHERE r.request_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, req.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentLoadedResult = new TestResult();
                    currentLoadedResult.setId(rs.getInt("id")); currentLoadedResult.setRequestId(rs.getInt("request_id"));
                    currentLoadedResult.setResultValue(rs.getString("result_value")); currentLoadedResult.setFileName(rs.getString("file_name"));
                    currentLoadedResult.setFileData(rs.getBytes("file_data")); currentLoadedResult.setValidated(rs.getBoolean("is_validated"));
                    currentLoadedResult.setValidatedByName(rs.getString("validator_name")); currentLoadedResult.setValidatedAt(rs.getTimestamp("validated_at"));
                    if (viewValidatorLabel != null) viewValidatorLabel.setText(currentLoadedResult.getValidatedByName() != null ? currentLoadedResult.getValidatedByName() : "Lab Staff");
                    String fmt = req.getTestTypeResultFormat() != null ? req.getTestTypeResultFormat().toLowerCase() : "";
                    if ("numeric".equals(fmt) || "text".equals(fmt)) {
                        viewContentBox.setVisible(true); viewContentBox.setManaged(true);
                        String val = currentLoadedResult.getResultValue();
                        viewResultValLabel.setText(val != null && !val.isEmpty() ? val : "(No result value entered)");
                    } else if ("image".equals(fmt)) {
                        viewImageContainer.setVisible(true); viewImageContainer.setManaged(true); downloadAttachmentButton.setVisible(true);
                        if (currentLoadedResult.getFileData() != null) {
                            try { viewImageView.setImage(new Image(new ByteArrayInputStream(currentLoadedResult.getFileData()))); } catch (Exception e) { System.err.println("Failed to display image: " + e.getMessage()); }
                        }
                    } else if ("pdf".equals(fmt)) {
                        viewPdfBox.setVisible(true); viewPdfBox.setManaged(true); downloadAttachmentButton.setVisible(true);
                    } else {
                        viewContentBox.setVisible(true); viewContentBox.setManaged(true);
                        String val = currentLoadedResult.getResultValue();
                        viewResultValLabel.setText(val != null && !val.isEmpty() ? val : "(No result data available)");
                        if (currentLoadedResult.getFileData() != null) downloadAttachmentButton.setVisible(true);
                    }
                } else {
                    viewContentBox.setVisible(true); viewContentBox.setManaged(true);
                    viewResultValLabel.setText("Result is validated but data has not been entered yet. Contact the lab.");
                }
            }
            resultViewerCard.setVisible(true);
        } catch (SQLException e) { e.printStackTrace(); showAlert("Error", "Failed to retrieve results from database."); }
    }

    private void hideResultViewer() { resultViewerCard.setVisible(false); selectedRequestForResult = null; currentLoadedResult = null; }

    @FXML
    void handlePrintResult(ActionEvent event) {
        if (selectedRequestForResult == null) { showAlert("Nothing Selected", "Please select a result from the list to print."); return; }
        StringBuilder content = new StringBuilder();
        content.append("════════════════════════════════════════════════════\n");
        content.append("         SANTE DIAGNOSTICS LTD\n         Laboratory Diagnostic Report\n");
        content.append("════════════════════════════════════════════════════\n\nPATIENT INFORMATION\n────────────────────────────────────────────────────\n");
        content.append("Name          : ").append(patientUser.getName()).append("\nEmail         : ").append(patientUser.getEmail()).append("\n\nTEST DETAILS\n────────────────────────────────────────────────────\n");
        content.append("Test Name     : ").append(selectedRequestForResult.getTestTypeName()).append("\n");
        content.append("Date Ordered  : ").append(selectedRequestForResult.getCreatedAt() != null ? selectedRequestForResult.getCreatedAt().toString() : "N/A").append("\n");
        content.append("Date Released : ").append(selectedRequestForResult.getValidatedAt() != null ? selectedRequestForResult.getValidatedAt().toString() : "N/A").append("\n");
        if (currentLoadedResult != null && currentLoadedResult.getValidatedByName() != null) content.append("Validated By  : ").append(currentLoadedResult.getValidatedByName()).append("\n");
        content.append("\nRESULT\n────────────────────────────────────────────────────\n");
        if (currentLoadedResult != null && currentLoadedResult.getResultValue() != null && !currentLoadedResult.getResultValue().isEmpty()) content.append(currentLoadedResult.getResultValue()).append("\n");
        else if (currentLoadedResult != null && currentLoadedResult.getFileData() != null) content.append("[Attachment: ").append(currentLoadedResult.getFileName()).append("]\nPlease download the file for the full report.\n");
        else content.append("Result data not available in text form.\n");
        content.append("\n════════════════════════════════════════════════════\n");
        Text printText = new Text(content.toString()); printText.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");
        TextFlow printNode = new TextFlow(printText); printNode.setPrefWidth(550); printNode.setStyle("-fx-padding: 30px;");
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            boolean proceed = job.showPrintDialog(App.getPrimaryStage());
            if (proceed) { boolean printed = job.printPage(printNode); if (printed) { job.endJob(); AuditService.log(patientUser.getId(), patientUser.getEmail(), "PRINT_REPORT", "Printed result for: " + selectedRequestForResult.getTestTypeName() + " (Request ID: " + selectedRequestForResult.getId() + ")"); showInfo("Print Successful", "Report sent to printer successfully."); } else showAlert("Print Failed", "The print job failed. Please try again."); }
        } else showAlert("No Printer", "No printer is available on this machine.");
    }

    @FXML
    void handleDownloadAttachment(ActionEvent event) {
        if (currentLoadedResult == null || currentLoadedResult.getFileData() == null) { showAlert("No Attachment", "No attachment data is loaded for this result."); return; }
        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Save Diagnostic Attachment");
        saveChooser.setInitialFileName(currentLoadedResult.getFileName());
        String format = selectedRequestForResult.getTestTypeResultFormat();
        if ("PDF".equals(format)) saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        else if ("image".equals(format)) saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = saveChooser.showSaveDialog(App.getPrimaryStage());
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(currentLoadedResult.getFileData()); fos.flush();
                AuditService.log(patientUser.getId(), patientUser.getEmail(), "DOWNLOAD_REPORT", "Downloaded result file: " + currentLoadedResult.getFileName());
                showInfo("File Saved", "Report file downloaded successfully to:\n" + file.getAbsolutePath());
            } catch (Exception e) { e.printStackTrace(); showAlert("Error", "Failed to save file locally."); }
        }
    }

    // ========== MY PROFILE: Edit + Change Password ==========

    @FXML
    void handleSaveProfile(ActionEvent event) {
        if (profileNameField == null) return;
        String newName = profileNameField.getText().trim();
        String newEmail = profileEmailField.getText().trim();
        if (newName.isEmpty() || newEmail.isEmpty()) {
            profileErrorLabel.setText("Name and email are required."); profileErrorLabel.setVisible(true); return;
        }
        String sql = "UPDATE users SET name=?, email=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName); pstmt.setString(2, newEmail); pstmt.setInt(3, patientUser.getId());
            pstmt.executeUpdate();
            patientUser.setName(newName); patientUser.setEmail(newEmail);
            patientNameLabel.setText(newName); patientEmailLabel.setText(newEmail);
            AuditService.log(patientUser.getId(), patientUser.getEmail(), "UPDATE_PROFILE", "Updated profile: name=" + newName + ", email=" + newEmail);
            profileErrorLabel.setVisible(false);
            showInfo("Profile Saved", "Your profile has been updated successfully.");
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { profileErrorLabel.setText("That email is already in use."); profileErrorLabel.setVisible(true); }
            else { e.printStackTrace(); profileErrorLabel.setText("Failed to update profile."); profileErrorLabel.setVisible(true); }
        }
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        if (currentPassField == null) return;
        String currentPass = currentPassField.getText();
        String newPass = newPassField.getText();
        String confirmPass = confirmPassField.getText();
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            passErrorLabel.setText("All password fields are required."); passErrorLabel.setVisible(true); return;
        }
        if (!SecurityUtils.checkPassword(currentPass, patientUser.getPasswordHash())) {
            passErrorLabel.setText("Current password is incorrect."); passErrorLabel.setVisible(true); return;
        }
        if (!newPass.equals(confirmPass)) {
            passErrorLabel.setText("New passwords do not match."); passErrorLabel.setVisible(true); return;
        }
        if (newPass.length() < 6) {
            passErrorLabel.setText("New password must be at least 6 characters."); passErrorLabel.setVisible(true); return;
        }
        String hash = SecurityUtils.hashPassword(newPass);
        String sql = "UPDATE users SET password_hash=?, must_change_password=false WHERE id=?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hash); pstmt.setInt(2, patientUser.getId()); pstmt.executeUpdate();
            patientUser.setPasswordHash(hash);
            AuditService.log(patientUser.getId(), patientUser.getEmail(), "CHANGE_PASSWORD", "User changed their own password.");
            currentPassField.clear(); newPassField.clear(); confirmPassField.clear(); passErrorLabel.setVisible(false);
            showInfo("Password Changed", "Your password has been updated successfully.");
        } catch (SQLException e) { e.printStackTrace(); passErrorLabel.setText("Failed to update password."); passErrorLabel.setVisible(true); }
    }

    // ========== GENERAL ==========

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.WARNING); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
    private void showInfo(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }

    @FXML
    void handleLogout(ActionEvent event) {
        if (countdownTimeline != null) countdownTimeline.stop();
        AuditService.log(patientUser.getId(), patientUser.getEmail(), "LOGOUT", "User logged out.");
        App.setCurrentUser(null);
        App.switchScene("login.fxml", "Sante Diagnostics - Login", 800, 600);
    }

    private TestRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
        TestRequest tr = new TestRequest();
        tr.setId(rs.getInt("id")); tr.setCustomerId(rs.getInt("customer_id"));
        tr.setCustomerName(rs.getString("customer_name")); tr.setCustomerEmail(rs.getString("customer_email"));
        tr.setTestTypeId(rs.getInt("test_type_id")); tr.setTestTypeName(rs.getString("test_name"));
        tr.setTestTypePrice(rs.getDouble("test_price")); tr.setTestTypeTatHours(rs.getInt("test_tat"));
        tr.setTestTypeResultFormat(rs.getString("test_format")); tr.setPaymentStatus(rs.getString("payment_status"));
        tr.setPaymentEvidence(rs.getString("payment_evidence")); tr.setSampleStatus(rs.getString("sample_status"));
        tr.setCreatedAt(rs.getTimestamp("created_at")); tr.setCollectedAt(rs.getTimestamp("collected_at"));
        tr.setProcessedAt(rs.getTimestamp("processed_at")); tr.setValidatedAt(rs.getTimestamp("validated_at"));
        tr.setDueAt(rs.getTimestamp("due_at"));
        return tr;
    }
}
