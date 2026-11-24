package com.yashik.shockpulse.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import com.yashik.shockpulse.exception.OutOfStockException;
import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.model.Order.OrderStatus;
import com.yashik.shockpulse.util.DBConnectionManager;

public class OrderDAO {

    // Lab 16: Atomic ACID Transaction (Stock Deduction + Wallet Deduction + Order
    // Record)
    public Order placeOrderTransaction(String customerId, String productId, int quantity)
            throws SQLException, OutOfStockException {

        String cleanCustId = customerId.trim().toUpperCase();
        String cleanProdId = productId.trim().toUpperCase();
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String fetchPriceSql = "SELECT flash_price, stock_quantity FROM products WHERE product_id = ? FOR UPDATE";
        String deductStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";
        String deductWalletSql = "UPDATE customers SET wallet_balance = wallet_balance - ? WHERE customer_id = ? AND wallet_balance >= ?";
        String insertOrderSql = "INSERT INTO orders (order_id, customer_id, product_id, quantity, total_amount, status) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnectionManager.getConnection();
            // Step 1: Disable auto-commit to start an ACID transaction
            conn.setAutoCommit(false);

            double unitPrice;
            int availableStock;

            try (PreparedStatement fetchStmt = conn.prepareStatement(fetchPriceSql)) {
                fetchStmt.setString(1, cleanProdId);
                try (ResultSet rs = fetchStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Product not found: " + cleanProdId, "23503");
                    }
                    unitPrice = rs.getDouble("flash_price");
                    availableStock = rs.getInt("stock_quantity");
                }
            }

            if (availableStock < quantity) {
                throw new OutOfStockException(
                        "Supabase stock insufficient for " + cleanProdId + " (Available: " + availableStock + ")");
            }

            double totalAmount = unitPrice * quantity;

            // Step 2: Deduct inventory stock
            try (PreparedStatement stockStmt = conn.prepareStatement(deductStockSql)) {
                stockStmt.setInt(1, quantity);
                stockStmt.setString(2, cleanProdId);
                stockStmt.setInt(3, quantity);
                if (stockStmt.executeUpdate() == 0) {
                    throw new OutOfStockException("Concurrent stock depletion for " + cleanProdId);
                }
            }

            // Step 3: Deduct customer wallet balance
            try (PreparedStatement walletStmt = conn.prepareStatement(deductWalletSql)) {
                walletStmt.setDouble(1, totalAmount);
                walletStmt.setString(2, cleanCustId);
                walletStmt.setDouble(3, totalAmount);
                if (walletStmt.executeUpdate() == 0) {
                    throw new SQLException("Insufficient wallet balance or invalid customer: " + cleanCustId, "23514");
                }
            }

            // Step 4: Insert confirmed order record
            try (PreparedStatement orderStmt = conn.prepareStatement(insertOrderSql)) {
                orderStmt.setString(1, orderId);
                orderStmt.setString(2, cleanCustId);
                orderStmt.setString(3, cleanProdId);
                orderStmt.setInt(4, quantity);
                orderStmt.setDouble(5, totalAmount);
                orderStmt.setString(6, OrderStatus.CONFIRMED.name());
                orderStmt.executeUpdate();
            }

            // All 3 updates succeeded -> Commit transaction!
            conn.commit();
            return new Order(orderId, cleanCustId, cleanProdId, quantity, totalAmount, OrderStatus.CONFIRMED);

        } catch (SQLException | OutOfStockException e) {
            // Any failure -> Roll back all partial changes!
            if (conn != null) {
                try {
                    System.err.println("  \u001B[33m[TX ROLLBACK] Reverting database changes due to error...\u001B[0m");
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    DBConnectionManager.printSqlException(rollbackEx);
                }
            }
            if (e instanceof SQLException sqlEx) {
                DBConnectionManager.printSqlException(sqlEx);
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    DBConnectionManager.printSqlException(closeEx);
                }
            }
        }
    }

    // Fetch all persisted orders from Supabase for Stream Analytics & CSV Export
    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT order_id, customer_id, product_id, quantity, total_amount, status FROM orders ORDER BY ordered_at DESC";

        try (Connection conn = DBConnectionManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                orders.add(new Order(
                        rs.getString("order_id"),
                        rs.getString("customer_id"),
                        rs.getString("product_id"),
                        rs.getInt("quantity"),
                        rs.getDouble("total_amount"),
                        OrderStatus.valueOf(rs.getString("status"))));
            }
        }
        return orders;
    }
}