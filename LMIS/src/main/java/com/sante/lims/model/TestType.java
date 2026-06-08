package com.sante.lims.model;

import java.sql.Timestamp;

public class TestType {
    private int id;
    private String name;
    private double price;
    private int tatHours;
    private String resultFormat;
    private Timestamp createdAt;

    public TestType() {}

    public TestType(int id, String name, double price, int tatHours, String resultFormat, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.tatHours = tatHours;
        this.resultFormat = resultFormat;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getTatHours() {
        return tatHours;
    }

    public void setTatHours(int tatHours) {
        this.tatHours = tatHours;
    }

    public String getResultFormat() {
        return resultFormat;
    }

    public void setResultFormat(String resultFormat) {
        this.resultFormat = resultFormat;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " ($" + String.format("%.2f", price) + " | TAT: " + tatHours + "h)";
    }
}
