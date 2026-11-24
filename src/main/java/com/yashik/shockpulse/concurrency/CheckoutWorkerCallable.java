package com.yashik.shockpulse.concurrency;

import java.util.concurrent.Callable;

import com.yashik.shockpulse.exception.DuplicateOrderException;
import com.yashik.shockpulse.exception.OutOfStockException;
import com.yashik.shockpulse.exception.ProductNotFoundException;
import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.service.ThreadSafeInventoryService;

public class CheckoutWorkerCallable implements Callable<Integer> {
    private final OrderBuffer buffer;
    private final ThreadSafeInventoryService inventoryService;

    public CheckoutWorkerCallable(OrderBuffer buffer, ThreadSafeInventoryService inventoryService) {
        this.buffer = buffer;
        this.inventoryService = inventoryService;
    }

    @Override
    public Integer call() throws Exception {
        int confirmedByThisWorker = 0;
        String workerName = Thread.currentThread().getName();

        while (true) {
            OrderRequest request = buffer.consume();
            if (request == null) {
                // Sale closed and buffer is completely drained
                break;
            }

            try {
                Order order = inventoryService.processCheckout(
                        request.customerId(),
                        request.productId(),
                        request.quantity());
                confirmedByThisWorker++;
                System.out.printf("  \u001B[32m[%s] CONFIRMED -> %s\u001B[0m%n", workerName, order);
            } catch (OutOfStockException | DuplicateOrderException | ProductNotFoundException e) {
                System.out.printf("  \u001B[31m[%s] REJECTED (%s) -> %s\u001B[0m%n",
                        workerName, e.getClass().getSimpleName(), e.getMessage());
            }
        }

        System.out.printf("  \u001B[35m[%s] Worker shutting down. Total confirmed: %d\u001B[0m%n",
                workerName, confirmedByThisWorker);
        return confirmedByThisWorker;
    }
}