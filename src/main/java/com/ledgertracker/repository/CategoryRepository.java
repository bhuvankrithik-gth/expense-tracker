package com.ledgertracker.repository;

import com.ledgertracker.model.Category;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Category> categoryRowMapper = (rs, rowNum) -> new Category(
            rs.getLong("id"),
            rs.getObject("user_id") != null ? rs.getLong("user_id") : null,
            rs.getString("name"),
            rs.getString("type"),
            rs.getString("color"),
            rs.getString("icon"),
            rs.getBoolean("is_default"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
    );

    public List<Category> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, name, type, color, icon, is_default, created_at " +
                "FROM categories " +
                "WHERE user_id = ? OR (user_id IS NULL AND is_default = TRUE) " +
                "ORDER BY type DESC, name ASC";
        return jdbcTemplate.query(sql, categoryRowMapper, userId);
    }

    public List<Category> findByUserIdAndType(Long userId, String type) {
        String sql = "SELECT id, user_id, name, type, color, icon, is_default, created_at " +
                "FROM categories " +
                "WHERE (user_id = ? OR (user_id IS NULL AND is_default = TRUE)) AND type = ? " +
                "ORDER BY name ASC";
        return jdbcTemplate.query(sql, categoryRowMapper, userId, type);
    }

    public Optional<Category> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT id, user_id, name, type, color, icon, is_default, created_at " +
                "FROM categories " +
                "WHERE id = ? AND (user_id = ? OR user_id IS NULL)";
        try {
            Category category = jdbcTemplate.queryForObject(sql, categoryRowMapper, id, userId);
            return Optional.ofNullable(category);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByNameAndType(Long userId, String name, String type) {
        String sql = "SELECT COUNT(1) FROM categories " +
                "WHERE user_id = ? AND LOWER(name) = LOWER(?) AND type = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, name, type);
        return count != null && count > 0;
    }

    public Category create(Category category) {
        String sql = "INSERT INTO categories (user_id, name, type, color, icon, is_default, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (category.getUserId() != null) {
                ps.setLong(1, category.getUserId());
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setString(2, category.getName());
            ps.setString(3, category.getType());
            ps.setString(4, category.getColor());
            ps.setString(5, category.getIcon());
            ps.setBoolean(6, category.isDefault());
            return ps;
        }, keyHolder);

        Long generatedId = extractId(keyHolder);
        if (generatedId != null) {
            category.setId(generatedId);
        }
        return category;
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

    public void update(Category category) {
        String sql = "UPDATE categories SET name = ?, color = ?, icon = ? " +
                "WHERE id = ? AND user_id = ?";
        jdbcTemplate.update(sql,
                category.getName(),
                category.getColor(),
                category.getIcon(),
                category.getId(),
                category.getUserId()
        );
    }

    public boolean hasTransactions(Long categoryId) {
        String sql = "SELECT COUNT(1) FROM transactions WHERE category_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, categoryId);
        return count != null && count > 0;
    }

    public void delete(Long id, Long userId) {
        String sql = "DELETE FROM categories WHERE id = ? AND user_id = ?";
        jdbcTemplate.update(sql, id, userId);
    }

    public void seedDefaultCategoriesForUser(Long userId) {
        // Initial set of standard starter categories
        String[][] defaultCats = {
                {"Housing & Rent", "EXPENSE", "#1B4332", "home"},
                {"Groceries & Food", "EXPENSE", "#2A9D8F", "shopping-cart"},
                {"Utilities & Bills", "EXPENSE", "#E76F51", "zap"},
                {"Transit & Travel", "EXPENSE", "#F4A261", "truck"},
                {"Health & Medical", "EXPENSE", "#E63946", "heart"},
                {"Dining & Cafes", "EXPENSE", "#A23B72", "coffee"},
                {"Personal & Shopping", "EXPENSE", "#457B9D", "tag"},
                {"Salary", "INCOME", "#2D6A4F", "briefcase"},
                {"Freelance", "INCOME", "#40916C", "code"},
                {"Investments", "INCOME", "#52B788", "trending-up"}
        };

        String sql = "INSERT INTO categories (user_id, name, type, color, icon, is_default, created_at) " +
                "VALUES (?, ?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name)";

        for (String[] cat : defaultCats) {
            jdbcTemplate.update(sql, userId, cat[0], cat[1], cat[2], cat[3]);
        }
    }
}
