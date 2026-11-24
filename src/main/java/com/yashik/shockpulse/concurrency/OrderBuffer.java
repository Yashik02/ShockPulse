package com.yashik.shockpulse.concurrency;

import java.util.LinkedList;
import java.util.Queue;

public class OrderBuffer {
    private final Queue<OrderRequest> queue = new LinkedList<>();
    private final int capacity;
    private boolean saleClosed = false;

    public OrderBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be positive.");
        }
        this.capacity = capacity;
    }

    // Lab 7: Producer adds order request; waits if buffer is full
    public synchronized boolean produce(OrderRequest request) throws InterruptedException {
        while (queue.size() == capacity && !saleClosed) {
            System.out.printf("  \u001B[33m[BUFFER FULL (%d/%d)] %s waiting to enqueue %s...\u001B[0m%n",
                    queue.size(), capacity, Thread.currentThread().getName(), request);
            wait();
        }

        if (saleClosed) {
            return false; // Reject new requests once flash sale ends
        }

        queue.offer(request);
        System.out.printf("  \u001B[36m[PRODUCED] %-16s | Buffer Size: %d/%d (by %s)\u001B[0m%n",
                request, queue.size(), capacity, Thread.currentThread().getName());

        // Wake up waiting worker threads
        notifyAll();
        return true;
    }

    // Lab 7: Consumer removes order request; waits if buffer is empty
    public synchronized OrderRequest consume() throws InterruptedException {
        while (queue.isEmpty() && !saleClosed) {
            wait();
        }

        // Drain remaining queued items even after saleClosed=true, then return null to
        // signal exit
        if (queue.isEmpty() && saleClosed) {
            return null;
        }

        OrderRequest request = queue.poll();
        System.out.printf("  \u001B[34m[CONSUMED] %-16s | Buffer Size: %d/%d (by %s)\u001B[0m%n",
                request, queue.size(), capacity, Thread.currentThread().getName());

        // Wake up waiting producer threads
        notifyAll();
        return request;
    }

    // Signals all producers & consumers that the flash-sale window has closed
    public synchronized void closeSale() {
        this.saleClosed = true;
        notifyAll();
    }

    public synchronized boolean isSaleClosed() {
        return saleClosed;
    }
}