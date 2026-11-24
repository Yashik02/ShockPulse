package com.yashik.shockpulse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.yashik.shockpulse.concurrency.CheckoutWorkerCallable;
import com.yashik.shockpulse.concurrency.OrderBuffer;
import com.yashik.shockpulse.concurrency.OrderRequest;
import com.yashik.shockpulse.model.Product;
import com.yashik.shockpulse.service.InventoryManager;
import com.yashik.shockpulse.service.ThreadSafeInventoryService;

public class Main {
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println(CYAN + "======================================================================" + RESET);
        System.out.println(CYAN + "   SHOCKPULSE: PHASE 4 - PRODUCER-CONSUMER & THREAD POOLS (LABS 7-9)  " + RESET);
        System.out.println(CYAN + "======================================================================" + RESET);

        // Setup inventory with limited flash-sale stock (Total 6 items available)
        InventoryManager manager = new InventoryManager();
        manager.addProduct(new Product("SP-101", "Mechanical Keyboard", "Peripherals", 2499.00, 4));
        manager.addProduct(new Product("SP-102", "RTX 4070 Super GPU", "Graphics", 52999.00, 2));

        ThreadSafeInventoryService safeService = new ThreadSafeInventoryService(manager);

        // Lab 7: Bounded buffer of capacity 4 (forces producers to wait() when full)
        OrderBuffer orderBuffer = new OrderBuffer(4);

        // Lab 8: Fixed thread pools for producers (API gateway) and consumers (Checkout
        // workers)
        ExecutorService producerPool = Executors.newFixedThreadPool(3);
        ExecutorService workerPool = Executors.newFixedThreadPool(3);

        // Lab 9: ScheduledExecutorService to close the flash-sale window after 1.5
        // seconds
        ScheduledExecutorService saleScheduler = Executors.newSingleThreadScheduledExecutor();
        saleScheduler.schedule(() -> {
            System.out.println(YELLOW + "\n[SCHEDULER] Flash-sale timer expired! Closing OrderBuffer..." + RESET);
            orderBuffer.closeSale();
        }, 1500, TimeUnit.MILLISECONDS);

        // Submit 3 Callable<Integer> checkout workers (Lab 9)
        List<Future<Integer>> workerFutures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            workerFutures.add(workerPool.submit(new CheckoutWorkerCallable(orderBuffer, safeService)));
        }

        // Submit 12 incoming customer requests into the bounded buffer (Lab 7 & 8)
        List<OrderRequest> incomingTraffic = List.of(
                new OrderRequest("CUST-01", "SP-102", 1),
                new OrderRequest("CUST-02", "SP-102", 1),
                new OrderRequest("CUST-03", "SP-102", 1), // Will fail (only 2 GPUs in stock)
                new OrderRequest("CUST-04", "SP-101", 1),
                new OrderRequest("CUST-05", "SP-101", 2),
                new OrderRequest("CUST-06", "SP-101", 1),
                new OrderRequest("CUST-07", "SP-101", 1), // Will fail (4 keyboards already claimed)
                new OrderRequest("CUST-04", "SP-101", 1), // Will fail (Duplicate customer claim)
                new OrderRequest("CUST-08", "SP-102", 1),
                new OrderRequest("CUST-09", "SP-101", 1),
                new OrderRequest("CUST-10", "SP-101", 1),
                new OrderRequest("CUST-11", "SP-102", 1));

        System.out.println(
                YELLOW + "\n[1] Dispatching 12 burst customer requests into Bounded Buffer (Capacity: 4)..." + RESET);
        for (OrderRequest req : incomingTraffic) {
            producerPool.submit(() -> {
                try {
                    orderBuffer.produce(req);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Lab 8: Graceful shutdown of producer pool
        producerPool.shutdown();
        producerPool.awaitTermination(5, TimeUnit.SECONDS);

        // Lab 9: Collect Callable<Integer> results via Future.get()
        int totalConfirmedOrders = 0;
        for (int i = 0; i < workerFutures.size(); i++) {
            int count = workerFutures.get(i).get();
            totalConfirmedOrders += count;
            System.out.printf("  Worker-%d reported %d confirmed orders via Future.get()%n", (i + 1), count);
        }

        // Graceful shutdown of worker pool & scheduler
        workerPool.shutdown();
        workerPool.awaitTermination(5, TimeUnit.SECONDS);
        saleScheduler.shutdown();

        System.out.println(GREEN + "\n[2] Final Post-Sale Audit Summary:" + RESET);
        System.out.println("  Total Confirmed Orders (Aggregated from Futures): " + totalConfirmedOrders);
        manager.displayAllProducts();
    }
}