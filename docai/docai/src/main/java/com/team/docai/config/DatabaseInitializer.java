package com.team.docai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 数据库初始化器 - 应用启动时确保表结构正确
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 确保 users 表存在
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  username VARCHAR(50) NOT NULL UNIQUE," +
                "  password VARCHAR(200) NOT NULL," +
                "  nickname VARCHAR(100)," +
                "  email VARCHAR(200)," +
                "  avatar VARCHAR(500)," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  INDEX idx_username (username)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 确保 documents 表存在
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS documents (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id BIGINT DEFAULT 1," +
                "  title VARCHAR(500) NOT NULL," +
                "  file_type VARCHAR(20) NOT NULL," +
                "  file_path VARCHAR(500) NOT NULL," +
                "  file_size BIGINT DEFAULT 0," +
                "  content_text LONGTEXT," +
                "  raw_text LONGTEXT," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  INDEX idx_user_id (user_id)," +
                "  INDEX idx_file_type (file_type)," +
                "  INDEX idx_created_at (created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 为已有表补充 raw_text 列（唯一可能缺失的列）
            safeAddColumn(stmt, "documents", "raw_text", "LONGTEXT AFTER content_text");

            log.info("数据库表结构检查完成");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
    }

    private void safeAddColumn(Statement stmt, String table, String column, String definition) {
        try {
            stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("表 {} 添加列 {} 成功", table, column);
        } catch (Exception e) {
            // 列已存在，忽略
            if (!e.getMessage().contains("Duplicate column")) {
                log.warn("表 {} 添加列 {} 失败: {}", table, column, e.getMessage());
            }
        }
    }
}
