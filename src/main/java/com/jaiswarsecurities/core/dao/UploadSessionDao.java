package com.jaiswarsecurities.core.dao;

import com.jaiswarsecurities.core.model.UploadSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class UploadSessionDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveUploadSession(UploadSession uploadSession) {
        String sql = "INSERT INTO upload_sessions (id, fileName, uploadSessionUri) VALUES (?, ?, ?)";
        jdbcTemplate.update(
                sql,
                uploadSession.getId(),
                uploadSession.getFileName(),
                uploadSession.getUploadSessionUri()
        );
    }

    public UploadSession getUploadSession(String fileName) {
        String sql = "SELECT id, fileName, uploadSessionUri FROM upload_sessions WHERE fileName = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{fileName}, (rs, rowNum) ->
                UploadSession.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .fileName(rs.getString("fileName"))
                        .uploadSessionUri(rs.getString("uploadSessionUri"))
                        .build());
    }

    public void deleteUploadSession(String fileName) {
        String sql = "DELETE FROM upload_sessions WHERE fileName = ?";
        jdbcTemplate.update(sql, fileName);
    }
}