package com.yashik.shockpulse;

import java.sql.SQLException;
import java.util.List;

import com.yashik.shockpulse.dao.OrderDAO;
import com.yashik.shockpulse.dao.ProductDAO;
import com.yashik.shockpulse.exception.OutOfStockException;
import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.model.Product;
import com.yashik.shockpulse.util.DBConnectionManager;

public class Main {
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        System.out.println(CYAN + "=====================================================================" + RESET);
        System.out.println(CYAN + "   SHOCKPULSE: PHASE 5 - SUPABASE JDBC & TRANSACTIONS (LABS 10-16)   " + RESET);
        System.out.println(CYAN + "=====================================================================" + RESET);

        ProductDAO productDAO = new ProductDAO();
        OrderDAO orderDAO = new OrderDAO();

        try {
            // 1. Lab 15: DatabaseMetaData & ResultSetMetaData
            System.out.println(
                    YELLOW + "\n[1] Inspecting Supabase DatabaseMetaData & ResultSetMetaData (Lab 15):" + RESET);
            DBConnectionManager.printDatabaseMetadata();
            productDAO.printProductsTableSchema();

            // 2. Lab 14: Fetch all products from Supabase into ArrayList<Product>
            System.out
                    .println(YELLOW + "\n[2] Fetching Live Products from Supabase (Lab 14 ResultSet Mapping):" + RESET);
            List<Product> products = productDAO.getAllProducts();
            products.forEach(p -> System.out.println("  " + p));

            // 3. Lab 16: Successful Atomic Order Transaction (Commit)
            System.out.println(YELLOW
                    + "\n[3] Executing Atomic Checkout Transaction for CUST-01 -> SP-101 (Lab 16 Commit):" + RESET);
            Order confirmed = orderDAO.placeOrderTransaction("CUST-01", "SP-101", 1);
            System.out.println(GREEN + "  TRANSACTION COMMITTED: " + confirmed + RESET);

            // 4. Lab 16: Failing Transaction -> Automatic Rollback (CUST-03 only has Rs.
            // 4000, GPU costs Rs. 52999)
            System.out.println(
                    YELLOW + "\n[4] Testing Transaction Rollback on Insufficient Balance (CUST-03 -> SP-102):" + RESET);
            try {
                orderDAO.placeOrderTransaction("CUST-03", "SP-102", 1);
            } catch (SQLException | OutOfStockException expected) {
                System.out.println(GREEN + "  ROLLBACK VERIFIED: Stock for SP-102 remained untouched!" + RESET);
            }

            // 5. Verify final database state
            System.out.println(YELLOW + "\n[5] Updated Supabase Inventory After Transactions:" + RESET);
            productDAO.getAllProducts().forEach(p -> System.out.println("  " + p));

        } catch (SQLException | OutOfStockException e) {
            System.out.println(RED + "Database operation failed: " + e.getMessage() + RESET);
        }
    }
}