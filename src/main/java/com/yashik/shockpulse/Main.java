package com.yashik.shockpulse;

import java.util.ArrayList;
import java.util.List;

import com.yashik.shockpulse.concurrency.FlashSaleCheckoutEngine;
import com.yashik.shockpulse.concurrency.FlashSaleCheckoutEngine.SyncMode;
import com.yashik.shockpulse.model.Product;

public class Main {
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args) throws InterruptedException {
        System.out.println(CYAN + "=====================================================================" + RESET);
        System.out.println(CYAN + "   SHOCKPULSE: PHASE 3 - RACE CONDITIONS & SYNCHRONIZATION (LAB 5-6) " + RESET);
        System.out.println(CYAN + "=====================================================================" + RESET);

        int initialStock = 3;
        int concurrentBuyers = 10;

        // 1. Lab 5 Part A: Unsafe Execution (Demonstrates Race Condition / Overselling)
        System.out.println(YELLOW + "\n[TEST 1] Running UNSAFE Checkout (10 Buyers vs 3 Units in Stock):" + RESET);
        long unsafeTime = runSimulation(SyncMode.UNSAFE, initialStock, concurrentBuyers);

        // 2. Lab 5 Part B: Synchronized Method (Fixes Race Condition, Full-Method Lock)
        System.out.println(YELLOW + "\n[TEST 2] Running SYNCHRONIZED METHOD Checkout (Lab 5 Fix):" + RESET);
        long syncMethodTime = runSimulation(SyncMode.SYNC_METHOD, initialStock, concurrentBuyers);

        // 3. Lab 6: Synchronized Block (Fine-Grained Locking + Parallel Payment Check)
        System.out.println(YELLOW + "\n[TEST 3] Running SYNCHRONIZED BLOCK Checkout (Lab 6 Optimization):" + RESET);
        long syncBlockTime = runSimulation(SyncMode.SYNC_BLOCK, initialStock, concurrentBuyers);

        // Final Comparison Table for Viva Walkthrough
        System.out.println(CYAN + "\n=====================================================================" + RESET);
        System.out.println(CYAN + "   LAB 5 vs LAB 6 PERFORMANCE & CONSISTENCY SUMMARY                  " + RESET);
        System.out.println(CYAN + "=====================================================================" + RESET);
        System.out.printf("  1. UNSAFE Mode             : %4d ms (Fast, but causes OVERSELLING / Negative Stock!)%n",
                unsafeTime);
        System.out.printf("  2. Synchronized Method     : %4d ms (Thread-safe, but serializes payment verification)%n",
                syncMethodTime);
        System.out.printf("  3. Synchronized Block      : %4d ms (Thread-safe AND ~3x faster via fine-grained locks)%n",
                syncBlockTime);
    }

    private static long runSimulation(SyncMode mode, int initialStock, int buyerCount) throws InterruptedException {
        Product gpuDeal = new Product("SP-102", "RTX 4070 Super GPU", "Graphics", 52999.00, initialStock);
        FlashSaleCheckoutEngine engine = new FlashSaleCheckoutEngine(gpuDeal);

        List<Thread> buyers = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= buyerCount; i++) {
            final String customerId = String.format("CUST-%02d", i);
            Thread buyerThread = new Thread(() -> {
                switch (mode) {
                    case UNSAFE -> engine.checkoutUnsafe(customerId, 1);
                    case SYNC_METHOD -> engine.checkoutSynchronizedMethod(customerId, 1);
                    case SYNC_BLOCK -> engine.checkoutSynchronizedBlock(customerId, 1);
                }
            }, "Buyer-" + i);
            buyers.add(buyerThread);
            buyerThread.start();
        }

        for (Thread t : buyers) {
            t.join();
        }

        long elapsed = System.currentTimeMillis() - startTime;

        boolean consistent = (engine.getUnitsSold() == initialStock) && (engine.getRemainingStock() == 0);
        String color = consistent ? GREEN : RED;

        System.out.printf(
                "%s  -> RESULT [%s]: Initial Stock: %d | Units Sold: %d | Final Stock: %d | Traffic Logged: %d | Time: %d ms%s%n",
                color,
                consistent ? "CONSISTENT" : "RACE CONDITION DETECTED!",
                initialStock,
                engine.getUnitsSold(),
                engine.getRemainingStock(),
                engine.getTotalTrafficAttempts(),
                elapsed,
                RESET);

        return elapsed;
    }
}