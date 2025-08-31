-- Sample data for testing

-- Insert departments
INSERT INTO departments (id, name, manager_id) VALUES 
(1, 'Engineering', 1),
(2, 'Sales', 2),
(3, 'Marketing', 3);

-- Insert users
INSERT INTO users (id, username, email, full_name, status, created_date, last_login, department_id, email_verified) VALUES
(1, 'john.doe', 'john.doe@company.com', 'John Doe', 'ACTIVE', '2023-01-15', '2024-12-20', 1, true),
(2, 'jane.smith', 'jane.smith@company.com', 'Jane Smith', 'ACTIVE', '2023-02-20', '2024-12-19', 2, true),
(3, 'bob.johnson', 'bob.johnson@company.com', 'Bob Johnson', 'ACTIVE', '2023-03-10', '2024-12-18', 1, true),
(4, 'alice.williams', 'alice.williams@company.com', 'Alice Williams', 'INACTIVE', '2023-04-05', '2024-10-15', 3, false),
(5, 'charlie.brown', 'charlie.brown@company.com', 'Charlie Brown', 'ACTIVE', '2023-05-12', '2024-12-20', 2, true),
(6, 'diana.prince', 'diana.prince@company.com', 'Diana Prince', 'SUSPENDED', '2023-06-18', '2024-11-30', 1, true),
(7, 'edward.norton', 'edward.norton@company.com', 'Edward Norton', 'ACTIVE', '2023-07-22', '2024-12-15', 3, true),
(8, 'fiona.green', 'fiona.green@company.com', 'Fiona Green', 'PENDING', '2024-01-10', NULL, 2, false),
(9, 'george.white', 'george.white@company.com', 'George White', 'ACTIVE', '2024-02-15', '2024-12-19', 1, true),
(10, 'helen.black', 'helen.black@company.com', 'Helen Black', 'ACTIVE', '2024-03-20', '2024-12-20', 2, true);

-- Insert orders
INSERT INTO orders (user_id, order_number, amount, status, order_date) VALUES
(1, 'ORD-2023-001', 1500.00, 'COMPLETED', '2023-02-01'),
(1, 'ORD-2023-002', 2300.50, 'COMPLETED', '2023-03-15'),
(1, 'ORD-2023-003', 850.25, 'COMPLETED', '2023-05-20'),
(1, 'ORD-2024-001', 3200.00, 'COMPLETED', '2024-01-10'),
(1, 'ORD-2024-002', 1750.75, 'PENDING', '2024-11-25'),
(2, 'ORD-2023-004', 5500.00, 'COMPLETED', '2023-03-01'),
(2, 'ORD-2023-005', 3250.50, 'COMPLETED', '2023-06-10'),
(2, 'ORD-2024-003', 4100.00, 'COMPLETED', '2024-02-15'),
(3, 'ORD-2023-006', 950.00, 'COMPLETED', '2023-04-05'),
(3, 'ORD-2023-007', 1200.25, 'COMPLETED', '2023-07-20'),
(3, 'ORD-2024-004', 2800.50, 'COMPLETED', '2024-03-10'),
(5, 'ORD-2023-008', 750.00, 'COMPLETED', '2023-06-15'),
(5, 'ORD-2024-005', 1100.00, 'COMPLETED', '2024-04-20'),
(7, 'ORD-2023-009', 6200.00, 'COMPLETED', '2023-08-10'),
(7, 'ORD-2024-006', 4500.00, 'COMPLETED', '2024-05-15'),
(9, 'ORD-2024-007', 320.50, 'COMPLETED', '2024-03-25'),
(10, 'ORD-2024-008', 890.00, 'PROCESSING', '2024-12-15');

-- Insert categories
INSERT INTO categories (id, parent_id, name) VALUES
(1, NULL, 'Electronics'),
(2, NULL, 'Clothing'),
(3, 1, 'Computers'),
(4, 1, 'Phones'),
(5, 2, 'Men'),
(6, 2, 'Women');

-- Insert products
INSERT INTO products (category_id, name, price) VALUES
(3, 'Laptop Pro', 1299.99),
(3, 'Desktop PC', 899.99),
(4, 'Smartphone X', 799.99),
(4, 'Smartphone Y', 599.99),
(5, 'Mens Shirt', 49.99),
(5, 'Mens Jeans', 79.99),
(6, 'Womens Dress', 89.99),
(6, 'Womens Shoes', 69.99);