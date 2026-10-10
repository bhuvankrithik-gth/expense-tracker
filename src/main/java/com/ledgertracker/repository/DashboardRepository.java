package com.ledgertracker.repository;

import com.ledgertracker.dto.CategorySpendingDto;
import com.ledgertracker.dto.MonthlySummaryDto;
import com.ledgertracker.dto.MonthlyTrendDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MonthlySummaryDto getMonthlySummary(Long userId, int year, int month) {
        // Query the v_monthly_summary view or fallback aggregation
        String sql = "SELECT summary_year, summary_month, total_income, total_expense, net_savings, transaction_count " +
                "FROM v_monthly_summary " +
                "WHERE user_id = ? AND summary_year = ? AND summary_month = ?";

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new MonthlySummaryDto(
                    rs.getInt("summary_year"),
                    rs.getInt("summary_month"),
                    rs.getBigDecimal("total_income"),
                    rs.getBigDecimal("total_expense"),
                    rs.getBigDecimal("net_savings"),
                    rs.getLong("transaction_count")
            ), userId, year, month);
        } catch (EmptyResultDataAccessException e) {
            // No transactions for this month yet
            return new MonthlySummaryDto(year, month, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        }
    }

    public List<CategorySpendingDto> getCategorySpending(Long userId, int year, int month) {
        // Query view v_category_spending
        String sql = "SELECT category_id, category_name, category_color, category_icon, " +
                "total_spent, transaction_count, average_amount " +
                "FROM v_category_spending " +
                "WHERE user_id = ? AND spending_year = ? AND spending_month = ? " +
                "ORDER BY total_spent DESC";

        List<CategorySpendingDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            CategorySpendingDto dto = new CategorySpendingDto();
            dto.setCategoryId(rs.getLong("category_id"));
            dto.setCategoryName(rs.getString("category_name"));
            dto.setCategoryColor(rs.getString("category_color"));
            dto.setCategoryIcon(rs.getString("category_icon"));
            dto.setTotalSpent(rs.getBigDecimal("total_spent"));
            dto.setTransactionCount(rs.getLong("transaction_count"));
            dto.setAverageAmount(rs.getBigDecimal("average_amount"));
            return dto;
        }, userId, year, month);

        // Calculate percentage of total expense
        BigDecimal grandTotal = list.stream()
                .map(CategorySpendingDto::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            for (CategorySpendingDto dto : list) {
                BigDecimal pct = dto.getTotalSpent()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(grandTotal, 2, RoundingMode.HALF_UP);
                dto.setPercentageOfTotal(pct);
            }
        }

        return list;
    }

    public List<MonthlyTrendDto> getSixMonthTrends(Long userId, int currentYear, int currentMonth) {
        // Group by year and month for the last 6 months
        String sql = "SELECT " +
                "  YEAR(transaction_date) AS yr, " +
                "  MONTH(transaction_date) AS mo, " +
                "  COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income, " +
                "  COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense " +
                "FROM transactions " +
                "WHERE user_id = ? " +
                "  AND transaction_date >= DATE_SUB(DATE(CONCAT(?, '-', LPAD(?, 2, '0'), '-01')), INTERVAL 5 MONTH) " +
                "  AND transaction_date <= LAST_DAY(DATE(CONCAT(?, '-', LPAD(?, 2, '0'), '-01'))) " +
                "GROUP BY YEAR(transaction_date), MONTH(transaction_date) " +
                "ORDER BY yr ASC, mo ASC";

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                int yr = rs.getInt("yr");
                int mo = rs.getInt("mo");
                BigDecimal inc = rs.getBigDecimal("income");
                BigDecimal exp = rs.getBigDecimal("expense");
                BigDecimal net = inc.subtract(exp);
                String label = Month.of(mo).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + yr;
                return new MonthlyTrendDto(yr, mo, label, inc, exp, net);
            }, userId, currentYear, currentMonth, currentYear, currentMonth);
        } catch (Exception e) {
            // Fallback: simplified cross-db query
            return getFallbackSixMonthTrends(userId, currentYear, currentMonth);
        }
    }

    private List<MonthlyTrendDto> getFallbackSixMonthTrends(Long userId, int currentYear, int currentMonth) {
        String sql = "SELECT " +
                "  YEAR(transaction_date) AS yr, " +
                "  MONTH(transaction_date) AS mo, " +
                "  COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income, " +
                "  COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense " +
                "FROM transactions " +
                "WHERE user_id = ? " +
                "GROUP BY YEAR(transaction_date), MONTH(transaction_date) " +
                "ORDER BY yr DESC, mo DESC " +
                "LIMIT 6";

        List<MonthlyTrendDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            int yr = rs.getInt("yr");
            int mo = rs.getInt("mo");
            BigDecimal inc = rs.getBigDecimal("income");
            BigDecimal exp = rs.getBigDecimal("expense");
            BigDecimal net = inc.subtract(exp);
            String label = Month.of(mo).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + yr;
            return new MonthlyTrendDto(yr, mo, label, inc, exp, net);
        }, userId);

        // Reverse so chronological order (oldest to newest)
        List<MonthlyTrendDto> reversed = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            reversed.add(list.get(i));
        }
        return reversed;
    }
}
