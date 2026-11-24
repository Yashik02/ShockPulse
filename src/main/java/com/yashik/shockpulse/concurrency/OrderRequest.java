package com.yashik.shockpulse.concurrency;

public record OrderRequest(String customerId, String productId, int quantity) {
    @Override
    public String toString() {
        return String.format("Req[%s -> %d x %s]", customerId, quantity, productId);
    }
}