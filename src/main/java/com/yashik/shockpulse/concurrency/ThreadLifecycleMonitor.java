package com.yashik.shockpulse.concurrency;

import java.util.List;

public class ThreadLifecycleMonitor extends Thread {
    private final List<Thread> watchedThreads;
    private final long pollIntervalMs;

    public ThreadLifecycleMonitor(List<Thread> watchedThreads, long pollIntervalMs) {
        super("Daemon-Lifecycle-Monitor");
        this.watchedThreads = watchedThreads;
        this.pollIntervalMs = pollIntervalMs;
        // Lab 4 requirement: mark as Daemon thread so it terminates automatically when
        // user threads finish
        setDaemon(true);
    }

    @Override
    public void run() {
        System.out.printf("  \u001B[33m[%s | Daemon=%b] Started monitoring thread states...\u001B[0m%n",
                getName(), isDaemon());

        while (true) {
            try {
                Thread.sleep(pollIntervalMs);
                StringBuilder statusLine = new StringBuilder("  \u001B[33m[STATE TRACE] ");
                for (Thread t : watchedThreads) {
                    statusLine.append(String.format("%s=%s | ", t.getName(), t.getState()));
                }
                statusLine.append("\u001B[0m");
                System.out.println(statusLine);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}