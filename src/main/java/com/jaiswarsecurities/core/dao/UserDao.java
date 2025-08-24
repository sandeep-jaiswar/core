package com.jaiswarsecurities.core.dao;

import com.jaiswarsecurities.core.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class UserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insertUser(User user) {
        String sql = "INSERT INTO users (userId, username, email, password, firstName, lastName, phoneNumber, userStatus, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(
                sql,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getUserStatus().toString(), // Assuming UserStatus is an enum
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT userId, username, email, password, firstName, lastName, phoneNumber, userStatus, createdAt, updatedAt FROM users WHERE username = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) ->
                User.builder()
                        .userId(UUID.fromString(rs.getString("userId")))
                        .username(rs.getString("username"))
                        .email(rs.getString("email"))
                        .password(rs.getString("password"))
                        .firstName(rs.getString("firstName"))
                        .lastName(rs.getString("lastName"))
                        .phoneNumber(rs.getString("phoneNumber"))
                        .userStatus(rs.getString("userStatus")) // Assuming UserStatus is an enum
                        .createdAt(rs.getTimestamp("createdAt").toLocalDateTime())
                        .updatedAt(rs.getTimestamp("updatedAt").toLocalDateTime())
                        .build());
    }

        public User getUserByEmail(String email) {
        String sql = "SELECT userId, username, email, password, firstName, lastName, phoneNumber, userStatus, createdAt, updatedAt FROM users WHERE email = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{email}, (rs, rowNum) ->
                User.builder()
                        .userId(UUID.fromString(rs.getString("userId")))
                        .username(rs.getString("username"))
                        .email(rs.getString("email"))
                        .password(rs.getString("password"))
                        .firstName(rs.getString("firstName"))
                        .lastName(rs.getString("lastName"))
                        .phoneNumber(rs.getString("phoneNumber"))
                        .userStatus(rs.getString("userStatus")) // Assuming UserStatus is an enum
                        .createdAt(rs.getTimestamp("createdAt").toLocalDateTime())
                        .updatedAt(rs.getTimestamp("updatedAt").toLocalDateTime())
                        .build());
    }

    // Add other methods for update, delete, etc.
}
