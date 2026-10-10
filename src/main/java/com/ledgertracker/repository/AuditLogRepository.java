package com.ledgertracker.repository;

import com.ledgertracker.model.AuditLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AuditLog> auditLogRowMapper = (rs, rowNum) -> new AuditLog(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("entity_type"),
            rs.getLong("entity_id"),
            rs.getString("action"),
            rs.getString("details"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
    );

    public List<AuditLog> findRecentByUserId(Long userId, int limit) {
        String sql = "SELECT id, user_id, entity_type, entity_id, action, details, created_at " +
                "FROM audit_logs " +
                "WHERE user_id = ? " +
                "ORDER BY created_at DESC, id DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, auditLogRowMapper, userId, limit);
    }
}
