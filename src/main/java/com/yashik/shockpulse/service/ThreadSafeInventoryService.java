package com.yashik.shockpulse.service;

import com.yashik.shockpulse.exception.DuplicateOrderException;
import com.yashik.shockpulse.exception.OutOfStockException;
import com.yashik.shockpulse.exception.ProductNotFoundException;
import com.yashik.shockpulse.model.Order;

public class ThreadSafeInventoryService {
    private final InventoryManager inventoryManager;
    private final Object checkoutLock = new Object();

    public ThreadSafeInventoryService(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    public Order processCheckout(String customerId, String productId, int quantity)
            throws ProductNotFoundException, OutOfStockException, DuplicateOrderException {
        // Simulate external payment validation outside the critical section
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Critical section protected via synchronized block
        synchronized (checkoutLock) {
            return inventoryManager.placeFlashOrder(customerId, productId, quantity);
        }
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}