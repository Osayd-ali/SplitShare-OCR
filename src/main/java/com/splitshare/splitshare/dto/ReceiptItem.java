package com.splitshare.splitshare.dto;

/**
     * Data class for individual receipt items.
     * Represents a single purchased item with its name and price.
     */
public class ReceiptItem {
    private String name;
    private double price;
    private int quantity;

    /**
     * Constructor for creating a new receipt item
     *
     * @param name The item description
     * @param price The item price
     * @param quantity The item quantity
     * 
     */
    public ReceiptItem(String name, double price,int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters and setters

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

    public int getQuantity() { 
        return quantity; 
    }

    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
    }
}
