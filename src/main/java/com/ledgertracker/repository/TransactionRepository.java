package com.ledgertracker.repository;

import com.ledgertracker.dto.PagedResponse;
import com.ledgertracker.model.Transaction;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Transaction> transactionRowMapper = (rs, rowNum) -> {
        Transaction tx = new Transaction();
        tx.setId(rs.getLong("id"));
        tx.setUserId(rs.getLong("user_id"));
        tx.setCategoryId(rs.getLong("category_id"));
        tx.setCategoryName(rs.getString("category_name"));
        tx.setCategoryColor(rs.getString("category_color"));
        tx.setCategoryIcon(rs.getString("category_icon"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setType(rs.getString("type"));
        tx.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        tx.setPaymentMethod(rs.getString("payment_method"));
        tx.setNote(rs.getString("note"));
        tx.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        tx.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return tx;
    };

    public Transaction create(Transaction tx) {
        String sql = "INSERT INTO transactions (user_id, category_id, amount, type, transaction_date, " +
                "payment_method, note, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, tx.getUserId());
            ps.setLong(2, tx.getCategoryId());
            ps.setBigDecimal(3, tx.getAmount());
            ps.setString(4, tx.getType());
            ps.setDate(5, Date.valueOf(tx.getTransactionDate()));
            ps.setString(6, tx.getPaymentMethod());
            ps.setString(7, tx.getNote());
            return ps;
        }, keyHolder);

        Long generatedId = extractId(keyHolder);
        if (generatedId != null) {
            tx.setId(generatedId);
        }
        return tx;
    }

    private Long extractId(KeyHolder keyHolder) {
        if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
            java.util.Map<String, Object> keys = keyHolder.getKeys();
            for (String keyName : new String[]{"id", "ID", "id_"}) {
                if (keys.containsKey(keyName) && keys.get(keyName) instanceof Number n) {
                    return n.longValue();
                }
            }
            for (Object val : keys.values()) {
                if (val instanceof Number n) {
                    return n.longValue();
                }
            }
        }
        try {
            Number key = keyHolder.getKey();
            return key != null ? key.longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void update(Transaction tx) {
        String sql = "UPDATE transactions SET category_id = ?, amount = ?, type = ?, " +
                "transaction_date = ?, payment_method = ?, note = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? AND user_id = ?";
        jdbcTemplate.update(sql,
                tx.getCategoryId(),
                tx.getAmount(),
                tx.getType(),
                Date.valueOf(tx.getTransactionDate()),
                tx.getPaymentMethod(),
                tx.getNote(),
                tx.getId(),
                tx.getUserId()
        );
    }

    public void delete(Long id, Long userId) {
        String sql = "DELETE FROM transactions WHERE id = ? AND user_id = ?";
        jdbcTemplate.update(sql, id, userId);
    }

    public Optional<Transaction> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT t.id, t.user_id, t.category_id, c.name AS category_name, " +
                "c.color AS category_color, c.icon AS category_icon, t.amount, t.type, " +
                "t.transaction_date, t.payment_method, t.note, t.created_at, t.updated_at " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.id " +
                "WHERE t.id = ? AND t.user_id = ?";
        try {
            Transaction tx = jdbcTemplate.queryForObject(sql, transactionRowMapper, id, userId);
            return Optional.ofNullable(tx);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Transaction> findRecent(Long userId, int limit) {
        String sql = "SELECT t.id, t.user_id, t.category_id, c.name AS category_name, " +
                "c.color AS category_color, c.icon AS category_icon, t.amount, t.type, " +
                "t.transaction_date, t.payment_method, t.note, t.created_at, t.updated_at " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.id " +
                "WHERE t.user_id = ? " +
                "ORDER BY t.transaction_date DESC, t.id DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, transactionRowMapper, userId, limit);
    }

    public PagedResponse<Transaction> findWithFilters(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            String type,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        StringBuilder whereClause = new StringBuilder(" WHERE t.user_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (startDate != null) {
            whereClause.append(" AND t.transaction_date >= ? ");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            whereClause.append(" AND t.transaction_date <= ? ");
            params.add(Date.valueOf(endDate));
        }
        if (categoryId != null) {
            whereClause.append(" AND t.category_id = ? ");
            params.add(categoryId);
        }
        if (type != null && !type.isBlank()) {
            whereClause.append(" AND t.type = ? ");
            params.add(type);
        }
        if (minAmount != null) {
            whereClause.append(" AND t.amount >= ? ");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            whereClause.append(" AND t.amount <= ? ");
            params.add(maxAmount);
        }
        if (search != null && !search.isBlank()) {
            whereClause.append(" AND (LOWER(t.note) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(t.payment_method) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        // Count query
        String countSql = "SELECT COUNT(1) FROM transactions t " +
                "JOIN categories c ON t.category_id = c.id " + whereClause;
        Long totalElements = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long total = totalElements != null ? totalElements : 0L;

        // Whitelist safe order columns
        Set<String> allowedSort = Set.of("t.transaction_date", "t.amount", "c.name", "t.created_at", "t.payment_method");
        String safeSort = allowedSort.contains(sortBy) ? sortBy : "t.transaction_date";
        String safeDirection = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";

        // Query with pagination
        int offset = Math.max(0, page) * Math.max(1, size);
        String dataSql = "SELECT t.id, t.user_id, t.category_id, c.name AS category_name, " +
                "c.color AS category_color, c.icon AS category_icon, t.amount, t.type, " +
                "t.transaction_date, t.payment_method, t.note, t.created_at, t.updated_at " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.id " +
                whereClause +
                " ORDER BY " + safeSort + " " + safeDirection + ", t.id DESC " +
                " LIMIT ? OFFSET ?";

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(size);
        queryParams.add(offset);

        List<Transaction> content = jdbcTemplate.query(dataSql, transactionRowMapper, queryParams.toArray());

        return new PagedResponse<>(content, page, size, total);
    }

    public List<Transaction> findAllForExport(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            String type
    ) {
        StringBuilder whereClause = new StringBuilder(" WHERE t.user_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (startDate != null) {
            whereClause.append(" AND t.transaction_date >= ? ");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            whereClause.append(" AND t.transaction_date <= ? ");
            params.add(Date.valueOf(endDate));
        }
        if (categoryId != null) {
            whereClause.append(" AND t.category_id = ? ");
            params.add(categoryId);
        }
        if (type != null && !type.isBlank()) {
            whereClause.append(" AND t.type = ? ");
            params.add(type);
        }

        String sql = "SELECT t.id, t.user_id, t.category_id, c.name AS category_name, " +
                "c.color AS category_color, c.icon AS category_icon, t.amount, t.type, " +
                "t.transaction_date, t.payment_method, t.note, t.created_at, t.updated_at " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.id " +
                whereClause +
                " ORDER BY t.transaction_date DESC, t.id DESC";

        return jdbcTemplate.query(sql, transactionRowMapper, params.toArray());
    }
}
