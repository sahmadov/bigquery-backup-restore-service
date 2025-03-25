-- 1. Create the first dataset (if not exists)
CREATE SCHEMA IF NOT EXISTS `target-bigquery-host.ecommerce_data`;

-- 2. Create the customers table
CREATE OR REPLACE TABLE `target-bigquery-host.ecommerce_data.customers` (
    customer_id INT64,
    first_name STRING,
    last_name STRING,
    email STRING,
    phone STRING,
    address STRING,
    city STRING,
    state STRING,
    zip_code STRING,
    registration_date DATE
);
INSERT INTO `target-bigquery-host.ecommerce_data.customers`
    (customer_id, first_name, last_name, email, phone, address, city, state, zip_code, registration_date)
VALUES
    (1001, 'John', 'Doe', 'john.doe@example.com', '555-123-4567', '123 Main St', 'Portland', 'OR', '97201', '2023-01-15'),
    (1002, 'Jane', 'Smith', 'jane.smith@example.com', '555-987-6543', '456 Oak Ave', 'Seattle', 'WA', '98101', '2023-02-20'),
    (1003, 'Robert', 'Johnson', 'robert.j@example.com', '555-222-3333', '789 Pine Blvd', 'San Francisco', 'CA', '94107', '2023-03-10'),
    (1004, 'Emily', 'Davis', 'emily.d@example.com', '555-444-5555', '321 Cedar Ln', 'Portland', 'OR', '97209', '2023-04-05');

-- 3. Create the products table
CREATE OR REPLACE TABLE `target-bigquery-host.ecommerce_data.products` (
    product_id INT64,
    product_name STRING,
    category STRING,
    price FLOAT64,
    cost FLOAT64,
    supplier STRING,
    stock_quantity INT64
);
INSERT INTO `target-bigquery-host.ecommerce_data.products`
    (product_id, product_name, category, price, cost, supplier, stock_quantity)
VALUES
    (101, 'Premium Laptop', 'Electronics', 1299.99, 950.00, 'TechSuppliers Inc.', 45),
    (102, 'Wireless Headphones', 'Electronics', 249.99, 120.00, 'AudioMasters LLC', 120),
    (103, 'Smartphone Model X', 'Electronics', 899.99, 600.00, 'MobileTech Corp', 78),
    (104, 'Smart Watch', 'Electronics', 349.99, 175.00, 'WearableTech Inc.', 65),
    (105, 'Bluetooth Speaker', 'Electronics', 79.99, 35.00, 'AudioMasters LLC', 150),
    (106, 'Ergonomic Keyboard', 'Computer Accessories', 129.99, 60.00, 'OfficeGear Ltd', 90);

-- 4. Create the orders table
CREATE OR REPLACE TABLE `target-bigquery-host.ecommerce_data.orders` (
    order_id INT64,
    customer_id INT64,
    order_date DATE,
    shipping_address STRING,
    shipping_city STRING,
    shipping_state STRING,
    shipping_zip STRING,
    total_amount FLOAT64,
    status STRING
);
INSERT INTO `target-bigquery-host.ecommerce_data.orders`
    (order_id, customer_id, order_date, shipping_address, shipping_city, shipping_state, shipping_zip, total_amount, status)
VALUES
    (10001, 1001, '2024-01-05', '123 Main St', 'Portland', 'OR', '97201', 1549.98, 'Delivered'),
    (10002, 1002, '2024-01-12', '456 Oak Ave', 'Seattle', 'WA', '98101', 929.98, 'Delivered'),
    (10003, 1003, '2024-01-20', '789 Pine Blvd', 'San Francisco', 'CA', '94107', 429.98, 'Delivered'),
    (10004, 1004, '2024-02-03', '321 Cedar Ln', 'Portland', 'OR', '97209', 1049.98, 'Delivered'),
    (10005, 1001, '2024-02-15', '123 Main St', 'Portland', 'OR', '97201', 349.99, 'Delivered'),
    (10006, 1002, '2024-03-01', '456 Oak Ave', 'Seattle', 'WA', '98101', 1379.98, 'Shipped'),
    (10007, 1003, '2024-03-08', '789 Pine Blvd', 'San Francisco', 'CA', '94107', 79.99, 'Processing');

-- 5. Create the order_items table
CREATE OR REPLACE TABLE `target-bigquery-host.ecommerce_data.order_items` (
    order_item_id INT64,
    order_id INT64,
    product_id INT64,
    quantity INT64,
    price_per_unit FLOAT64,
    line_total FLOAT64
);
INSERT INTO `target-bigquery-host.ecommerce_data.order_items`
    (order_item_id, order_id, product_id, quantity, price_per_unit, line_total)
VALUES
    (1, 10001, 101, 1, 1299.99, 1299.99),
    (2, 10001, 102, 1, 249.99, 249.99),
    (3, 10002, 103, 1, 899.99, 899.99),
    (4, 10002, 105, 1, 79.99, 79.99),
    (5, 10003, 102, 1, 249.99, 249.99),
    (6, 10003, 106, 1, 129.99, 129.99),
    (7, 10004, 103, 1, 899.99, 899.99),
    (8, 10004, 102, 1, 249.99, 249.99),
    (9, 10005, 104, 1, 349.99, 349.99),
    (10, 10006, 101, 1, 1299.99, 1299.99),
    (11, 10006, 105, 1, 79.99, 79.99),
    (12, 10007, 105, 1, 79.99, 79.99);

-- 6. Create sales tables that match wildcard filter 'sales_*' (monthly sales summaries)
CREATE OR REPLACE TABLE `target-bigquery-host.ecommerce_data.sales_2024_01` (
    product_id INT64,
    product_name STRING,
    units_sold INT64,
    revenue FLOAT64,
    cost FLOAT64,
    profit FLOAT64
);
INSERT INTO `target-bigquery-host.ecommerce_data.sales_2024_01`
    (product_id, product_name, units_sold, revenue, cost, profit)
VALUES
    (101, 'Premium Laptop', 5, 6499.95, 4750.00, 1749.95),
    (102, 'Wireless Headphones', 8, 1999.92, 960.00, 1039.92),
    (103, 'Smartphone Model X', 6, 5399.94, 3600.00, 1799.94),
    (104, 'Smart Watch', 3, 1049.97, 525.00, 524.97),
    (105, 'Bluetooth Speaker', 12, 959.88, 420.00, 539.88),
    (106, 'Ergonomic Keyboard', 7, 909.93, 420.00, 489.93);

CREATE OR REPLACE TABLE `target-bigquery-host.ecommerce_data.sales_2024_02` (
    product_id INT64,
    product_name STRING,
    units_sold INT64,
    revenue FLOAT64,
    cost FLOAT64,
    profit FLOAT64
);
INSERT INTO `target-bigquery-host.ecommerce_data.sales_2024_02`
    (product_id, product_name, units_sold, revenue, cost, profit)
VALUES
    (101, 'Premium Laptop', 7, 9099.93, 6650.00, 2449.93),
    (102, 'Wireless Headphones', 11, 2749.89, 1320.00, 1429.89),
    (103, 'Smartphone Model X', 8, 7199.92, 4800.00, 2399.92),
    (104, 'Smart Watch', 9, 3149.91, 1575.00, 1574.91),
    (105, 'Bluetooth Speaker', 15, 1199.85, 525.00, 674.85),
    (106, 'Ergonomic Keyboard', 10, 1299.90, 600.00, 699.90);

-- 5. Create a second dataset (if not exists)
CREATE SCHEMA IF NOT EXISTS `target-bigquery-host.analytics_dataset`;

-- 6. Create the product_metrics table in the second dataset
CREATE OR REPLACE TABLE `target-bigquery-host.analytics_dataset.product_metrics` (
    product_id INT64,
    product_name STRING,
    views INT64,
    conversions INT64,
    conversion_rate FLOAT64
);
INSERT INTO `target-bigquery-host.analytics_dataset.product_metrics`
    (product_id, product_name, views, conversions, conversion_rate)
VALUES
    (1001, 'Premium Laptop', 5420, 128, 2.36),
    (1002, 'Budget Smartphone', 8750, 312, 3.57),
    (1003, 'Wireless Headphones', 3210, 89, 2.77);

-- 7. Create the customer_segmentation table in the second dataset
CREATE OR REPLACE TABLE `target-bigquery-host.analytics_dataset.customer_segmentation` (
    segment_id INT64,
    segment_name STRING,
    customer_count INT64,
    avg_order_value FLOAT64,
    retention_rate FLOAT64
);
INSERT INTO `target-bigquery-host.analytics_dataset.customer_segmentation`
    (segment_id, segment_name, customer_count, avg_order_value, retention_rate)
VALUES
    (1, 'New Customers', 1245, 78.50, 32.5),
    (2, 'Repeat Customers', 842, 125.75, 68.3),
    (3, 'Premium Members', 326, 210.42, 89.7);

-- 8. Create a reporting table with monthly data
CREATE OR REPLACE TABLE `target-bigquery-host.analytics_dataset.monthly_revenue` (
    month DATE,
    total_revenue FLOAT64,
    total_orders INT64,
    avg_order_value FLOAT64
);
INSERT INTO `target-bigquery-host.analytics_dataset.monthly_revenue`
    (month, total_revenue, total_orders, avg_order_value)
VALUES
    ('2024-01-01', 125420.75, 1248, 100.50),
    ('2024-02-01', 142850.25, 1380, 103.52),
    ('2024-03-01', 158720.50, 1485, 106.88);

-- 9. Verify the tables
SELECT table_name
FROM `target-bigquery-host.ecommerce_data.INFORMATION_SCHEMA.TABLES`;

SELECT table_name
FROM `target-bigquery-host.analytics_dataset.INFORMATION_SCHEMA.TABLES`;

-- 10. Verify inserted data for ecommerce_data
SELECT * FROM `target-bigquery-host.ecommerce_data.customers`;
SELECT * FROM `target-bigquery-host.ecommerce_data.products`;
SELECT * FROM `target-bigquery-host.ecommerce_data.orders`;
SELECT * FROM `target-bigquery-host.ecommerce_data.order_items`;
SELECT * FROM `target-bigquery-host.ecommerce_data.sales_2024_01`;
SELECT * FROM `target-bigquery-host.ecommerce_data.sales_2024_02`;

-- 11. Verify inserted data for analytics_dataset
SELECT * FROM `target-bigquery-host.analytics_dataset.product_metrics`;
SELECT * FROM `target-bigquery-host.analytics_dataset.customer_segmentation`;
SELECT * FROM `target-bigquery-host.analytics_dataset.monthly_revenue`;