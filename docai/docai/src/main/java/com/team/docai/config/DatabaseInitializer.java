package com.team.docai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 数据库初始化器 - 应用启动时自动确保表结构正确
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        log.info("开始检查数据库表结构...");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // ========== 确保 users 表存在且结构完整 ==========
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID'," +
                "  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名'," +
                "  password VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)'," +
                "  nickname VARCHAR(100) COMMENT '昵称'," +
                "  email VARCHAR(200) COMMENT '邮箱'," +
                "  avatar VARCHAR(500) COMMENT '头像URL'," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "  INDEX idx_username (username)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表'"
            );

            // 补丁：为已存在但缺少列的 users 表添加列
            safeAddColumn(stmt, "users", "nickname", "VARCHAR(100) COMMENT '昵称' AFTER password");
            safeAddColumn(stmt, "users", "email", "VARCHAR(200) COMMENT '邮箱' AFTER nickname");
            safeAddColumn(stmt, "users", "avatar", "VARCHAR(500) COMMENT '头像URL' AFTER email");
            safeAddColumn(stmt, "users", "created_at", "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
            safeAddColumn(stmt, "users", "updated_at", "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");

            // ========== 确保 documents 表存在且结构完整 ==========
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS documents (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文档ID'," +
                "  user_id BIGINT DEFAULT 1 COMMENT '用户ID'," +
                "  title VARCHAR(500) NOT NULL COMMENT '文档标题'," +
                "  file_type VARCHAR(20) NOT NULL COMMENT '文件类型'," +
                "  file_path VARCHAR(500) NOT NULL COMMENT '存储路径'," +
                "  file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)'," +
                "  content_text LONGTEXT COMMENT '解析后的文本内容'," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "  INDEX idx_user_id (user_id)," +
                "  INDEX idx_file_type (file_type)," +
                "  INDEX idx_created_at (created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表'"
            );

            safeAddColumn(stmt, "documents", "user_id", "BIGINT DEFAULT 1 COMMENT '用户ID' AFTER id");
            safeAddColumn(stmt, "documents", "content_text", "LONGTEXT COMMENT '解析后的文本内容' AFTER file_size");
            safeAddColumn(stmt, "documents", "raw_text", "LONGTEXT COMMENT '文档原始解析文本' AFTER content_text");

            log.info("数据库表结构检查完成");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
    }

    /**
     * 安全地添加列 - 如果列已存在则忽略
     */
    private void safeAddColumn(Statement stmt, String table, String column, String definition) {
        try {
            stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("表 {} 添加列 {} 成功", table, column);
        } catch (Exception e) {
            // 列已存在，忽略 (MySQL error code 1060: Duplicate column name)
            if (!e.getMessage().contains("Duplicate column")) {
                log.warn("表 {} 添加列 {} 失败: {}", table, column, e.getMessage());
            }
        }
    }
}
