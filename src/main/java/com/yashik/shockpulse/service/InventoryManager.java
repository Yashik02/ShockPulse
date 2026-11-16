package com.yashik.shockpulse.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yashik.shockpulse.exception.DuplicateOrderException;
import com.yashik.shockpulse.exception.OutOfStockException;
import com.yashik.shockpulse.exception.ProductNotFoundException;
import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.model.Order.OrderStatus;
import com.yashik.shockpulse.model.Product;

public class InventoryManager {
    // 1. ArrayList to store records (Lab 1 requirement)
    private final List<Product> productList = new ArrayList<>();
    private final List<Order> orderHistory = new ArrayList<>();

    // 2. HashSet to maintain unique product names & unique customer claims (Lab 1
    // requirement)
    private final Set<String> uniqueProductNames = new HashSet<>();
    private final Set<String> uniqueCustomerClaims = new HashSet<>();

    // 3. HashMap<ID, Product> for O(1) ID-based searching (Lab 1 requirement)
    private final Map<String, Product> productMap = new HashMap<>();

    private int orderSequence = 1000;

    // CREATE / ADD
    public void addProduct(Product product) {
        String id = product.getProductId();
        String normalizedName = product.getName().toLowerCase();

        if (productMap.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate Product ID: " + id);
        }
        if (uniqueProductNames.contains(normalizedName)) {
            throw new IllegalArgumentException("Duplicate Product Name: " + product.getName());
        }

        productList.add(product);
        productMap.put(id, product);
        uniqueProductNames.add(normalizedName);
    }

    // READ / SEARCH BY ID (O(1) via HashMap)
    public Product searchProductById(String productId) throws ProductNotFoundException {
        Product product = productMap.get(productId.trim().toUpperCase());
        if (product == null) {
            throw new ProductNotFoundException("Product ID '" + productId + "' not found in inventory.");
        }
        return product;
    }

    // UPDATE STOCK & PRICE
    public void updateProductStock(String productId, int newStock) throws ProductNotFoundException {
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        Product product = searchProductById(productId);
        product.setStockQuantity(newStock);
    }

    // DELETE PRODUCT
    public Product deleteProduct(String productId) throws ProductNotFoundException {
        Product target = searchProductById(productId);
        productMap.remove(target.getProductId());
        productList.remove(target);
        uniqueProductNames.remove(target.getName().toLowerCase());
        return target;
    }

    // DISPLAY ALL (Unsorted / Insertion Order)
    public void displayAllProducts() {
        if (productList.isEmpty()) {
            System.out.println("Inventory is currently empty.");
            return;
        }
        for (Product p : productList) {
            System.out.println("  " + p);
        }
    }

    // SORT BY CUSTOM COMPARATORS (Lab 1 & Lectures 23-25 requirement)
    public List<Product> getProductsSortedByStockDesc() {
        List<Product> sorted = new ArrayList<>(productList);
        Comparator<Product> byStockDesc = (p1, p2) -> Integer.compare(p2.getStockQuantity(), p1.getStockQuantity());
        sorted.sort(byStockDesc);
        return sorted;
    }

    public List<Order> getOrdersSortedByAmountDesc() {
        List<Order> sorted = new ArrayList<>(orderHistory);
        sorted.sort(Comparator.comparingDouble(Order::getTotalAmount).reversed());
        return sorted;
    }

    // FLASH SALE CHECKOUT (Single-threaded baseline for Phase 1)
    public Order placeFlashOrder(String customerId, String productId, int quantity)
            throws ProductNotFoundException, OutOfStockException, DuplicateOrderException {

        String claimKey = customerId.trim().toUpperCase() + ":" + productId.trim().toUpperCase();
        if (uniqueCustomerClaims.contains(claimKey)) {
            throw new DuplicateOrderException(
                    "Customer " + customerId + " already claimed flash deal on " + productId);
        }

        Product product = searchProductById(productId);
        if (product.getStockQuantity() < quantity) {
            Order failedOrder = new Order(
                    "ORD-" + (++orderSequence),
                    customerId,
                    productId,
                    quantity,
                    0.0,
                    OrderStatus.FAILED);
            orderHistory.add(failedOrder);
            throw new OutOfStockException(
                    "Insufficient stock for " + product.getName() + " (Requested: " + quantity + ", Available: "
                            + product.getStockQuantity() + ")");
        }

        // Deduct stock and record unique claim
        product.setStockQuantity(product.getStockQuantity() - quantity);
        uniqueCustomerClaims.add(claimKey);

        double total = product.getFlashPrice() * quantity;
        Order confirmedOrder = new Order(
                "ORD-" + (++orderSequence),
                customerId,
                productId,
                quantity,
                total,
                OrderStatus.CONFIRMED);
        orderHistory.add(confirmedOrder);
        return confirmedOrder;
    }

    public List<Order> getOrderHistory() {
        return orderHistory;
    }
}