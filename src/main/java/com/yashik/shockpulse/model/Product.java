package com.yashik.shockpulse.model;

public class Product implements Comparable<Product> {
    private final String productId;
    private String name;
    private String category;
    private double flashPrice;
    private int stockQuantity;

    public Product(String productId, String name, String category, double flashPrice, int stockQuantity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be empty.");
        }
        if (flashPrice < 0 || stockQuantity < 0) {
            throw new IllegalArgumentException("Price and stock cannot be negative.");
        }
        this.productId = productId.trim().toUpperCase();
        this.name = name.trim();
        this.category = category.trim();
        this.flashPrice = flashPrice;
        this.stockQuantity = stockQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getFlashPrice() {
        return flashPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    @Override
    public int compareTo(Product other) {
        return Double.compare(this.flashPrice, other.flashPrice);
    }

    @Override
    public String toString() {
        return String.format("[%s] %-22s | %-12s | Rs. %8.2f | Stock: %d",
                productId, name, category, flashPrice, stockQuantity);
    }
}