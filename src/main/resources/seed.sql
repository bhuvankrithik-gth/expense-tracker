-- ============================================================================
-- Ledgerly Expense Tracker - Realistic Production Seed Data
-- ============================================================================

-- 1. SEED USERS
-- Password for Elena Vance: "Ledger2026!" (BCrypt hashed: $2a$10$vI8A7C6JzQ3vU9R4o8T1ZeNlP5sJ2kH3mF7wO9yL2bV4cX6dA8eSa)
INSERT INTO users (id, email, password_hash, auth_provider, google_sub, display_name, avatar_url, email_verified, created_at)
VALUES 
(1, 'elena.vance@ledgerly.internal', '$2a$10$3sWn3i77J7n9O/s7R8gMfuM3b1P.Q8D6z3fL0rUqE7hFzG6sC1sve', 'LOCAL', NULL, 'Elena Vance', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&q=80', TRUE, '2026-05-01 09:00:00'),
(2, 'marcus.reed@ledgerly.internal', NULL, 'GOOGLE', '109283746192837465920', 'Marcus Reed', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=160&q=80', TRUE, '2026-06-15 14:30:00')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

-- 2. SEED CATEGORIES (Standard System Categories for Elena Vance and defaults)
INSERT INTO categories (id, user_id, name, type, color, icon, is_default, created_at)
VALUES
(1, 1, 'Housing & Rent', 'EXPENSE', '#1B4332', 'home', TRUE, '2026-05-01 09:05:00'),
(2, 1, 'Groceries & Provisions', 'EXPENSE', '#2A9D8F', 'shopping-cart', TRUE, '2026-05-01 09:05:00'),
(3, 1, 'Utilities & Telecom', 'EXPENSE', '#E76F51', 'zap', TRUE, '2026-05-01 09:05:00'),
(4, 1, 'Transit & Commute', 'EXPENSE', '#F4A261', 'truck', TRUE, '2026-05-01 09:05:00'),
(5, 1, 'Healthcare & Wellness', 'EXPENSE', '#E63946', 'heart', TRUE, '2026-05-01 09:05:00'),
(6, 1, 'Dining & Hospitality', 'EXPENSE', '#A23B72', 'coffee', TRUE, '2026-05-01 09:05:00'),
(7, 1, 'Literature & Tech Books', 'EXPENSE', '#457B9D', 'book', FALSE, '2026-05-01 09:05:00'),
(8, 1, 'Salary & Compensation', 'INCOME', '#2D6A4F', 'briefcase', TRUE, '2026-05-01 09:05:00'),
(9, 1, 'Consulting Retainer', 'INCOME', '#40916C', 'code', FALSE, '2026-05-01 09:05:00'),
(10, 1, 'Investment Dividends', 'INCOME', '#52B788', 'trending-up', TRUE, '2026-05-01 09:05:00')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 3. SEED BUDGETS FOR CURRENT PERIOD (October 2026)
INSERT INTO budgets (user_id, category_id, month, year, amount, created_at)
VALUES
(1, 1, 10, 2026, 1850.00, '2026-10-01 00:00:00'),
(1, 2, 10, 2026, 650.00, '2026-10-01 00:00:00'),
(1, 3, 10, 2026, 280.00, '2026-10-01 00:00:00'),
(1, 4, 10, 2026, 200.00, '2026-10-01 00:00:00'),
(1, 5, 10, 2026, 150.00, '2026-10-01 00:00:00'),
(1, 6, 10, 2026, 300.00, '2026-10-01 00:00:00')
ON DUPLICATE KEY UPDATE amount = VALUES(amount);

-- 4. SEED REALISTIC TRANSACTIONS (Multi-month ledger history)
INSERT INTO transactions (user_id, category_id, amount, type, transaction_date, payment_method, note, created_at)
VALUES
-- Current Month: October 2026
(1, 8, 5800.00, 'INCOME', '2026-10-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-10-01 10:00:00'),
(1, 1, 1850.00, 'EXPENSE', '2026-10-01', 'BANK_TRANSFER', 'October Residential Lease - Apt 4B', '2026-10-01 11:30:00'),
(1, 3, 142.50, 'EXPENSE', '2026-10-03', 'DEBIT_CARD', 'Municipal Electric & Clean Water Bill', '2026-10-03 14:15:00'),
(1, 2, 168.45, 'EXPENSE', '2026-10-04', 'CREDIT_CARD', 'Artisanal Pantry, Fresh Greens & Staples', '2026-10-04 18:20:00'),
(1, 4, 85.00, 'EXPENSE', '2026-10-05', 'DEBIT_CARD', 'Regional Transit Monthly Rail Card Pass', '2026-10-05 08:45:00'),
(1, 6, 64.20, 'EXPENSE', '2026-10-06', 'CREDIT_CARD', 'Dinner at The Cellar Table with colleagues', '2026-10-06 20:10:00'),
(1, 2, 94.80, 'EXPENSE', '2026-10-08', 'CREDIT_CARD', 'Local Farmers Market - Organic produce', '2026-10-08 11:00:00'),
(1, 7, 72.00, 'EXPENSE', '2026-10-09', 'CREDIT_CARD', 'Database Internals Hardcover & ACM Sub', '2026-10-09 16:30:00'),
(1, 10, 315.60, 'INCOME', '2026-10-10', 'BANK_TRANSFER', 'Vanguard S&P Index Quarterly Dividend', '2026-10-10 09:15:00'),

-- Previous Month: September 2026
(1, 8, 5800.00, 'INCOME', '2026-09-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-09-01 10:00:00'),
(1, 1, 1850.00, 'EXPENSE', '2026-09-01', 'BANK_TRANSFER', 'September Residential Lease - Apt 4B', '2026-09-01 11:30:00'),
(1, 2, 592.10, 'EXPENSE', '2026-09-12', 'CREDIT_CARD', 'Monthly Groceries & Pantry Provisions', '2026-09-12 17:00:00'),
(1, 3, 235.40, 'EXPENSE', '2026-09-15', 'DEBIT_CARD', 'Fiber Internet & Power Grid Services', '2026-09-15 14:00:00'),
(1, 6, 220.00, 'EXPENSE', '2026-09-20', 'CREDIT_CARD', 'Bistro Lumiere Birthday Dinner', '2026-09-20 21:00:00'),
(1, 9, 1450.00, 'INCOME', '2026-09-25', 'BANK_TRANSFER', 'Architecture Audit Consulting Stipend', '2026-09-25 15:00:00'),

-- August 2026
(1, 8, 5800.00, 'INCOME', '2026-08-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-08-01 10:00:00'),
(1, 1, 1850.00, 'EXPENSE', '2026-08-01', 'BANK_TRANSFER', 'August Residential Lease - Apt 4B', '2026-08-01 11:30:00'),
(1, 2, 612.30, 'EXPENSE', '2026-08-14', 'CREDIT_CARD', 'Organic Groceries & Weekly Meals', '2026-08-14 18:00:00'),
(1, 4, 180.00, 'EXPENSE', '2026-08-18', 'CREDIT_CARD', 'High-speed Rail Intercity Transit Pass', '2026-08-18 09:00:00'),

-- July 2026
(1, 8, 5800.00, 'INCOME', '2026-07-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-07-01 10:00:00'),
(1, 1, 1850.00, 'EXPENSE', '2026-07-01', 'BANK_TRANSFER', 'July Residential Lease - Apt 4B', '2026-07-01 11:30:00'),
(1, 5, 240.00, 'EXPENSE', '2026-07-16', 'DEBIT_CARD', 'Annual Preventative Health & Optical Care', '2026-07-16 11:00:00'),
(1, 2, 580.40, 'EXPENSE', '2026-07-22', 'CREDIT_CARD', 'Supermarket & Bulk Provisions', '2026-07-22 17:00:00'),

-- June 2026
(1, 8, 5800.00, 'INCOME', '2026-06-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-06-01 10:00:00'),
(1, 1, 1850.00, 'EXPENSE', '2026-06-01', 'BANK_TRANSFER', 'June Residential Lease - Apt 4B', '2026-06-01 11:30:00'),
(1, 2, 540.20, 'EXPENSE', '2026-06-18', 'CREDIT_CARD', 'Farmers Market & Bakery Essentials', '2026-06-18 16:30:00'),

-- May 2026
(1, 8, 5800.00, 'INCOME', '2026-05-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-05-01 10:00:00'),
(1, 1, 1850.00, 'EXPENSE', '2026-05-01', 'BANK_TRANSFER', 'May Residential Lease - Apt 4B', '2026-05-01 11:30:00'),
(1, 2, 520.00, 'EXPENSE', '2026-05-19', 'CREDIT_CARD', 'Groceries & Seasonal Produce', '2026-05-19 15:45:00');
