package com.yashik.shockpulse;

import java.util.List;

import com.yashik.shockpulse.concurrency.CustomerRushTask;
import com.yashik.shockpulse.concurrency.FlashSaleAuditThread;
import com.yashik.shockpulse.concurrency.ThreadLifecycleMonitor;
import com.yashik.shockpulse.model.Product;
import com.yashik.shockpulse.service.InventoryManager;

public class Main {
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        System.out.println(CYAN + "================================================================" + RESET);
        System.out.println(CYAN + "   SHOCKPULSE: PHASE 2 - THREAD CONTROL & LIFECYCLE (LABS 2-4)  " + RESET);
        System.out.println(CYAN + "================================================================" + RESET);

        InventoryManager manager = new InventoryManager();
        manager.addProduct(new Product("SP-101", "Mechanical Keyboard", "Peripherals", 2499.00, 3));
        manager.addProduct(new Product("SP-102", "RTX 4070 Super GPU", "Graphics", 52999.00, 1));

        // Lab 2: Create tasks using Runnable (CustomerRushTask) and extending Thread
        // (FlashSaleAuditThread)
        Thread vipBuyer = new Thread(
                new CustomerRushTask(manager, "CUST-VIP", "SP-102", 1, 200),
                "Buyer-VIP");
        Thread regularBuyer = new Thread(
                new CustomerRushTask(manager, "CUST-REG", "SP-101", 2, 350),
                "Buyer-Regular");
        Thread slowBuyer = new Thread(
                new CustomerRushTask(manager, "CUST-SLOW", "SP-101", 1, 1500),
                "Buyer-Slow");

        FlashSaleAuditThread auditThread = new FlashSaleAuditThread(manager, 2, 250);

        // Lab 4: Set thread priorities
        vipBuyer.setPriority(Thread.MAX_PRIORITY); // Priority 10
        regularBuyer.setPriority(Thread.NORM_PRIORITY); // Priority 5
        slowBuyer.setPriority(Thread.MIN_PRIORITY); // Priority 1

        List<Thread> watchedThreads = List.of(vipBuyer, regularBuyer, slowBuyer, auditThread);

        // Lab 4: Observe NEW state before calling start()
        System.out.println(YELLOW + "\n[1] Observing Initial Thread States (Before start()):" + RESET);
        for (Thread t : watchedThreads) {
            System.out.printf("  Thread %-15s | State: %-14s | Priority: %d | Daemon: %b%n",
                    t.getName(), t.getState(), t.getPriority(), t.isDaemon());
        }

        // Lab 4: Start Daemon Lifecycle Monitor
        ThreadLifecycleMonitor daemonMonitor = new ThreadLifecycleMonitor(watchedThreads, 120);
        daemonMonitor.start();

        // Lab 2: Start threads concurrently using start()
        System.out.println(YELLOW + "\n[2] Starting Concurrent Threads..." + RESET);
        auditThread.start();
        vipBuyer.start();
        regularBuyer.start();
        slowBuyer.start();

        try {
            // Lab 3: Wait for VIP and Regular buyers to complete using join()
            vipBuyer.join();
            regularBuyer.join();

            // Lab 3: Interrupt the slow sleeping buyer if still active after 450ms
            if (slowBuyer.isAlive()) {
                System.out.println(YELLOW + "\n[3] Flash Sale Window Closing! Calling interrupt() on "
                        + slowBuyer.getName() + "..." + RESET);
                slowBuyer.interrupt();
            }

            // Ensure all remaining non-daemon threads complete
            slowBuyer.join();
            auditThread.join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Lab 4: Observe TERMINATED state after completion
        System.out.println(YELLOW + "\n[4] Observing Final Thread States (After join()):" + RESET);
        for (Thread t : watchedThreads) {
            System.out.printf("  Thread %-15s | State: %-14s%n", t.getName(), t.getState());
        }

        System.out.println(GREEN + "\n[5] Final Inventory & Order Ledger:" + RESET);
        manager.displayAllProducts();
        manager.getOrderHistory().forEach(o -> System.out.println("  " + o));
    }
}