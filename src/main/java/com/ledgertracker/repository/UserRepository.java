package com.ledgertracker.repository;

import com.ledgertracker.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("auth_provider"),
            rs.getString("google_sub"),
            rs.getString("display_name"),
            rs.getString("avatar_url"),
            rs.getBoolean("email_verified"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
    );

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, email, password_hash, auth_provider, google_sub, display_name, " +
                "avatar_url, email_verified, created_at, updated_at " +
                "FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, auth_provider, google_sub, display_name, " +
                "avatar_url, email_verified, created_at, updated_at " +
                "FROM users WHERE LOWER(email) = LOWER(?)";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByGoogleSub(String googleSub) {
        String sql = "SELECT id, email, password_hash, auth_provider, google_sub, display_name, " +
                "avatar_url, email_verified, created_at, updated_at " +
                "FROM users WHERE google_sub = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, googleSub);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM users WHERE LOWER(email) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public User create(User user) {
        String sql = "INSERT INTO users (email, password_hash, auth_provider, google_sub, " +
                "display_name, avatar_url, email_verified, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail().toLowerCase());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getAuthProvider());
            ps.setString(4, user.getGoogleSub());
            ps.setString(5, user.getDisplayName());
            ps.setString(6, user.getAvatarUrl());
            ps.setBoolean(7, user.isEmailVerified());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }
        return user;
    }

    public void update(User user) {
        String sql = "UPDATE users SET display_name = ?, avatar_url = ?, password_hash = ?, " +
                "auth_provider = ?, google_sub = ?, email_verified = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";
        jdbcTemplate.update(sql,
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getPasswordHash(),
                user.getAuthProvider(),
                user.getGoogleSub(),
                user.isEmailVerified(),
                user.getId()
        );
    }

    public void linkGoogleAccount(Long userId, String googleSub, String avatarUrl) {
        String sql = "UPDATE users SET google_sub = ?, " +
                "avatar_url = COALESCE(avatar_url, ?), " +
                "auth_provider = CASE WHEN password_hash IS NOT NULL THEN 'BOTH' ELSE 'GOOGLE' END, " +
                "email_verified = TRUE, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";
        jdbcTemplate.update(sql, googleSub, avatarUrl, userId);
    }
}
