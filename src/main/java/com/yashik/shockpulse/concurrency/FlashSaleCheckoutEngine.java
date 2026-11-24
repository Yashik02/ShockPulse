package com.yashik.shockpulse.concurrency;

import com.yashik.shockpulse.model.Product;

public class FlashSaleCheckoutEngine {

    public enum SyncMode {
        UNSAFE, // Lab 5: No synchronization (causes race condition / overselling)
        SYNC_METHOD, // Lab 5: Entire method synchronized (safe, but slow bottleneck)
        SYNC_BLOCK // Lab 6: Fine-grained synchronized blocks on independent locks (safe & fast)
    }

    private final Product flashProduct;

    // Independent shared state #1: Inventory & confirmed sales count
    private int unitsSold = 0;

    // Independent shared state #2: Total checkout requests & revenue metrics (Lab 6
    // second task)
    private int totalTrafficAttempts = 0;

    // Dedicated intrinsic lock objects for Lab 6 fine-grained synchronization
    private final Object stockLock = new Object();
    private final Object metricsLock = new Object();

    public FlashSaleCheckoutEngine(Product flashProduct) {
        this.flashProduct = flashProduct;
    }

    // =========================================================================
    // MODE 1: UNSAFE CHECKOUT (Lab 5 - Demonstrates Race Condition)
    // =========================================================================
    public boolean checkoutUnsafe(String customerId, int qty) {
        totalTrafficAttempts++; // Unsafe increment (can lose updates)
        simulatePaymentVerification();

        int currentStock = flashProduct.getStockQuantity();
        if (currentStock >= qty) {
            // Context switch window: multiple threads enter here simultaneously!
            simulateContextSwitchDelay();
            flashProduct.setStockQuantity(currentStock - qty);
            unitsSold += qty;
            System.out.printf("  \u001B[32m[UNSAFE-SUCCESS] %s bought %d unit(s) | Remaining Stock: %d\u001B[0m%n",
                    customerId, qty, flashProduct.getStockQuantity());
            return true;
        } else {
            System.out.printf("  \u001B[31m[UNSAFE-REJECT]  %s failed (Out of Stock | Current: %d)\u001B[0m%n",
                    customerId, flashProduct.getStockQuantity());
            return false;
        }
    }

    // =========================================================================
    // MODE 2: SYNCHRONIZED METHOD (Lab 5 - Fixes Race Condition, Locks Everything)
    // =========================================================================
    public synchronized boolean checkoutSynchronizedMethod(String customerId, int qty) {
        totalTrafficAttempts++;

        // Because the entire method is synchronized, threads wait in a single-file line
        // even during this slow payment gateway verification!
        simulatePaymentVerification();

        int currentStock = flashProduct.getStockQuantity();
        if (currentStock >= qty) {
            simulateContextSwitchDelay();
            flashProduct.setStockQuantity(currentStock - qty);
            unitsSold += qty;
            System.out.printf("  \u001B[32m[METHOD-SUCCESS] %s bought %d unit(s) | Remaining Stock: %d\u001B[0m%n",
                    customerId, qty, flashProduct.getStockQuantity());
            return true;
        } else {
            System.out.printf("  \u001B[31m[METHOD-REJECT]  %s failed (Out of Stock | Current: %d)\u001B[0m%n",
                    customerId, flashProduct.getStockQuantity());
            return false;
        }
    }

    // =========================================================================
    // MODE 3: SYNCHRONIZED BLOCK & MULTIPLE SHARED TASKS (Lab 6)
    // =========================================================================
    public boolean checkoutSynchronizedBlock(String customerId, int qty) {
        // Independent Operation 1: Protect only traffic counter using metricsLock
        synchronized (metricsLock) {
            totalTrafficAttempts++;
        }

        // Non-critical section: Payment gateway verification runs CONCURRENTLY across
        // all threads!
        simulatePaymentVerification();

        // Independent Operation 2: Protect ONLY the critical stock check-and-deduct
        // section
        synchronized (stockLock) {
            int currentStock = flashProduct.getStockQuantity();
            if (currentStock >= qty) {
                simulateContextSwitchDelay();
                flashProduct.setStockQuantity(currentStock - qty);
                unitsSold += qty;
                System.out.printf("  \u001B[32m[BLOCK-SUCCESS]  %s bought %d unit(s) | Remaining Stock: %d\u001B[0m%n",
                        customerId, qty, flashProduct.getStockQuantity());
                return true;
            } else {
                System.out.printf("  \u001B[31m[BLOCK-REJECT]   %s failed (Out of Stock | Current: %d)\u001B[0m%n",
                        customerId, flashProduct.getStockQuantity());
                return false;
            }
        }
    }

    private void simulatePaymentVerification() {
        try {
            Thread.sleep(60); // Simulates 60ms external bank/UPI verification
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateContextSwitchDelay() {
        try {
            Thread.sleep(15); // Forces thread interleaving during check-then-act
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getRemainingStock() {
        return flashProduct.getStockQuantity();
    }

    public int getUnitsSold() {
        return unitsSold;
    }

    public int getTotalTrafficAttempts() {
        return totalTrafficAttempts;
    }
}