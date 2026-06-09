package com.sante.lims.model;

import java.sql.Timestamp;

public class TestResult {
    private int id;
    private int requestId;
    private String resultValue;
    private String fileName;
    private byte[] fileData;
    private boolean isValidated;
    private Integer validatedBy;
    private String validatedByName;
    private Timestamp validatedAt;

    public TestResult() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public boolean isValidated() {
        return isValidated;
    }

    public void setValidated(boolean validated) {
        isValidated = validated;
    }

    public Integer getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(Integer validatedBy) {
        this.validatedBy = validatedBy;
    }

    public String getValidatedByName() {
        return validatedByName;
    }

    public void setValidatedByName(String validatedByName) {
        this.validatedByName = validatedByName;
    }

    public Timestamp getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Timestamp validatedAt) {
        this.validatedAt = validatedAt;
    }
}
