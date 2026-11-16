package com.yashik.shockpulse.concurrency;

import com.yashik.shockpulse.exception.DuplicateOrderException;
import com.yashik.shockpulse.exception.OutOfStockException;
import com.yashik.shockpulse.exception.ProductNotFoundException;
import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.service.InventoryManager;

public class CustomerRushTask implements Runnable {
    private final InventoryManager inventoryManager;
    private final String customerId;
    private final String productId;
    private final int quantity;
    private final long simulatedNetworkDelayMs;

    public CustomerRushTask(InventoryManager inventoryManager, String customerId, String productId, int quantity,
            long simulatedNetworkDelayMs) {
        this.inventoryManager = inventoryManager;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.simulatedNetworkDelayMs = simulatedNetworkDelayMs;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        int priority = Thread.currentThread().getPriority();
        System.out.printf("  [%s | Priority: %d] Customer %s connected, attempting to buy %d x %s...%n",
                threadName, priority, customerId, quantity, productId);

        try {
            // Lab 3: Use sleep() to introduce network/payment gateway delay
            Thread.sleep(simulatedNetworkDelayMs);

            Order order = inventoryManager.placeFlashOrder(customerId, productId, quantity);
            System.out.printf("  \u001B[32m[%s] CHECKOUT CONFIRMED -> %s\u001B[0m%n", threadName, order);

        } catch (InterruptedException e) {
            // Lab 3: Handle interrupt() on a sleeping thread cleanly
            Thread.currentThread().interrupt();
            System.out.printf("  \u001B[35m[%s] INTERRUPTED -> Customer %s checkout cancelled/timed out!\u001B[0m%n",
                    threadName, customerId);
        } catch (OutOfStockException | DuplicateOrderException | ProductNotFoundException e) {
            System.out.printf("  \u001B[31m[%s] REJECTED (%s) -> %s\u001B[0m%n",
                    threadName, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}