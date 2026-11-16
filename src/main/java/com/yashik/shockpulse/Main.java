package com.yashik.shockpulse;

import com.yashik.shockpulse.exception.DuplicateOrderException;
import com.yashik.shockpulse.exception.OutOfStockException;
import com.yashik.shockpulse.exception.ProductNotFoundException;
import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.model.Product;
import com.yashik.shockpulse.service.InventoryManager;

public class Main {
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        System.out.println(CYAN + "================================================================" + RESET);
        System.out.println(CYAN + "   SHOCKPULSE: PHASE 1 - COLLECTIONS & RECORD MANAGER (LAB 1)   " + RESET);
        System.out.println(CYAN + "================================================================" + RESET);

        InventoryManager manager = new InventoryManager();

        // 1. Add Products (ArrayList + HashMap + HashSet)
        manager.addProduct(new Product("SP-101", "Mechanical Keyboard", "Peripherals", 2499.00, 3));
        manager.addProduct(new Product("SP-102", "RTX 4070 Super GPU", "Graphics", 52999.00, 1));
        manager.addProduct(new Product("SP-103", "NVMe Gen4 SSD 1TB", "Storage", 5499.00, 8));
        manager.addProduct(new Product("SP-104", "Gaming Mouse 26K DPI", "Peripherals", 1899.00, 5));

        System.out.println(YELLOW + "\n[1] Initial Inventory (Insertion Order):" + RESET);
        manager.displayAllProducts();

        // 2. Search & Update (HashMap O(1) lookup)
        try {
            System.out.println(YELLOW + "\n[2] Searching & Updating SP-101 Stock to 5..." + RESET);
            manager.updateProductStock("SP-101", 5);
            System.out.println(GREEN + "  Updated: " + manager.searchProductById("SP-101") + RESET);
        } catch (ProductNotFoundException e) {
            System.out.println(RED + "  Error: " + e.getMessage() + RESET);
        }

        // 3. Process Flash-Sale Orders & Trigger Custom Exceptions
        System.out.println(YELLOW + "\n[3] Executing Flash-Sale Orders & Exception Checks:" + RESET);
        attemptOrder(manager, "CUST-01", "SP-102", 1); // Confirms last GPU
        attemptOrder(manager, "CUST-02", "SP-102", 1); // Fails -> OutOfStockException
        attemptOrder(manager, "CUST-01", "SP-101", 2); // Confirms 2 Keyboards
        attemptOrder(manager, "CUST-01", "SP-101", 1); // Fails -> DuplicateOrderException (HashSet)
        attemptOrder(manager, "CUST-03", "SP-103", 2); // Confirms 2 SSDs

        // 4. Sort Products by Stock Descending using Comparator
        System.out.println(YELLOW + "\n[4] Products Sorted by Remaining Stock (Comparator):" + RESET);
        for (Product p : manager.getProductsSortedByStockDesc()) {
            System.out.println("  " + p);
        }

        // 5. Sort Orders by Total Amount Descending using Comparator
        System.out.println(YELLOW + "\n[5] Order Ledger Sorted by Total Value (Comparator):" + RESET);
        for (Order o : manager.getOrdersSortedByAmountDesc()) {
            System.out.println("  " + o);
        }
    }

    private static void attemptOrder(InventoryManager manager, String customerId, String productId, int qty) {
        try {
            Order order = manager.placeFlashOrder(customerId, productId, qty);
            System.out.println(GREEN + "  SUCCESS: " + order + RESET);
        } catch (OutOfStockException | DuplicateOrderException | ProductNotFoundException e) {
            System.out.println(RED + "  REJECTED [" + e.getClass().getSimpleName() + "]: " + e.getMessage() + RESET);
        }
    }
}