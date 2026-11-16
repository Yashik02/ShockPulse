package com.yashik.shockpulse.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Order {
    public enum OrderStatus {
        PENDING, CONFIRMED, FAILED
    }

    private final String orderId;
    private final String customerId;
    private final String productId;
    private final int quantity;
    private final double totalAmount;
    private OrderStatus status;
    private final LocalDateTime timestamp;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public Order(String orderId, String customerId, String productId, int quantity, double totalAmount,
            OrderStatus status) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be at least 1.");
        }
        if (totalAmount < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative.");
        }
        this.orderId = orderId;
        this.customerId = customerId.trim().toUpperCase();
        this.productId = productId.trim().toUpperCase();
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("Order[%-7s] | Cust: %-8s | Prod: %-6s | Qty: %2d | Rs. %9.2f | %-9s | %s",
                orderId, customerId, productId, quantity, totalAmount, status, timestamp.format(FORMATTER));
    }
}