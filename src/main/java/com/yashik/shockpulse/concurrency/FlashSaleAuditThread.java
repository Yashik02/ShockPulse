package com.yashik.shockpulse.concurrency;

import com.yashik.shockpulse.model.Product;
import com.yashik.shockpulse.service.InventoryManager;

public class FlashSaleAuditThread extends Thread {
    private final InventoryManager inventoryManager;
    private final int snapshots;
    private final long intervalMs;

    public FlashSaleAuditThread(InventoryManager inventoryManager, int snapshots, long intervalMs) {
        super("Audit-Thread");
        this.inventoryManager = inventoryManager;
        this.snapshots = snapshots;
        this.intervalMs = intervalMs;
    }

    @Override
    public void run() {
        for (int i = 1; i <= snapshots; i++) {
            try {
                Thread.sleep(intervalMs);
                System.out.printf("  \u001B[36m[%s] Snapshot #%d | Total Orders Processed So Far: %d\u001B[0m%n",
                        getName(), i, inventoryManager.getOrderHistory().size());
                for (Product p : inventoryManager.getProductsSortedByStockDesc()) {
                    System.out.printf("    -> %s has %d units left%n", p.getProductId(), p.getStockQuantity());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("  [%s] Audit interrupted early.%n", getName());
                break;
            }
        }
        System.out.printf("  \u001B[36m[%s] Completed all audit snapshots.\u001B[0m%n", getName());
    }
}