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

-- Seed initial flash-sale catalog and customer wallets
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