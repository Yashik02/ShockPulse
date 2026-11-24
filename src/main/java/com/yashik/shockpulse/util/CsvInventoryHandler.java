package com.yashik.shockpulse.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.yashik.shockpulse.model.Order;
import com.yashik.shockpulse.model.Product;

public class CsvInventoryHandler {

    // Lab 20: Read & validate Product records from a CSV file into an ArrayList
    public static List<Product> importProductsFromCsv(String filePath) throws IOException {
        List<Product> importedProducts = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || (lineNumber == 1 && line.toLowerCase().startsWith("product_id"))) {
                    continue; // Skip header or empty lines
                }

                String[] parts = line.split(",");
                if (parts.length != 5) {
                    System.err.printf("  [CSV WARNING] Skipping malformed row %d: %s%n", lineNumber, line);
                    continue;
                }

                try {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String category = parts[2].trim();
                    double price = Double.parseDouble(parts[3].trim());
                    int stock = Integer.parseInt(parts[4].trim());

                    importedProducts.add(new Product(id, name, category, price, stock));
                } catch (IllegalArgumentException e) {
                    System.err.printf("  [CSV WARNING] Invalid numeric value at row %d: %s%n", lineNumber,
                            e.getMessage());
                }
            }
        }
        return importedProducts;
    }

    // Lab 20: Export Order audit report to CSV using BufferedWriter +
    // try-with-resources
    public static void exportOrdersToCsv(List<Order> orders, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("order_id,customer_id,product_id,quantity,total_amount,status,timestamp");
            writer.newLine();

            for (Order o : orders) {
                writer.write(String.format("%s,%s,%s,%d,%.2f,%s,%s",
                        o.getOrderId(),
                        o.getCustomerId(),
                        o.getProductId(),
                        o.getQuantity(),
                        o.getTotalAmount(),
                        o.getStatus(),
                        o.getTimestamp()));
                writer.newLine();
            }
        }
    }
}