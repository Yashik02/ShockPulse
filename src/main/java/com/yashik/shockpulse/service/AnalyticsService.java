package com.yashik.shockpulse.service;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.model.Order.OrderStatus;
import com.yashik.shockpulse.model.Product;

public class AnalyticsService {

    // Lab 17: Functional Interfaces (Predicate, Function, Consumer, Supplier) &
    // Method References
    public void demonstrateFunctionalInterfaces(List<Product> products) {
        System.out.println("\n  --- Lab 17: Functional Interfaces & Lambda Pipeline ---");

        // Supplier: Generates a unique audit report reference token
        Supplier<String> auditTokenSupplier = () -> "AUDIT-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Predicate: Filters low-stock flash deals (<= 3 units)
        Predicate<Product> isLowStock = p -> p.getStockQuantity() <= 3;

        // Function: Transforms Product into a formatted alert string
        Function<Product, String> alertFormatter = p -> String.format("%s (%s) -> Only %d units left @ Rs. %.2f",
                p.getName(), p.getProductId(), p.getStockQuantity(), p.getFlashPrice());

        // Consumer: Prints output using method reference
        Consumer<String> printer = System.out::println;

        System.out.println("  Generated Report Token (Supplier): " + auditTokenSupplier.get());
        System.out.println("  Low-Stock Alerts (Predicate + Function + Consumer):");
        products.stream()
                .filter(isLowStock)
                .map(alertFormatter)
                .forEach(printer);
    }

    // Lab 18: Intermediate & Terminal Stream Operations + Loop Comparison
    public void runStreamPipelineDemo(List<Product> products, List<Order> orders) {
        System.out.println("\n  --- Lab 18: Stream Intermediate & Terminal Operations ---");

        // Distinct categories sorted alphabetically
        List<String> categories = products.stream()
                .map(Product::getCategory)
                .distinct()
                .sorted()
                .toList();
        System.out.println("  Distinct Sorted Categories : " + categories);

        // Top 2 most expensive products (skip 0, limit 2)
        System.out.println("  Top 2 Highest-Priced Deals :");
        products.stream()
                .sorted(Comparator.comparingDouble(Product::getFlashPrice).reversed())
                .skip(0)
                .limit(2)
                .forEach(p -> System.out.println("    * " + p.getName() + " (Rs. " + p.getFlashPrice() + ")"));

        // Terminal checks: count(), min(), max(), findFirst(), anyMatch()
        long confirmedCount = orders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        boolean hasHighValueOrder = orders.stream().anyMatch(o -> o.getTotalAmount() > 50000);
        orders.stream()
                .max(Comparator.comparingDouble(Order::getTotalAmount))
                .ifPresent(maxOrder -> System.out.println("  Highest Single Order       : " + maxOrder.getOrderId()
                        + " (Rs. " + maxOrder.getTotalAmount() + ")"));
        orders.stream()
                .min(Comparator.comparingDouble(Order::getTotalAmount))
                .ifPresent(minOrder -> System.out.println("  Lowest Single Order        : " + minOrder.getOrderId()
                        + " (Rs. " + minOrder.getTotalAmount() + ")"));
        orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
                .findFirst()
                .ifPresent(first -> System.out.println("  First Confirmed Order      : " + first.getOrderId()));

        System.out.println("  Confirmed Orders Count     : " + confirmedCount);
        System.out.println("  Any Order > Rs. 50,000?    : " + hasHighValueOrder);

        // Lab 18: Total revenue via reduce() vs Conventional Loop
        double streamRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
                .map(Order::getTotalAmount)
                .reduce(0.0, Double::sum);

        double loopRevenue = 0.0;
        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.CONFIRMED) {
                loopRevenue += o.getTotalAmount();
            }
        }
        System.out.printf("  Total Revenue (Stream reduce vs For-Loop): Rs. %.2f == Rs. %.2f%n", streamRevenue,
                loopRevenue);
    }

    // Lab 19: Collectors Analytics (groupingBy, partitioningBy, joining,
    // summarizingDouble)
    public void generateCollectorsReport(List<Product> products, List<Order> orders) {
        System.out.println("\n  --- Lab 19: Collectors Post-Sale Analytics Report ---");

        // 1. Collectors.toSet() & Collectors.joining()
        Set<String> uniqueBuyers = orders.stream()
                .map(Order::getCustomerId)
                .collect(Collectors.toSet());
        String joinedNames = products.stream()
                .map(Product::getName)
                .collect(Collectors.joining(" | "));
        System.out.println("  Active Flash-Sale Catalog  : " + joinedNames);
        System.out.println("  Unique Participating Buyers: " + uniqueBuyers);

        // 2. Collectors.groupingBy() + counting()
        Map<String, Long> countByCategory = products.stream()
                .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));
        System.out.println("  Product Count by Category  : " + countByCategory);

        // 3. Collectors.partitioningBy() (Confirmed vs Failed Orders)
        Map<Boolean, List<Order>> partitionedOrders = orders.stream()
                .collect(Collectors.partitioningBy(o -> o.getStatus() == OrderStatus.CONFIRMED));
        System.out.println("  Partition -> Confirmed     : " + partitionedOrders.get(true).size() + " orders");
        System.out.println("  Partition -> Failed        : " + partitionedOrders.get(false).size() + " orders");

        // 4. Collectors.summarizingDouble()
        DoubleSummaryStatistics stats = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
                .collect(Collectors.summarizingDouble(Order::getTotalAmount));
        System.out.printf(
                "  Revenue Summary Stats      : Count=%d | Sum=Rs. %.2f | Avg=Rs. %.2f | Min=Rs. %.2f | Max=Rs. %.2f%n",
                stats.getCount(), stats.getSum(), stats.getAverage(),
                stats.getCount() > 0 ? stats.getMin() : 0.0,
                stats.getCount() > 0 ? stats.getMax() : 0.0);
    }
}