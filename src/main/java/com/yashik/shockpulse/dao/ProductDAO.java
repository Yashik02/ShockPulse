package com.yashik.shockpulse.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.yashik.shockpulse.model.Product;
import com.yashik.shockpulse.util.DBConnectionManager;

public class ProductDAO {

    // Lab 12 & 13: CREATE using PreparedStatement with ? placeholders
    public int insertProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products (product_id, name, category, flash_price, stock_quantity) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getProductId());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getCategory());
            pstmt.setDouble(4, product.getFlashPrice());
            pstmt.setInt(5, product.getStockQuantity());

            return pstmt.executeUpdate();
        }
    }

    // Lab 14: READ ALL & map ResultSet rows into Java Product objects inside an
    // ArrayList
    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT product_id, name, category, flash_price, stock_quantity FROM products ORDER BY product_id";

        try (Connection conn = DBConnectionManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product p = new Product(
                        rs.getString("product_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("flash_price"),
                        rs.getInt("stock_quantity"));
                products.add(p);
            }
        }
        return products;
    }

    // Lab 12 & 13: SEARCH BY ID using PreparedStatement
    public Product findProductById(String productId) throws SQLException {
        String sql = "SELECT product_id, name, category, flash_price, stock_quantity FROM products WHERE product_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, productId.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getString("product_id"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getDouble("flash_price"),
                            rs.getInt("stock_quantity"));
                }
            }
        }
        return null;
    }

    // Lab 11, 12 & 13: UPDATE stock using PreparedStatement
    public int updateStock(String productId, int newStock) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = ? WHERE product_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newStock);
            pstmt.setString(2, productId.trim().toUpperCase());
            return pstmt.executeUpdate();
        }
    }

    // Lab 11 & 13: DELETE product using PreparedStatement
    public int deleteProduct(String productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, productId.trim().toUpperCase());
            return pstmt.executeUpdate();
        }
    }

    // Lab 15: Inspect ResultSetMetaData (column count, names, SQL types)
    public void printProductsTableSchema() throws SQLException {
        String sql = "SELECT product_id, name, category, flash_price, stock_quantity FROM products LIMIT 1";
        try (Connection conn = DBConnectionManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData rsMeta = rs.getMetaData();
            int colCount = rsMeta.getColumnCount();
            System.out.println("  ResultSet Column Count: " + colCount);
            for (int i = 1; i <= colCount; i++) {
                System.out.printf("    Col %d: %-16s | Type: %-12s | Table: %s%n",
                        i, rsMeta.getColumnName(i), rsMeta.getColumnTypeName(i), rsMeta.getTableName(i));
            }
        }
    }
}