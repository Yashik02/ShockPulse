# ⚡ ShockPulse: Multi-Threaded Flash-Sale & Transactional Inventory Engine

> **Course:** Programming in Java (`25CSE0202`) — B.E. CSE (AI & ML), Chitkara University  
> **Tech Stack:** Java 21 LTS • Apache Maven • PostgreSQL (Supabase JDBC) • Java Concurrency API • Java Stream API • Java File I/O  

---

## 📖 Table of Contents
1. [What is ShockPulse? (Beginner Overview)](#1-what-is-shockpulse-beginner-overview)
2. [System Architecture & Dual-Tier Design](#2-system-architecture--dual-tier-design)
3. [Project Directory Structure](#3-project-directory-structure)
4. [Setup & Installation Guide](#4-setup--installation-guide)
5. [End-to-End Execution Flow (Options 1 to 8)](#5-end-to-end-execution-flow-options-1-to-8)
6. [How the Colored Terminal Works (ANSI Escape Codes)](#6-how-the-colored-terminal-works-ansi-escape-codes)
7. [Deep-Dive: File-by-File & Method-by-Method Breakdown](#7-deep-dive-file-by-file--method-by-method-breakdown)
   - [Package 1: `model` (Domain Entities)](#package-1-model-domain-entities)
   - [Package 2: `exception` (Custom Checked Exceptions)](#package-2-exception-custom-checked-exceptions)
   - [Package 3: `util` (Database Connection & CSV File I/O)](#package-3-util-database-connection--csv-file-io)
   - [Package 4: `dao` (JDBC Persistence & Generics)](#package-4-dao-jdbc-persistence--generics)
   - [Package 5: `service` (Business Logic & Stream Analytics)](#package-5-service-business-logic--stream-analytics)
   - [Package 6: `concurrency` (Multithreading, Locks & Thread Pools)](#package-6-concurrency-multithreading-locks--thread-pools)
   - [Entry Point: `Main.java` (Interactive CLI)](#entry-point-mainjava-interactive-cli)
8. [Course Handout (CHO) & Lab Mapping Matrix (Labs 1–20)](#8-course-handout-cho--lab-mapping-matrix-labs-120)

---

## 1. What is ShockPulse? (Beginner Overview)

Imagine an e-commerce flash sale (like a midnight launch of an RTX 4070 GPU) where **only 2 units are in stock**, but **hundreds of buyers click "Buy Now" at the exact same millisecond**.

If a backend system is written naively without thread synchronization or database transactions, two catastrophic bugs occur:
1. **Overselling (Race Condition):** Thread A and Thread B both check the stock at the same time, both see `stock = 2`, and both confirm orders—leaving the store with `-1` or `-2` inventory.
2. **Partial Database Corruption:** A customer's wallet money is deducted, or inventory stock is reduced, even though the final order record fails to save.

**ShockPulse** is a backend Java engine built to solve both problems using pure core Java APIs:
* **Concurrency Engine:** Coordinates multiple simultaneous buyer threads using a bounded Producer-Consumer queue (`wait()` / `notifyAll()`), fixed thread pools (`ExecutorService`), and fine-grained `synchronized` critical sections.
* **ACID Database Engine:** Connects to a live **Supabase PostgreSQL** database via **JDBC** using parameterized SQL (`PreparedStatement`) and manual transaction control (`commit()` / `rollback()`) so stock deduction, wallet payment, and order creation succeed or fail as a single atomic unit.

---

## 2. System Architecture & Dual-Tier Design

ShockPulse separates its operations into two clean tiers inside a single menu-driven CLI:

```text
+-----------------------------------------------------------------------------------+
|                        Main.java (Menu-Driven CLI Interface)                      |
+-----------------------------------------+-----------------------------------------+
|   TIER 1: PRODUCTION PERSISTENCE        |   TIER 2: ISOLATED CONCURRENCY SANDBOX  |
|   (Menu Options 1 through 6)            |   (Menu Option 7 - Auto-Resetting)      |
+-----------------------------------------+-----------------------------------------+
|                                         |                                         |
|  [CsvInventoryHandler]                  |  [OrderRequest Burst (12 Buyers)]       |
|         | (import CSV)                  |         |                               |
|         v                               |         v                               |
|  [ProductDAO] <---> [Supabase DB]       |  [Producer Thread Pool (3 Threads)]     |
|  [OrderDAO]   <---> (PostgreSQL)        |         |                               |
|         |                               |         v                               |
|         +---> [AnalyticsService]        |  [OrderBuffer (Bounded Capacity = 4)]   |
|         |     (Lambdas & Streams)       |         | (wait / notifyAll)            |
|         |                               |         v                               |
|         +---> [CsvInventoryHandler]     |  [CheckoutWorkerCallable Pool (3)]      |
|               (export audit CSV)        |         |                               |
|                                         |         v                               |
|  [GenericRepository<Product, String>]   |  [ThreadSafeInventoryService]           |
|   (In-memory typed cache)               |   (synchronized critical section)       |
|                                         |         |                               |
|                                         |         v                               |
|                                         |  [InventoryManager (HashMap/HashSet)]   |
+-----------------------------------------+-----------------------------------------+
```

### Why Separate Tier 1 and Tier 2?
* **Options 1–6** operate directly on your live **Supabase PostgreSQL** database, proving real-world persistence, SQL injection safety, ACID transactions, CSV bulk ingestion, and Stream API analytics.
* **Option 7** spins up a **fresh, self-resetting in-memory sandbox** (`InventoryManager`) every time it runs. Because flash-sale traffic simulations require controlled thread delays (`Thread.sleep`) and repeatable stock depletion tests during live demos, keeping Option 7 isolated ensures you can run the 12-buyer stress test repeatedly without corrupting your cloud database or hitting duplicate-order errors on re-runs.

---

## 3. Project Directory Structure

```text
shockpulse/
├── pom.xml                                      # Maven configuration (Java 21 + PostgreSQL JDBC driver)
├── .env                                         # Local Supabase credentials (ignored by Git)
├── .gitignore                                   # Excludes target/, .env, and IDE metadata
├── README.md                                    # Complete project documentation
└── src/main/
    ├── resources/
    │   └── restock_catalog.csv                  # Sample supplier CSV catalog for bulk import
    └── java/com/yashik/shockpulse/
        ├── Main.java                            # Interactive Menu-Driven CLI & orchestrator
        ├── model/
        │   ├── Product.java                     # Product entity implementing Comparable<Product>
        │   └── Order.java                       # Order entity with OrderStatus enum & LocalDateTime
        ├── exception/
        │   ├── OutOfStockException.java         # Custom checked exception for zero/low stock
        │   ├── DuplicateOrderException.java     # Custom checked exception for repeat flash claims
        │   └── ProductNotFoundException.java    # Custom checked exception for invalid product IDs
        ├── util/
        │   ├── DBConnectionManager.java         # JDBC DriverManager, DatabaseMetaData & SQLException logger
        │   └── CsvInventoryHandler.java         # BufferedReader / BufferedWriter CSV import & export
        ├── dao/
        │   ├── GenericRepository.java           # Generic type-safe cache Repository<T, ID>
        │   ├── ProductDAO.java                  # JDBC CRUD, Upsert & ResultSetMetaData inspection
        │   └── OrderDAO.java                    # Multi-step ACID transaction (commit / rollback)
        ├── service/
        │   ├── InventoryManager.java            # Collections engine (ArrayList, HashMap, HashSet, Comparator)
        │   ├── ThreadSafeInventoryService.java  # Synchronized wrapper around InventoryManager
        │   └── AnalyticsService.java            # Functional interfaces, Lambdas, Streams & Collectors
        └── concurrency/
            ├── OrderRequest.java                # Immutable Java 21 record representing a buy request
            ├── OrderBuffer.java                 # Bounded Producer-Consumer queue (wait / notifyAll)
            ├── CheckoutWorkerCallable.java      # Callable<Integer> consumer worker returning order count
            ├── CustomerRushTask.java            # Runnable buyer task demonstrating sleep() & interrupt()
            ├── FlashSaleAuditThread.java        # Subclass of Thread for periodic inventory snapshots
            ├── ThreadLifecycleMonitor.java      # Daemon thread logging getState() transitions
            └── FlashSaleCheckoutEngine.java     # Benchmark comparing UNSAFE vs SYNC_METHOD vs SYNC_BLOCK
```

---

## 4. Setup & Installation Guide

### Prerequisites
* **Java Development Kit (JDK) 21 LTS**
* **Apache Maven 3.8+**
* **Supabase Account** (Free tier PostgreSQL database)

### Step 1: Create Database Tables in Supabase
Open your **Supabase Dashboard -> SQL Editor** and execute:

```sql
CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    flash_price NUMERIC(10, 2) NOT NULL CHECK (flash_price >= 0),
    stock_quantity INT NOT NULL CHECK (stock_quantity >= 0)
);

CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    wallet_balance NUMERIC(10, 2) NOT NULL CHECK (wallet_balance >= 0)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(20) REFERENCES customers(customer_id),
    product_id VARCHAR(20) REFERENCES products(product_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO products (product_id, name, category, flash_price, stock_quantity) VALUES
    ('SP-101', 'Mechanical Keyboard', 'Peripherals', 2499.00, 5),
    ('SP-102', 'RTX 4070 Super GPU', 'Graphics', 52999.00, 2),
    ('SP-103', 'NVMe Gen4 SSD 1TB', 'Storage', 5499.00, 8)
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO customers (customer_id, name, wallet_balance) VALUES
    ('CUST-01', 'Yashik', 75000.00),
    ('CUST-02', 'Akshat', 10000.00),
    ('CUST-03', 'Keshav', 4000.00)
ON CONFLICT (customer_id) DO NOTHING;
```

### Step 2: Configure Environment Variables (`.env`)
In Supabase, click **Connect -> Session Pooler (Port 5432) -> JDBC** (Session Pooler is required because Transaction Pooler on port 6543 does not support server-side `PreparedStatement`s). Create a `.env` file in the root directory:

```env
SUPABASE_DB_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres.<YOUR_PROJECT_REF>
SUPABASE_DB_PASSWORD=<YOUR_DATABASE_PASSWORD>
```

### Step 3: Build & Run
```bash
mvn clean compile
```
Run `Main.java` via VS Code (with `.vscode/launch.json` pointing `"envFile": "${workspaceFolder}/.env"`).

---

## 5. End-to-End Execution Flow (Options 1 to 8)

When you launch `Main.java`, an infinite `while (running)` loop renders an interactive menu:

* **Option `1` — View Live Supabase Inventory & DB Metadata:** Opens a JDBC connection, prints PostgreSQL server/driver metadata (`DatabaseMetaData`), fetches all rows from `products` into an `ArrayList<Product>`, caches them in `GenericRepository<Product, String>`, and prints them.
* **Option `2` — Add Single Product to Supabase:** Prompts for product details via `Scanner`, validates inputs via the `Product` constructor, executes a parameterized `INSERT` via `ProductDAO.insertProduct()`, and updates the generic cache.
* **Option `3` — Bulk Import/Restock CSV into Supabase:** Reads `src/main/resources/restock_catalog.csv` line-by-line using `BufferedReader`, parses valid rows into `Product` objects, and runs an SQL `INSERT ... ON CONFLICT DO UPDATE` (Upsert) so existing items have their stock incremented safely.
* **Option `4` — Place Atomic Order in Supabase:** Executes a 4-step database transaction (`setAutoCommit(false)`). Locks the product row (`FOR UPDATE`), verifies stock, deducts stock, deducts the customer's wallet balance, and inserts an `orders` row. If *any* step fails (e.g., `CUST-03` has `Rs. 4,000` and tries to buy a `Rs. 52,999` GPU), `conn.rollback()` undoes everything.
* **Option `5` — Run Live Supabase Stream & Lambda Analytics:** Pulls live `products` and `orders` from Supabase and feeds them through `AnalyticsService` to generate functional-interface low-stock alerts, revenue reductions (`reduce()` vs `for`-loop), category groupings (`groupingBy`), order status partitions (`partitioningBy`), and statistical summaries (`summarizingDouble`).
* **Option `6` — Export Supabase Order Ledger to CSV:** Queries all live orders from Supabase and writes them to `supabase_sales_audit.csv` using `BufferedWriter` inside a `try-with-resources` block.
* **Option `7` — Run Multi-Threaded Flash-Sale Rush Demo:** Spins up a brand-new, isolated `InventoryManager` with 6 total items (`4` keyboards + `2` GPUs). Launches a 3-thread producer pool pushing **12 concurrent buyer requests** into a bounded `OrderBuffer` of capacity `4`, while a 3-thread `Callable<Integer>` consumer pool processes checkouts concurrently behind a `synchronized` lock. A `ScheduledExecutorService` closes the sale after `1.2 seconds`, and `Future.get()` aggregates exact confirmed order counts—proving **zero overselling**.
* **Option `8` — Exit:** Cleanly closes the `Scanner` and terminates the JVM.

---

## 6. How the Colored Terminal Works (ANSI Escape Codes)

Because this is a backend engine without a web frontend, terminal readability during live concurrency and transaction logs is critical. ShockPulse uses **ANSI Escape Sequences**—special character codes supported natively by modern terminals (VS Code Terminal, Windows Terminal, Linux/macOS bash):

```java
public static final String GREEN  = "\u001B[32m"; // Confirmed orders, DB commits, consistent states
public static final String RED    = "\u001B[31m"; // Exceptions, rollbacks, out-of-stock rejections
public static final String YELLOW = "\u001B[33m"; // Section headers, thread state traces, buffer waits
public static final String CYAN   = "\u001B[36m"; // Banners, producer enqueue logs, cache status
public static final String RESET  = "\u001B[0m";  // Resets terminal color back to default white/gray
```

### How it Works Under the Hood:
1. `\u001B` is the Unicode representation of the ASCII `ESC` character (`0x1B` or decimal `27`).
2. `[32m` instructs the terminal emulator's display driver to switch foreground text rendering to green.
3. Every colored string **must** end with `RESET` (`\u001B[0m`); otherwise, every subsequent line printed in the terminal would remain colored.

---

## 7. Deep-Dive: File-by-File & Method-by-Method Breakdown

### Package 1: `model` (Domain Entities)

#### 📄 `Product.java`
Represents a flash-sale item. Demonstrates **Encapsulation, Constructor Validation, and Natural Ordering (`CLO01` & `CLO02`)**.
* **Fields:** `private final String productId`, `private String name`, `private String category`, `private double flashPrice`, `private int stockQuantity`. Making `productId` `final` guarantees identity immutability once created.
* **`Product(...)` (Constructor):** Validates that `productId` is non-null/non-blank and that `flashPrice` and `stockQuantity` are $\ge 0$, throwing `IllegalArgumentException` (an unchecked runtime exception) immediately on bad input. Normalizes `productId` to uppercase.
* **`compareTo(Product other)`:** Implements `Comparable<Product>` using `Double.compare(this.flashPrice, other.flashPrice)` so any collection of products has a natural ascending price sort order.
* **`toString()`:** Uses `String.format(...)` for aligned, fixed-width CLI table rows.

#### 📄 `Order.java`
Represents a purchase attempt (either `CONFIRMED` or `FAILED`). Demonstrates **Enums, Encapsulation, and `java.time` API (`CLO01` & `CLO04`)**.
* **`public enum OrderStatus`:** Type-safe enumeration restricting order states strictly to `PENDING`, `CONFIRMED`, and `FAILED`.
* **Fields:** `orderId`, `customerId`, `productId`, `quantity`, `totalAmount`, `status`, and `timestamp` (`LocalDateTime`).
* **`Order(...)` (Constructor):** Validates `quantity > 0` and automatically stamps `this.timestamp = LocalDateTime.now()`.
* **`toString()`:** Formats the timestamp cleanly using `DateTimeFormatter.ofPattern("HH:mm:ss.SSS")` so millisecond-level concurrency ordering is visible in terminal logs.

---

### Package 2: `exception` (Custom Checked Exceptions)

All three classes extend `java.lang.Exception` (making them **checked exceptions** that the compiler forces callers to handle via `try-catch` or `throws`, fulfilling **Lectures 10–12 / `CLO01`**):
1. **`OutOfStockException.java`:** Thrown when requested quantity exceeds available inventory.
2. **`DuplicateOrderException.java`:** Thrown when a customer attempts to claim the same flash-sale product more than once.
3. **`ProductNotFoundException.java`:** Thrown when looking up, updating, or deleting a product ID that does not exist in the `HashMap`.

---

### Package 3: `util` (Database Connection & CSV File I/O)

#### 📄 `DBConnectionManager.java`
Manages PostgreSQL connectivity and diagnostics (**Labs 10, 15, & 16 / `CLO04`**).
* **`getConnection()`:** Reads `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, and `SUPABASE_DB_PASSWORD` via `System.getenv(...)` (preventing hardcoded credentials in source code) and returns a live `java.sql.Connection` from `DriverManager.getConnection(...)`.
* **`printDatabaseMetadata()` (Lab 15):** Calls `conn.getMetaData()` to retrieve a `DatabaseMetaData` object and prints the PostgreSQL engine name, server version, JDBC driver name/version, and connected database role.
* **`printSqlException(SQLException e)` (Lab 16):** Extracts and prints the standard 5-character ANSI SQLState code (`e.getSQLState()`), vendor error code (`e.getErrorCode()`), and error message.

#### 📄 `CsvInventoryHandler.java`
Integrates flat CSV files with Java Collections using `try-with-resources` (**Lab 20 / `CLO04`**).
* **`importProductsFromCsv(String filePath)`:** Opens a `BufferedReader` wrapping a `FileReader` inside `try (...)` so file handles close automatically even if an exception occurs. Skips the header row, splits lines by `,`, validates column count (`parts.length == 5`) and numeric parsing (`Double.parseDouble`, `Integer.parseInt`), and collects valid `Product` instances into an `ArrayList<Product>`.
* **`exportOrdersToCsv(List<Order> orders, String filePath)`:** Opens a `BufferedWriter` wrapping a `FileWriter` inside `try (...)`, writes a CSV header row, iterates through the `List<Order>`, and writes comma-separated fields using `writer.newLine()`.

---

### Package 4: `dao` (JDBC Persistence & Generics)

#### 📄 `GenericRepository.java`
Demonstrates **Introductory Generics (`Lab 20` / `CLO04`)** using two type parameters: `<T, ID>` (`T` = entity type, `ID` = primary key type).
* **`store(ID id, T entity)`:** Stores any entity type in a type-safe `HashMap<ID, T>` without needing casts.
* **`get(ID id)`:** Returns `Optional<T>` (`Optional.ofNullable(storage.get(id))`), avoiding `NullPointerException` when a key is missing.
* **`getAll()` & `count()`:** Returns a defensive `ArrayList<>(storage.values())` and current cache size.

#### 📄 `ProductDAO.java`
Data Access Object encapsulating all SQL operations on the `products` table (**Labs 11–15 / `CLO04`**).
* **`insertProduct(Product product)`:** Uses `PreparedStatement` with `?` parameter placeholders (`setString`, `setDouble`, `setInt`) to prevent SQL injection and executes `pstmt.executeUpdate()`.
* **`upsertProduct(Product product)`:** Executes PostgreSQL's `INSERT INTO products ... ON CONFLICT (product_id) DO UPDATE SET stock_quantity = products.stock_quantity + EXCLUDED.stock_quantity`. Allows CSV restock files to be imported repeatedly without duplicate primary key errors.
* **`getAllProducts()` (Lab 14):** Executes `SELECT ... FROM products` via `Statement.executeQuery(sql)`, iterates over the `ResultSet` cursor (`while (rs.next())`), maps each database row into a Java `Product` object using typed getters (`rs.getString`, `rs.getDouble`, `rs.getInt`), and returns a `List<Product>`.
* **`findProductById(String productId)`:** Parameterized `SELECT ... WHERE product_id = ?` lookup returning `Product` or `null`.
* **`updateStock(...)` & `deleteProduct(...)`:** Parameterized `UPDATE` and `DELETE` queries returning affected row counts.
* **`printProductsTableSchema()` (Lab 15):** Calls `rs.getMetaData()` to inspect `ResultSetMetaData`, printing total column count, column names, SQL data types, and source table names dynamically.

#### 📄 `OrderDAO.java`
Implements atomic multi-table order processing (**Lab 16 / `CLO04`**).
* **`placeOrderTransaction(String customerId, String productId, int quantity)`:**
  1. Calls `conn.setAutoCommit(false)` to begin a manual ACID transaction boundary.
  2. Executes `SELECT flash_price, stock_quantity FROM products WHERE product_id = ? FOR UPDATE` to acquire a row-level database lock and read current price/stock.
  3. Checks if `availableStock < quantity`; if so, throws `OutOfStockException`.
  4. Executes `UPDATE products SET stock_quantity = stock_quantity - ? ...` to deduct inventory.
  5. Executes `UPDATE customers SET wallet_balance = wallet_balance - ? WHERE customer_id = ? AND wallet_balance >= ?`. If `executeUpdate() == 0` (insufficient funds), throws a `SQLException` with SQLState `"23514"` (check constraint violation).
  6. Executes `INSERT INTO orders ...` to record the confirmed purchase.
  7. Calls `conn.commit()` only after all statements succeed.
  8. Inside the `catch` block, calls `conn.rollback()` to revert any partial stock or wallet deductions if any step fails, and resets `conn.setAutoCommit(true)` inside `finally`.

---

### Package 5: `service` (Business Logic & Stream Analytics)

#### 📄 `InventoryManager.java`
In-memory record manager demonstrating the **Java Collections Framework (`Lab 1` / `CLO02`)**.
* **Data Structures Used:**
  * `List<Product> productList = new ArrayList<>()` & `List<Order> orderHistory = new ArrayList<>()` — preserves insertion order and supports sorting.
  * `Map<String, Product> productMap = new HashMap<>()` — provides $O(1)$ average-time product lookup by ID.
  * `Set<String> uniqueProductNames = new HashSet<>()` & `Set<String> uniqueCustomerClaims = new HashSet<>()` — enforces $O(1)$ uniqueness checks so no duplicate product names are added and no customer buys the same flash deal twice.
* **Key Methods:**
  * `addProduct(Product)`: Validates uniqueness across `HashMap` and `HashSet`, then inserts into all collections.
  * `searchProductById(String)`: Performs $O(1)$ lookup in `productMap` or throws `ProductNotFoundException`.
  * `getProductsSortedByStockDesc()` & `getOrdersSortedByAmountDesc()`: Clones the list and sorts using custom lambda `Comparator`s (`Comparator.comparingDouble(Order::getTotalAmount).reversed()`).
  * `placeFlashOrder(...)`: Checks `uniqueCustomerClaims` (`DuplicateOrderException`), checks stock (`OutOfStockException`), deducts stock, records the claim in the `HashSet`, and logs a `CONFIRMED` or `FAILED` `Order` in `orderHistory`.

#### 📄 `ThreadSafeInventoryService.java`
Thread-safe service wrapper around `InventoryManager` (**Lab 6 / `CLO03`**).
* **`processCheckout(...)`:** Simulates external payment gateway latency (`Thread.sleep(40)`) *outside* any lock so threads execute payment checks in parallel, then wraps `inventoryManager.placeFlashOrder(...)` inside a `synchronized (checkoutLock)` block so stock deduction and `HashSet` updates are strictly mutually exclusive.

#### 📄 `AnalyticsService.java`
Post-sale reporting engine built with **Lambdas, Functional Interfaces, and Stream API (`Labs 17–19` / `CLO04`)**.
* **`demonstrateFunctionalInterfaces(List<Product>)` (Lab 17):** Uses all four core `java.util.function` interfaces:
  * `Supplier<String>` to generate unique audit report tokens.
  * `Predicate<Product>` to filter low-stock products (`stock <= 3`).
  * `Function<Product, String>` to map `Product` objects into formatted alert strings.
  * `Consumer<String>` using a method reference (`System.out::println`) to print alerts.
* **`runStreamPipelineDemo(List<Product>, List<Order>)` (Lab 18):** Chains intermediate operations (`map`, `distinct`, `sorted`, `skip`, `limit`, `filter`) and terminal operations (`toList`, `count`, `anyMatch`, `max`, `min`, `findFirst`, `reduce`), and verifies that `Stream.reduce(0.0, Double::sum)` matches a conventional `for`-each loop down to the decimal.
* **`generateCollectorsReport(List<Product>, List<Order>)` (Lab 19):** Uses `Collectors.toSet()` (unique buyers), `Collectors.joining(" | ")` (catalog banner), `Collectors.groupingBy(Product::getCategory, Collectors.counting())` (category breakdown), `Collectors.partitioningBy(...)` (splitting confirmed vs. failed orders), and `Collectors.summarizingDouble(Order::getTotalAmount)` (computing count, sum, average, min, and max order revenue in a single pass).

---

### Package 6: `concurrency` (Multithreading, Locks & Thread Pools)

#### 📄 `OrderRequest.java`
A Java 21 `record` (`public record OrderRequest(String customerId, String productId, int quantity)`) providing a concise, immutable data carrier between producer and consumer threads.

#### 📄 `OrderBuffer.java`
Bounded Producer-Consumer buffer using intrinsic monitor locks (**Lab 7 / `CLO03`**).
* **`produce(OrderRequest request)` (synchronized):** Uses a guarded `while (queue.size() == capacity && !saleClosed)` loop calling `wait()`. When the buffer has room, enqueues the request (`queue.offer(request)`) and calls `notifyAll()` to wake sleeping worker threads.
* **`consume()` (synchronized):** Uses `while (queue.isEmpty() && !saleClosed)` calling `wait()`. Once an item arrives, polls it (`queue.poll()`), calls `notifyAll()` to wake blocked producers, and returns it. When `saleClosed` is `true` and the queue is empty, returns `null` as a poison-pill signal so workers exit cleanly.
* **`closeSale()` (synchronized):** Sets `saleClosed = true` and calls `notifyAll()` so no thread ever hangs indefinitely in `WAITING`.

#### 📄 `CheckoutWorkerCallable.java`
Worker task implementing `Callable<Integer>` (**Labs 8 & 9 / `CLO03`**).
* **`call()`:** Continuously pulls `OrderRequest` items from `OrderBuffer.consume()` until receiving `null`. Submits each request to `ThreadSafeInventoryService.processCheckout(...)`, catches `OutOfStockException` / `DuplicateOrderException` cleanly without crashing the worker thread, increments a local `confirmedByThisWorker` counter, and returns the integer total to `Future.get()`.

#### 📄 `CustomerRushTask.java`, `FlashSaleAuditThread.java`, `ThreadLifecycleMonitor.java`, & `FlashSaleCheckoutEngine.java`
Supporting concurrency classes built during Phases 2 & 3 (**Labs 2–6 / `CLO03`**):
* **`CustomerRushTask` (`implements Runnable`):** Demonstrates `Thread.sleep()` and catching `InterruptedException` when `thread.interrupt()` cancels a slow buyer.
* **`FlashSaleAuditThread` (`extends Thread`):** Demonstrates thread creation via inheritance and periodic state snapshots.
* **`ThreadLifecycleMonitor` (`setDaemon(true)`):** Background daemon thread polling `thread.getState()` (`NEW`, `RUNNABLE`, `TIMED_WAITING`, `TERMINATED`) that terminates automatically when user threads finish.
* **`FlashSaleCheckoutEngine`:** Compares `UNSAFE` (race condition / negative stock), `SYNC_METHOD` (method-level lock bottleneck), and `SYNC_BLOCK` (fine-grained `stockLock` + `metricsLock`).

---

### Entry Point: `Main.java` (Interactive CLI)
Coordinates both tiers through clean helper methods (`viewSupabaseInventory`, `addProductToSupabase`, `importCsvToSupabase`, `executeSingleAcidCheckout`, `runSupabaseStreamAnalytics`, `exportSupabaseOrdersCsv`, and `runIsolatedFlashSaleDemo`).

---

## 8. Course Handout (CHO) & Lab Mapping Matrix (Labs 1–20)

| Lab / Syllabus Unit | Course Concept | Where It Is Implemented in ShockPulse |
| :--- | :--- | :--- |
| **Unit 1 (Lect 1–15)** | OOP, Encapsulation, Enums, String formatting, Custom Exceptions | `Product.java`, `Order.java`, `OutOfStockException`, `DuplicateOrderException`, `ProductNotFoundException` |
| **Lab 1 (`CLO02`)** | `ArrayList`, `HashSet`, `HashMap`, CRUD, Sorting via `Comparator` | `InventoryManager.java` (`productList`, `uniqueCustomerClaims`, `productMap`, `getOrdersSortedByAmountDesc`) |
| **Lab 2 (`CLO03`)** | Creating threads via `extends Thread` and `implements Runnable` | `FlashSaleAuditThread.java` (`extends Thread`) & `CustomerRushTask.java` (`implements Runnable`) |
| **Lab 3 (`CLO03`)** | Thread control using `sleep()`, `join()`, and `interrupt()` | `CustomerRushTask.java` (`sleep` + `InterruptedException`) & `Main.java` (`join` + `interrupt`) |
| **Lab 4 (`CLO03`)** | Thread lifecycle (`getState()`), `setDaemon(true)`, Priorities | `ThreadLifecycleMonitor.java` (`setDaemon(true)` polling `NEW`, `RUNNABLE`, `TIMED_WAITING`, `TERMINATED`) |
| **Lab 5 (`CLO03`)** | Race condition demonstration vs. `synchronized` method | `FlashSaleCheckoutEngine.java` (`checkoutUnsafe()` vs. `checkoutSynchronizedMethod()`) |
| **Lab 6 (`CLO03`)** | Fine-grained `synchronized` block & independent locks | `FlashSaleCheckoutEngine.java` (`stockLock`, `metricsLock`) & `ThreadSafeInventoryService.java` (`checkoutLock`) |
| **Lab 7 (`CLO03`)** | Bounded Producer-Consumer buffer with `wait()` & `notifyAll()` | `OrderBuffer.java` (`produce()`, `consume()`, `closeSale()`) |
| **Lab 8 (`CLO03`)** | `ExecutorService`, `Executors.newFixedThreadPool()`, `shutdown()`, `awaitTermination()` | `Main.java` (`runIsolatedFlashSaleDemo()` managing `producers` and `workers` thread pools) |
| **Lab 9 (`CLO03`)** | `Callable<Integer>`, `Future.get()`, `ScheduledExecutorService` | `CheckoutWorkerCallable.java` & `Main.java` (`saleScheduler.schedule(...)` + `Future.get()`) |
| **Labs 10–13 (`CLO04`)** | JDBC `DriverManager`, `Statement`, `PreparedStatement`, Menu-Driven CRUD | `DBConnectionManager.java`, `ProductDAO.java`, and `Main.java` CLI Menu |
| **Lab 14 (`CLO04`)** | `ResultSet` processing & mapping rows into Java objects (`ArrayList`) | `ProductDAO.getAllProducts()` & `OrderDAO.getAllOrders()` |
| **Lab 15 (`CLO04`)** | `DatabaseMetaData` & `ResultSetMetaData` inspection | `DBConnectionManager.printDatabaseMetadata()` & `ProductDAO.printProductsTableSchema()` |
| **Lab 16 (`CLO04`)** | ACID Transactions (`setAutoCommit(false)`, `commit()`, `rollback()`, `SQLException`) | `OrderDAO.placeOrderTransaction()` & `DBConnectionManager.printSqlException()` |
| **Lab 17 (`CLO04`)** | Lambdas, `Predicate`, `Function`, `Consumer`, `Supplier`, Method References | `AnalyticsService.demonstrateFunctionalInterfaces()` |
| **Lab 18 (`CLO04`)** | Stream intermediate & terminal ops (`filter`, `map`, `sorted`, `distinct`, `limit`, `skip`, `reduce`) | `AnalyticsService.runStreamPipelineDemo()` (including `reduce()` vs. `for`-loop comparison) |
| **Lab 19 (`CLO04`)** | `Collectors` (`toSet`, `joining`, `groupingBy`, `partitioningBy`, `counting`, `summarizingDouble`) | `AnalyticsService.generateCollectorsReport()` |
| **Lab 20 (`CLO04`)** | Generics (`Repository<T, ID>`) & CSV File I/O (`BufferedReader`/`BufferedWriter` + `try-with-resources`) | `GenericRepository.java` & `CsvInventoryHandler.java` |