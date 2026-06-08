package com.sante.lims.model;

import java.sql.Timestamp;

public class TestRequest {
    private int id;
    private int customerId;
    private String customerName;
    private String customerEmail;
    private int testTypeId;
    private String testTypeName;
    private double testTypePrice;
    private int testTypeTatHours;
    private String testTypeResultFormat;
    private String paymentStatus;
    private String paymentEvidence;
    private String sampleStatus;
    private Timestamp createdAt;
    private Timestamp collectedAt;
    private Timestamp processedAt;
    private Timestamp validatedAt;
    private Timestamp dueAt;

    public TestRequest() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public int getTestTypeId() {
        return testTypeId;
    }

    public void setTestTypeId(int testTypeId) {
        this.testTypeId = testTypeId;
    }

    public String getTestTypeName() {
        return testTypeName;
    }

    public void setTestTypeName(String testTypeName) {
        this.testTypeName = testTypeName;
    }

    public double getTestTypePrice() {
        return testTypePrice;
    }

    public void setTestTypePrice(double testTypePrice) {
        this.testTypePrice = testTypePrice;
    }

    public int getTestTypeTatHours() {
        return testTypeTatHours;
    }

    public void setTestTypeTatHours(int testTypeTatHours) {
        this.testTypeTatHours = testTypeTatHours;
    }

    public String getTestTypeResultFormat() {
        return testTypeResultFormat;
    }

    public void setTestTypeResultFormat(String testTypeResultFormat) {
        this.testTypeResultFormat = testTypeResultFormat;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentEvidence() {
        return paymentEvidence;
    }

    public void setPaymentEvidence(String paymentEvidence) {
        this.paymentEvidence = paymentEvidence;
    }

    public String getSampleStatus() {
        return sampleStatus;
    }

    public void setSampleStatus(String sampleStatus) {
        this.sampleStatus = sampleStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Timestamp collectedAt) {
        this.collectedAt = collectedAt;
    }

    public Timestamp getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Timestamp processedAt) {
        this.processedAt = processedAt;
    }

    public Timestamp getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Timestamp validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Timestamp getDueAt() {
        return dueAt;
    }

    public void setDueAt(Timestamp dueAt) {
        this.dueAt = dueAt;
    }

    /**
     * Calculates the seconds remaining until the due time.
     * Returns 0 or negative if the due time has passed.
     */
    public long getSecondsRemaining() {
        if (dueAt == null) return 0;
        long diff = dueAt.getTime() - System.currentTimeMillis();
        return Math.max(0, diff / 1000);
    }

    /**
     * Formats the remaining time as HH:MM:SS or "Time's up" or "Completed".
     */
    public String getCountdownText() {
        if ("Validated".equals(sampleStatus)) {
            return "Completed";
        }
        
        long seconds = getSecondsRemaining();
        if (seconds <= 0) {
            return "Overdue / Pending Result";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
