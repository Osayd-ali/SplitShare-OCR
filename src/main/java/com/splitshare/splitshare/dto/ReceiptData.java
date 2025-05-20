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

    private double subtotal;
    private double tax;
    private double tip;
    
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

    
    public double getSubtotal() { 
        return subtotal; 
    }
    public void setSubtotal(double subtotal) { 
        this.subtotal = subtotal; 
    }

    public double getTax() { 
        return tax; 
    }
    public void setTax(double tax) { 
        this.tax = tax; 
    }

    public double getTip() { 
        return tip; 
    }
    public void setTip(double tip) { 
        this.tip = tip; 
    }

    public double expectedSubTotal() {
        double subtotal = 0;
        for (ReceiptItem item : items) {
            subtotal += item.getPrice();
        }
        return subtotal;
    }
}
