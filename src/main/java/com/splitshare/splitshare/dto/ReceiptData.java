package com.splitshare.splitshare.dto;
import java.util.*;
/**
 * Data class for structured receipt information.
 * Contains all the extracted components of a receipt.
 */
public class ReceiptData {
    private String storeName;
    private Date date;
    private double total;
    private List<ReceiptItem> items;

    /**
     * Default constructor initializes an empty items list
     */
    public ReceiptData() {
        this.items = new ArrayList<>();
    }

    // Getters and setters for all fields

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<ReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItem> items) {
        this.items = items;
    }
}
