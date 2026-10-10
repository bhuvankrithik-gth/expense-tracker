-- ============================================================================
-- Ledgerly Expense Tracker - Database Schema (MySQL 8)
-- Normalized in 3NF with Constraints, Foreign Keys, Views, Procedure & Trigger
-- ============================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(191) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    google_sub VARCHAR(100) NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_auth_method CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL,
    color VARCHAR(10) NOT NULL DEFAULT '#1B4332',
    icon VARCHAR(50) NOT NULL DEFAULT 'tag',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_category_type CHECK (type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_category UNIQUE (user_id, name, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. TRANSACTIONS TABLE
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    type VARCHAR(10) NOT NULL,
    transaction_date DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL DEFAULT 'DEBIT_CARD',
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_tx_amount CHECK (amount > 0),
    CONSTRAINT chk_tx_type CHECK (type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT chk_tx_payment_method CHECK (payment_method IN ('CASH', 'DEBIT_CARD', 'CREDIT_CARD', 'BANK_TRANSFER', 'UPI', 'OTHER')),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. BUDGETS TABLE
CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    `month` TINYINT NOT NULL,
    `year` SMALLINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_budget_month CHECK (`month` BETWEEN 1 AND 12),
    CONSTRAINT chk_budget_year CHECK (`year` >= 2000),
    CONSTRAINT chk_budget_amount CHECK (amount > 0),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_budget_period UNIQUE (user_id, category_id, `month`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. AUDIT LOGS TABLE
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(10) NOT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. INDEXES FOR PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_tx_user_date ON transactions(user_id, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_user_category ON transactions(user_id, category_id);
CREATE INDEX IF NOT EXISTS idx_tx_user_type_date ON transactions(user_id, type, transaction_date);
CREATE INDEX IF NOT EXISTS idx_budgets_user_period ON budgets(user_id, `year`, `month`);
CREATE INDEX IF NOT EXISTS idx_categories_user ON categories(user_id, type);

-- 7. VIEW: v_monthly_summary
CREATE OR REPLACE VIEW v_monthly_summary AS
SELECT
    t.user_id,
    YEAR(t.transaction_date) AS summary_year,
    MONTH(t.transaction_date) AS summary_month,
    COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0) AS total_income,
    COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS total_expense,
    (COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0) -
     COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)) AS net_savings,
    COUNT(t.id) AS transaction_count
FROM transactions t
GROUP BY t.user_id, YEAR(t.transaction_date), MONTH(t.transaction_date);

-- 8. VIEW: v_category_spending
CREATE OR REPLACE VIEW v_category_spending AS
SELECT
    t.user_id,
    c.id AS category_id,
    c.name AS category_name,
    c.color AS category_color,
    c.icon AS category_icon,
    YEAR(t.transaction_date) AS spending_year,
    MONTH(t.transaction_date) AS spending_month,
    COALESCE(SUM(t.amount), 0) AS total_spent,
    COUNT(t.id) AS transaction_count,
    ROUND(AVG(t.amount), 2) AS average_amount
FROM transactions t
JOIN categories c ON t.category_id = c.id
WHERE t.type = 'EXPENSE'
GROUP BY t.user_id, c.id, c.name, c.color, c.icon, YEAR(t.transaction_date), MONTH(t.transaction_date);

-- 9. STORED PROCEDURE: sp_calculate_budget_usage
DROP PROCEDURE IF EXISTS sp_calculate_budget_usage;
DELIMITER //
CREATE PROCEDURE sp_calculate_budget_usage(
    IN p_user_id BIGINT,
    IN p_year INT,
    IN p_month INT
)
BEGIN
    SELECT
        b.id AS budget_id,
        b.category_id,
        c.name AS category_name,
        c.color AS category_color,
        c.icon AS category_icon,
        b.amount AS budget_amount,
        COALESCE(spent.total_spent, 0) AS actual_spent,
        (b.amount - COALESCE(spent.total_spent, 0)) AS remaining_amount,
        ROUND((COALESCE(spent.total_spent, 0) / b.amount) * 100, 2) AS percentage_used,
        CASE
            WHEN COALESCE(spent.total_spent, 0) > b.amount THEN 'EXCEEDED'
            WHEN COALESCE(spent.total_spent, 0) >= (b.amount * 0.85) THEN 'WARNING'
            ELSE 'SAFE'
        END AS budget_status
    FROM budgets b
    JOIN categories c ON b.category_id = c.id
    LEFT JOIN (
        SELECT
            category_id,
            SUM(amount) AS total_spent
        FROM transactions
        WHERE user_id = p_user_id
          AND type = 'EXPENSE'
          AND YEAR(transaction_date) = p_year
          AND MONTH(transaction_date) = p_month
        GROUP BY category_id
    ) spent ON b.category_id = spent.category_id
    WHERE b.user_id = p_user_id
      AND b.year = p_year
      AND b.month = p_month
    ORDER BY percentage_used DESC;
END //
DELIMITER ;

-- 10. TRIGGERS: Audit logging for transactions
DROP TRIGGER IF EXISTS trg_after_transaction_insert_audit;
DELIMITER //
CREATE TRIGGER trg_after_transaction_insert_audit
AFTER INSERT ON transactions
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, entity_type, entity_id, action, details)
    VALUES (
        NEW.user_id,
        'TRANSACTION',
        NEW.id,
        'INSERT',
        CONCAT('Type: ', NEW.type, ', Amount: ', NEW.amount, ', Date: ', NEW.transaction_date, ', Method: ', NEW.payment_method)
    );
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_after_transaction_delete_audit;
DELIMITER //
CREATE TRIGGER trg_after_transaction_delete_audit
AFTER DELETE ON transactions
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, entity_type, entity_id, action, details)
    VALUES (
        OLD.user_id,
        'TRANSACTION',
        OLD.id,
        'DELETE',
        CONCAT('Type: ', OLD.type, ', Amount: ', OLD.amount, ', Date: ', OLD.transaction_date)
    );
END //
DELIMITER ;
