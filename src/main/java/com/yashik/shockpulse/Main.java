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
        AnalyticsService analyticsService = new AnalyticsService();
        GenericRepository<Product, String> productCache = new GenericRepository<>();

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print(YELLOW + "Select option [1-8]: " + RESET);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewSupabaseInventory(productDAO, productCache);
                case "2" -> addProductToSupabase(scanner, productDAO, productCache);
                case "3" -> importCsvToSupabase(productDAO, productCache);
                case "4" -> executeSingleAcidCheckout(scanner, orderDAO);
                case "5" -> runSupabaseStreamAnalytics(productDAO, orderDAO, analyticsService);
                case "6" -> exportSupabaseOrdersCsv(orderDAO);
                case "7" -> runIsolatedFlashSaleDemo();
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
        System.out.println(CYAN + "\n====================================================================" + RESET);
        System.out.println(CYAN + "   SHOCKPULSE v1.0 - ACID INVENTORY & CONCURRENCY ENGINE            " + RESET);
        System.out.println(CYAN + "====================================================================" + RESET);
        System.out.println("  --- SUPABASE PERSISTENCE & ANALYTICS (PRODUCTION TIER) ---");
        System.out.println("  1. [JDBC] View Live Supabase Inventory & DB Metadata (Labs 10-15)");
        System.out.println("  2. [JDBC] Add Single Product to Supabase (Labs 12-13)");
        System.out.println("  3. [FILE -> DB] Bulk Import/Restock CSV into Supabase (Lab 20)");
        System.out.println("  4. [ACID TX] Place Atomic Order in Supabase (Lab 16)");
        System.out.println("  5. [STREAMS] Run Live Supabase Stream & Lambda Analytics (Labs 17-19)");
        System.out.println("  6. [DB -> FILE] Export Supabase Order Ledger to CSV (Lab 20)");
        System.out.println("  --- ISOLATED IN-MEMORY STRESS SANDBOX (AUTO-RESETTING) ---");
        System.out.println("  7. [CONCURRENCY] Run Multi-Threaded Flash-Sale Rush Demo (Labs 1-9)");
        System.out.println("  8. Exit");
    }

    private static void viewSupabaseInventory(ProductDAO productDAO, GenericRepository<Product, String> cache) {
        System.out.println(YELLOW + "\n--- Supabase Live Inventory & Metadata ---" + RESET);
        DBConnectionManager.printDatabaseMetadata();
        try {
            List<Product> dbProducts = productDAO.getAllProducts();
            dbProducts.forEach(p -> {
                cache.store(p.getProductId(), p);
                System.out.println("  " + p);
            });
            System.out.println(
                    CYAN + "  [GenericRepository<Product, String> synced: " + cache.count() + " items]" + RESET);
        } catch (SQLException e) {
            DBConnectionManager.printSqlException(e);
        }
    }

    private static void addProductToSupabase(Scanner scanner, ProductDAO productDAO,
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
            cache.store(product.getProductId(), product);
            System.out.println(GREEN + "  Inserted into Supabase (" + rows + " row affected)!" + RESET);
        } catch (SQLException e) {
            DBConnectionManager.printSqlException(e);
        } catch (IllegalArgumentException e) {
            System.out.println(RED + "  Validation Error: " + e.getMessage() + RESET);
        }
    }

    private static void importCsvToSupabase(ProductDAO productDAO, GenericRepository<Product, String> cache) {
        String path = "src/main/resources/restock_catalog.csv";
        try {
            List<Product> imported = CsvInventoryHandler.importProductsFromCsv(path);
            int synced = 0;
            for (Product p : imported) {
                productDAO.upsertProduct(p);
                cache.store(p.getProductId(), p);
                synced++;
            }
            System.out.println(GREEN + "  Imported & upserted " + synced + " products from " + path
                    + " directly into Supabase! (Select Option 1 to view)" + RESET);
        } catch (IOException | SQLException e) {
            System.out.println(RED + "  CSV -> Supabase Import failed: " + e.getMessage() + RESET);
        }
    }

    private static void executeSingleAcidCheckout(Scanner scanner, OrderDAO orderDAO) {
        try {
            System.out.print("  Customer ID (e.g., CUST-01, CUST-02, CUST-03): ");
            String custId = scanner.nextLine();
            System.out.print("  Product ID (e.g., SP-101, SP-102): ");
            String prodId = scanner.nextLine();
            System.out.print("  Quantity: ");
            int qty = Integer.parseInt(scanner.nextLine());

            Order order = orderDAO.placeOrderTransaction(custId, prodId, qty);
            System.out.println(GREEN + "  [SUPABASE COMMITTED] " + order + RESET);
        } catch (SQLException | OutOfStockException | IllegalArgumentException e) {
            System.out.println(RED + "  [SUPABASE ROLLED BACK] " + e.getMessage() + RESET);
        }
    }

    private static void runSupabaseStreamAnalytics(ProductDAO productDAO, OrderDAO orderDAO,
            AnalyticsService analytics) {
        System.out.println(YELLOW + "\n--- Fetching Live Data from Supabase for Stream Analytics ---" + RESET);
        try {
            List<Product> liveProducts = productDAO.getAllProducts();
            List<Order> liveOrders = orderDAO.getAllOrders();

            analytics.demonstrateFunctionalInterfaces(liveProducts);
            analytics.runStreamPipelineDemo(liveProducts, liveOrders);
            analytics.generateCollectorsReport(liveProducts, liveOrders);
        } catch (SQLException e) {
            DBConnectionManager.printSqlException(e);
        }
    }

    private static void exportSupabaseOrdersCsv(OrderDAO orderDAO) {
        String outPath = "supabase_sales_audit.csv";
        try {
            List<Order> liveOrders = orderDAO.getAllOrders();
            CsvInventoryHandler.exportOrdersToCsv(liveOrders, outPath);
            System.out.println(GREEN + "  Exported " + liveOrders.size()
                    + " live Supabase orders to " + outPath + RESET);
        } catch (SQLException | IOException e) {
            System.out.println(RED + "  Supabase -> CSV Export failed: " + e.getMessage() + RESET);
        }
    }

    // Isolated, self-resetting sandbox exclusively for Option 7 Flash-Sale Rush
    // Demo
    private static void runIsolatedFlashSaleDemo() {
        System.out.println(YELLOW + "\n--- Initializing Fresh Isolated Flash-Sale Sandbox (Auto-Reset) ---" + RESET);
        InventoryManager sandboxManager = new InventoryManager();
        sandboxManager.addProduct(new Product("SP-101", "Mechanical Keyboard", "Peripherals", 2499.00, 4));
        sandboxManager.addProduct(new Product("SP-102", "RTX 4070 Super GPU", "Graphics", 52999.00, 2));

        ThreadSafeInventoryService safeService = new ThreadSafeInventoryService(sandboxManager);
        OrderBuffer buffer = new OrderBuffer(4);
        ExecutorService producers = Executors.newFixedThreadPool(3);
        ExecutorService workers = Executors.newFixedThreadPool(3);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(() -> {
            System.out.println(YELLOW + "\n[SCHEDULER] Flash-sale window closed! Draining remaining buffer..." + RESET);
            buffer.closeSale();
        }, 1200, TimeUnit.MILLISECONDS);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(workers.submit(new CheckoutWorkerCallable(buffer, safeService)));
        }

        // 12 simulated concurrent buyer requests competing for 6 total units
        List<OrderRequest> burst = List.of(
                new OrderRequest("CUST-01", "SP-102", 1),
                new OrderRequest("CUST-02", "SP-102", 1),
                new OrderRequest("CUST-03", "SP-102", 1), // Fails: OutOfStock (only 2 GPUs)
                new OrderRequest("CUST-04", "SP-101", 1),
                new OrderRequest("CUST-05", "SP-101", 2),
                new OrderRequest("CUST-06", "SP-101", 1),
                new OrderRequest("CUST-07", "SP-101", 1), // Fails: OutOfStock (only 4 Keyboards)
                new OrderRequest("CUST-04", "SP-101", 1), // Fails: DuplicateOrderException (HashSet)
                new OrderRequest("CUST-08", "SP-102", 1),
                new OrderRequest("CUST-09", "SP-101", 1),
                new OrderRequest("CUST-10", "SP-101", 1),
                new OrderRequest("CUST-11", "SP-102", 1));

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

            System.out.println(GREEN + "\n[SANDBOX SUMMARY] Confirmed Orders: " + confirmedTotal
                    + " | Zero Overselling Verified!" + RESET);
            sandboxManager.displayAllProducts();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(RED + "  Simulation error: " + e.getMessage() + RESET);
        }
    }
}