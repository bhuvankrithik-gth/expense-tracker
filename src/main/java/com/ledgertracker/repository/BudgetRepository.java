package com.ledgertracker.repository;

import com.ledgertracker.dto.BudgetProgressDto;
import com.ledgertracker.model.Budget;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class BudgetRepository {

    private final JdbcTemplate jdbcTemplate;

    public BudgetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Budget> budgetRowMapper = (rs, rowNum) -> {
        Budget b = new Budget();
        b.setId(rs.getLong("id"));
        b.setUserId(rs.getLong("user_id"));
        b.setCategoryId(rs.getLong("category_id"));
        b.setCategoryName(rs.getString("category_name"));
        b.setCategoryColor(rs.getString("category_color"));
        b.setCategoryIcon(rs.getString("category_icon"));
        b.setMonth(rs.getInt("month"));
        b.setYear(rs.getInt("year"));
        b.setAmount(rs.getBigDecimal("amount"));
        b.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        b.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return b;
    };

    private final RowMapper<BudgetProgressDto> progressRowMapper = (rs, rowNum) -> {
        BudgetProgressDto dto = new BudgetProgressDto();
        dto.setBudgetId(rs.getLong("budget_id"));
        dto.setCategoryId(rs.getLong("category_id"));
        dto.setCategoryName(rs.getString("category_name"));
        dto.setCategoryColor(rs.getString("category_color"));
        dto.setCategoryIcon(rs.getString("category_icon"));

        BigDecimal budgetAmount = rs.getBigDecimal("budget_amount");
        BigDecimal actualSpent = rs.getBigDecimal("actual_spent");
        if (actualSpent == null) actualSpent = BigDecimal.ZERO;

        BigDecimal remainingAmount = budgetAmount.subtract(actualSpent);
        BigDecimal percent = BigDecimal.ZERO;
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            percent = actualSpent.multiply(BigDecimal.valueOf(100)).divide(budgetAmount, 2, RoundingMode.HALF_UP);
        }

        String status = "SAFE";
        if (actualSpent.compareTo(budgetAmount) > 0) {
            status = "EXCEEDED";
        } else if (actualSpent.compareTo(budgetAmount.multiply(BigDecimal.valueOf(0.85))) >= 0) {
            status = "WARNING";
        }

        dto.setBudgetAmount(budgetAmount);
        dto.setActualSpent(actualSpent);
        dto.setRemainingAmount(remainingAmount);
        dto.setPercentageUsed(percent);
        dto.setBudgetStatus(status);
        return dto;
    };

    public List<BudgetProgressDto> findBudgetsWithProgress(Long userId, int year, int month) {
        // Query joining budgets, categories, and an aggregated subquery of actual spending
        String sql = "SELECT " +
                "  b.id AS budget_id, " +
                "  b.category_id, " +
                "  c.name AS category_name, " +
                "  c.color AS category_color, " +
                "  c.icon AS category_icon, " +
                "  b.amount AS budget_amount, " +
                "  COALESCE(spent.total_spent, 0) AS actual_spent " +
                "FROM budgets b " +
                "JOIN categories c ON b.category_id = c.id " +
                "LEFT JOIN ( " +
                "  SELECT category_id, SUM(amount) AS total_spent " +
                "  FROM transactions " +
                "  WHERE user_id = ? " +
                "    AND type = 'EXPENSE' " +
                "    AND YEAR(transaction_date) = ? " +
                "    AND MONTH(transaction_date) = ? " +
                "  GROUP BY category_id " +
                ") spent ON b.category_id = spent.category_id " +
                "WHERE b.user_id = ? " +
                "  AND b.year = ? " +
                "  AND b.month = ? " +
                "ORDER BY b.amount DESC";

        return jdbcTemplate.query(sql, progressRowMapper, userId, year, month, userId, year, month);
    }

    public Optional<Budget> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT b.id, b.user_id, b.category_id, c.name AS category_name, " +
                "c.color AS category_color, c.icon AS category_icon, b.month, b.year, b.amount, " +
                "b.created_at, b.updated_at " +
                "FROM budgets b " +
                "JOIN categories c ON b.category_id = c.id " +
                "WHERE b.id = ? AND b.user_id = ?";
        try {
            Budget budget = jdbcTemplate.queryForObject(sql, budgetRowMapper, id, userId);
            return Optional.ofNullable(budget);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Budget> findByUserCategoryAndPeriod(Long userId, Long categoryId, int month, int year) {
        String sql = "SELECT b.id, b.user_id, b.category_id, c.name AS category_name, " +
                "c.color AS category_color, c.icon AS category_icon, b.month, b.year, b.amount, " +
                "b.created_at, b.updated_at " +
                "FROM budgets b " +
                "JOIN categories c ON b.category_id = c.id " +
                "WHERE b.user_id = ? AND b.category_id = ? AND b.month = ? AND b.year = ?";
        try {
            Budget budget = jdbcTemplate.queryForObject(sql, budgetRowMapper, userId, categoryId, month, year);
            return Optional.ofNullable(budget);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Budget create(Budget budget) {
        String sql = "INSERT INTO budgets (user_id, category_id, month, year, amount, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, budget.getUserId());
            ps.setLong(2, budget.getCategoryId());
            ps.setInt(3, budget.getMonth());
            ps.setInt(4, budget.getYear());
            ps.setBigDecimal(5, budget.getAmount());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            budget.setId(key.longValue());
        }
        return budget;
    }

    public void update(Budget budget) {
        String sql = "UPDATE budgets SET amount = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? AND user_id = ?";
        jdbcTemplate.update(sql, budget.getAmount(), budget.getId(), budget.getUserId());
    }

    public void delete(Long id, Long userId) {
        String sql = "DELETE FROM budgets WHERE id = ? AND user_id = ?";
        jdbcTemplate.update(sql, id, userId);
    }
}
