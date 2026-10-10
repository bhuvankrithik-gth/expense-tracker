package com.ledgertracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Bean
    public ApplicationRunner databaseInitializationRunner() {
        return args -> {
            try {
                log.info("Checking database schema initialization status...");

                // 1. Create tables if not exist
                initTables();

                // 2. Create or replace views
                initViews();

                // 3. Create or replace stored procedures
                initStoredProcedures();

                // 4. Create or replace triggers
                initTriggers();

                // 5. Seed initial data if database is empty
                seedInitialData();

                log.info("Database schema verification and initialization completed successfully.");
            } catch (Exception e) {
                log.warn("Notice during database schema initialization: {}. Continuing application startup.", e.getMessage());
            }
        };
    }

    private void initTables() {
        // Users Table
        jdbcTemplate.execute("""
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
        """);

        // Categories Table
        jdbcTemplate.execute("""
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
        """);

        // Transactions Table
        jdbcTemplate.execute("""
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
                CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);

        // Budgets Table
        jdbcTemplate.execute("""
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
        """);

        // Audit Logs Table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS audit_logs (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                entity_type VARCHAR(30) NOT NULL,
                entity_id BIGINT NOT NULL,
                action VARCHAR(10) NOT NULL,
                details TEXT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);

        // Indexes
        try {
            jdbcTemplate.execute("CREATE INDEX idx_tx_user_date ON transactions(user_id, transaction_date DESC);");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("CREATE INDEX idx_tx_user_category ON transactions(user_id, category_id);");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("CREATE INDEX idx_budgets_user_period ON budgets(user_id, `year`, `month`);");
        } catch (Exception ignored) {}
    }

    private void initViews() {
        try {
            jdbcTemplate.execute("""
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
            """);

            jdbcTemplate.execute("""
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
            """);
        } catch (Exception e) {
            log.warn("Could not register views: {}", e.getMessage());
        }
    }

    private void initStoredProcedures() {
        try {
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS sp_calculate_budget_usage;");
            jdbcTemplate.execute("""
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
                END
            """);
        } catch (Exception e) {
            log.warn("Could not register stored procedure: {}", e.getMessage());
        }
    }

    private void initTriggers() {
        try {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_after_transaction_insert_audit;");
            jdbcTemplate.execute("""
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
                        CONCAT('Type: ', NEW.type, ', Amount: ', NEW.amount, ', Date: ', NEW.transaction_date)
                    );
                END
            """);

            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_after_transaction_delete_audit;");
            jdbcTemplate.execute("""
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
                END
            """);
        } catch (Exception e) {
            log.warn("Could not register triggers: {}", e.getMessage());
        }
    }

    private void seedInitialData() {
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users", Integer.class);
        if (userCount == null || userCount == 0) {
            log.info("Database is empty. Seeding initial accounts and sample ledger data...");

            // Seed user Elena Vance
            jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, auth_provider, google_sub, display_name, avatar_url, email_verified, created_at)
                VALUES (1, 'elena.vance@ledgerly.internal', '$2a$10$E1sHE3vqU/KZFgw2qMLpp.lF3yfx3P1IJTApwCl4LgD0SfmQJIJLO', 'LOCAL', NULL, 'Elena Vance', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&q=80', TRUE, '2026-05-01 09:00:00');
            """);

            // Seed categories
            jdbcTemplate.execute("""
                INSERT INTO categories (id, user_id, name, type, color, icon, is_default, created_at) VALUES
                (1, 1, 'Housing & Rent', 'EXPENSE', '#1B4332', 'home', TRUE, '2026-05-01 09:05:00'),
                (2, 1, 'Groceries & Provisions', 'EXPENSE', '#2A9D8F', 'shopping-cart', TRUE, '2026-05-01 09:05:00'),
                (3, 1, 'Utilities & Telecom', 'EXPENSE', '#E76F51', 'zap', TRUE, '2026-05-01 09:05:00'),
                (4, 1, 'Transit & Commute', 'EXPENSE', '#F4A261', 'truck', TRUE, '2026-05-01 09:05:00'),
                (5, 1, 'Healthcare & Wellness', 'EXPENSE', '#E63946', 'heart', TRUE, '2026-05-01 09:05:00'),
                (6, 1, 'Dining & Hospitality', 'EXPENSE', '#A23B72', 'coffee', TRUE, '2026-05-01 09:05:00'),
                (7, 1, 'Literature & Tech Books', 'EXPENSE', '#457B9D', 'book', FALSE, '2026-05-01 09:05:00'),
                (8, 1, 'Salary & Compensation', 'INCOME', '#2D6A4F', 'briefcase', TRUE, '2026-05-01 09:05:00'),
                (9, 1, 'Consulting Retainer', 'INCOME', '#40916C', 'code', FALSE, '2026-05-01 09:05:00'),
                (10, 1, 'Investment Dividends', 'INCOME', '#52B788', 'trending-up', TRUE, '2026-05-01 09:05:00');
            """);

            // Seed budgets for current month
            jdbcTemplate.execute("""
                INSERT INTO budgets (user_id, category_id, `month`, `year`, amount, created_at) VALUES
                (1, 1, 10, 2026, 1850.00, '2026-10-01 00:00:00'),
                (1, 2, 10, 2026, 650.00, '2026-10-01 00:00:00'),
                (1, 3, 10, 2026, 280.00, '2026-10-01 00:00:00'),
                (1, 4, 10, 2026, 200.00, '2026-10-01 00:00:00'),
                (1, 5, 10, 2026, 150.00, '2026-10-01 00:00:00'),
                (1, 6, 10, 2026, 300.00, '2026-10-01 00:00:00');
            """);

            // Seed transactions
            jdbcTemplate.execute("""
                INSERT INTO transactions (user_id, category_id, amount, type, transaction_date, payment_method, note, created_at) VALUES
                (1, 8, 5800.00, 'INCOME', '2026-10-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-10-01 10:00:00'),
                (1, 1, 1850.00, 'EXPENSE', '2026-10-01', 'BANK_TRANSFER', 'October Residential Lease - Apt 4B', '2026-10-01 11:30:00'),
                (1, 3, 142.50, 'EXPENSE', '2026-10-03', 'DEBIT_CARD', 'Municipal Electric & Clean Water Bill', '2026-10-03 14:15:00'),
                (1, 2, 168.45, 'EXPENSE', '2026-10-04', 'CREDIT_CARD', 'Artisanal Pantry, Fresh Greens & Staples', '2026-10-04 18:20:00'),
                (1, 4, 85.00, 'EXPENSE', '2026-10-05', 'DEBIT_CARD', 'Regional Transit Monthly Rail Card Pass', '2026-10-05 08:45:00'),
                (1, 6, 64.20, 'EXPENSE', '2026-10-06', 'CREDIT_CARD', 'Dinner at The Cellar Table with colleagues', '2026-10-06 20:10:00'),
                (1, 2, 94.80, 'EXPENSE', '2026-10-08', 'CREDIT_CARD', 'Local Farmers Market - Organic produce', '2026-10-08 11:00:00'),
                (1, 7, 72.00, 'EXPENSE', '2026-10-09', 'CREDIT_CARD', 'Database Internals Hardcover & ACM Sub', '2026-10-09 16:30:00'),
                (1, 10, 315.60, 'INCOME', '2026-10-10', 'BANK_TRANSFER', 'Vanguard S&P Index Quarterly Dividend', '2026-10-10 09:15:00'),
                (1, 8, 5800.00, 'INCOME', '2026-09-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-09-01 10:00:00'),
                (1, 1, 1850.00, 'EXPENSE', '2026-09-01', 'BANK_TRANSFER', 'September Residential Lease - Apt 4B', '2026-09-01 11:30:00'),
                (1, 2, 592.10, 'EXPENSE', '2026-09-12', 'CREDIT_CARD', 'Monthly Groceries & Pantry Provisions', '2026-09-12 17:00:00'),
                (1, 3, 235.40, 'EXPENSE', '2026-09-15', 'DEBIT_CARD', 'Fiber Internet & Power Grid Services', '2026-09-15 14:00:00'),
                (1, 8, 5800.00, 'INCOME', '2026-08-01', 'BANK_TRANSFER', 'Monthly Enterprise Engineering Retainer', '2026-08-01 10:00:00'),
                (1, 1, 1850.00, 'EXPENSE', '2026-08-01', 'BANK_TRANSFER', 'August Residential Lease - Apt 4B', '2026-08-01 11:30:00'),
                (1, 2, 612.30, 'EXPENSE', '2026-08-14', 'CREDIT_CARD', 'Organic Groceries & Weekly Meals', '2026-08-14 18:00:00');
            """);
            log.info("Database seeded successfully with initial baseline data.");
        }
    }
}
