package com.yashik.shockpulse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.yashik.shockpulse.concurrency.CheckoutWorkerCallable;
import com.yashik.shockpulse.concurrency.OrderBuffer;
import com.yashik.shockpulse.concurrency.OrderRequest;
import com.yashik.shockpulse.dao.GenericRepository;
import com.yashik.shockpulse.dao.OrderDAO;
import com.yashik.shockpulse.dao.ProductDAO;
import com.yashik.shockpulse.exception.OutOfStockException;
import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.model.Product;
import com.yashik.shockpulse.service.AnalyticsService;
import com.yashik.shockpulse.service.InventoryManager;
import com.yashik.shockpulse.service.ThreadSafeInventoryService;
import com.yashik.shockpulse.util.CsvInventoryHandler;
import com.yashik.shockpulse.util.DBConnectionManager;

public class Main {
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ProductDAO productDAO = new ProductDAO();
        OrderDAO orderDAO = new OrderDAO();
        InventoryManager memoryManager = new InventoryManager();
        AnalyticsService analyticsService = new AnalyticsService();

        // Lab 20: GenericRepository<T, ID> demonstration
        GenericRepository<Product, String> productCache = new GenericRepository<>();

        // Seed initial in-memory state
        Product p1 = new Product("SP-101", "Mechanical Keyboard", "Peripherals", 2499.00, 4);
        Product p2 = new Product("SP-102", "RTX 4070 Super GPU", "Graphics", 52999.00, 2);
        memoryManager.addProduct(p1);
        memoryManager.addProduct(p2);
        productCache.store(p1.getProductId(), p1);
        productCache.store(p2.getProductId(), p2);

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print(YELLOW + "Select option [1-8]: " + RESET);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewSupabaseInventory(productDAO);
                case "2" -> addProductToSupabase(scanner, productDAO, memoryManager, productCache);
                case "3" -> executeSingleAcidCheckout(scanner, orderDAO, memoryManager);
                case "4" -> runConcurrentFlashSaleSimulation(memoryManager);
                case "5" -> runStreamAnalyticsDashboard(memoryManager, analyticsService);
                case "6" -> importCsvCatalog(memoryManager, productCache);
                case "7" -> exportOrdersCsv(memoryManager);
                case "8" -> {
                    running = false;
                    System.out.println(CYAN + "Shutting down ShockPulse Engine. Goodbye!" + RESET);
                }
                default -> System.out.println(RED + "Invalid choice. Enter 1-8." + RESET);
            }
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println(CYAN + "\n==============================================================" + RESET);
        System.out.println(CYAN + "   SHOCKPULSE v1.0 - FLASH-SALE & ACID INVENTORY ENGINE       " + RESET);
        System.out.println(CYAN + "==============================================================" + RESET);
        System.out.println("  1. [JDBC] View Live Supabase Inventory & DB Metadata (Labs 10-15)");
        System.out.println("  2. [JDBC] Add New Flash-Sale Product to Supabase (Lab 12-13)");
        System.out.println("  3. [ACID] Place Atomic Order Transaction in Supabase (Lab 16)");
        System.out.println("  4. [CONCURRENCY] Run Multi-Threaded Flash-Sale Rush (Labs 1-9)");
        System.out.println("  5. [STREAMS] Generate Lambda & Collectors Analytics (Labs 17-19)");
        System.out.println("  6. [FILE I/O] Import Restock Catalog from CSV (Lab 20)");
        System.out.println("  7. [FILE I/O] Export Order Audit Report to CSV (Lab 20)");
        System.out.println("  8. Exit");
    }

    private static void viewSupabaseInventory(ProductDAO productDAO) {
        System.out.println(YELLOW + "\n--- Supabase Live Inventory & Metadata ---" + RESET);
        DBConnectionManager.printDatabaseMetadata();
        try {
            List<Product> dbProducts = productDAO.getAllProducts();
            dbProducts.forEach(p -> System.out.println("  " + p));
        } catch (SQLException e) {
            DBConnectionManager.printSqlException(e);
        }
    }

    private static void addProductToSupabase(Scanner scanner, ProductDAO productDAO,
            InventoryManager memoryManager,
            GenericRepository<Product, String> cache) {
        try {
            System.out.print("  Product ID (e.g., SP-105): ");
            String id = scanner.nextLine();
            System.out.print("  Name: ");
            String name = scanner.nextLine();
            System.out.print("  Category: ");
            String category = scanner.nextLine();
            System.out.print("  Flash Price (Rs.): ");
            double price = Double.parseDouble(scanner.nextLine());
            System.out.print("  Stock Quantity: ");
            int stock = Integer.parseInt(scanner.nextLine());

            Product product = new Product(id, name, category, price, stock);
            int rows = productDAO.insertProduct(product);
            memoryManager.addProduct(product);
            cache.store(product.getProductId(), product);
            System.out.println(GREEN + "  Inserted into Supabase (" + rows
                    + " row affected) & cached in GenericRepository!" + RESET);
        } catch (SQLException e) {
            DBConnectionManager.printSqlException(e);
        } catch (IllegalArgumentException e) {
            System.out.println(RED + "  Validation Error: " + e.getMessage() + RESET);
        }
    }

    private static void executeSingleAcidCheckout(Scanner scanner, OrderDAO orderDAO, InventoryManager memoryManager) {
        try {
            System.out.print("  Customer ID (e.g., CUST-01, CUST-02, CUST-03): ");
            String custId = scanner.nextLine();
            System.out.print("  Product ID (e.g., SP-101, SP-102): ");
            String prodId = scanner.nextLine();
            System.out.print("  Quantity: ");
            int qty = Integer.parseInt(scanner.nextLine());

            Order order = orderDAO.placeOrderTransaction(custId, prodId, qty);
            memoryManager.getOrderHistory().add(order);
            System.out.println(GREEN + "  [COMMITTED] " + order + RESET);
        } catch (SQLException | OutOfStockException | IllegalArgumentException e) {
            System.out.println(RED + "  [ABORTED] " + e.getMessage() + RESET);
        }
    }

    private static void runConcurrentFlashSaleSimulation(InventoryManager memoryManager) {
        System.out.println(YELLOW + "\n--- Launching Bounded-Buffer Multi-Threaded Rush (Labs 7-9) ---" + RESET);
        ThreadSafeInventoryService safeService = new ThreadSafeInventoryService(memoryManager);
        OrderBuffer buffer = new OrderBuffer(4);
        ExecutorService producers = Executors.newFixedThreadPool(3);
        ExecutorService workers = Executors.newFixedThreadPool(3);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(buffer::closeSale, 1200, TimeUnit.MILLISECONDS);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(workers.submit(new CheckoutWorkerCallable(buffer, safeService)));
        }

        List<OrderRequest> burst = List.of(
                new OrderRequest("CUST-11", "SP-102", 1),
                new OrderRequest("CUST-12", "SP-102", 1),
                new OrderRequest("CUST-13", "SP-102", 1),
                new OrderRequest("CUST-14", "SP-101", 2),
                new OrderRequest("CUST-15", "SP-101", 2),
                new OrderRequest("CUST-16", "SP-101", 1));

        for (OrderRequest req : burst) {
            producers.submit(() -> {
                try {
                    buffer.produce(req);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        try {
            producers.shutdown();
            producers.awaitTermination(5, TimeUnit.SECONDS);
            int confirmedTotal = 0;
            for (Future<Integer> f : futures) {
                confirmedTotal += f.get();
            }
            workers.shutdown();
            workers.awaitTermination(5, TimeUnit.SECONDS);
            scheduler.shutdown();
            System.out.println(GREEN + "  Simulation Complete! Total Confirmed Orders: " + confirmedTotal + RESET);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(RED + "  Simulation error: " + e.getMessage() + RESET);
        }
    }

    private static void runStreamAnalyticsDashboard(InventoryManager memoryManager, AnalyticsService analytics) {
        List<Product> products = memoryManager.getProductsSortedByStockDesc();
        List<Order> orders = memoryManager.getOrderHistory();
        analytics.demonstrateFunctionalInterfaces(products);
        analytics.runStreamPipelineDemo(products, orders);
        analytics.generateCollectorsReport(products, orders);
    }

    private static void importCsvCatalog(InventoryManager memoryManager, GenericRepository<Product, String> cache) {
        String path = "src/main/resources/restock_catalog.csv";
        try {
            List<Product> imported = CsvInventoryHandler.importProductsFromCsv(path);
            for (Product p : imported) {
                try {
                    memoryManager.addProduct(p);
                    cache.store(p.getProductId(), p);
                } catch (IllegalArgumentException dup) {
                    System.out.println("  Skipped existing item: " + p.getProductId());
                }
            }
            System.out.println(GREEN + "  Imported " + imported.size() + " products from " + path
                    + " (Cache size: " + cache.count() + ")" + RESET);
        } catch (IOException e) {
            System.out.println(RED + "  CSV Import failed: " + e.getMessage() + RESET);
        }
    }

    private static void exportOrdersCsv(InventoryManager memoryManager) {
        String outPath = "sales_audit_report.csv";
        try {
            CsvInventoryHandler.exportOrdersToCsv(memoryManager.getOrderHistory(), outPath);
            System.out.println(GREEN + "  Exported " + memoryManager.getOrderHistory().size()
                    + " orders to " + outPath + RESET);
        } catch (IOException e) {
            System.out.println(RED + "  CSV Export failed: " + e.getMessage() + RESET);
        }
    }
}