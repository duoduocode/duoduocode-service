
package com.duoduocode.service;

import java.sql.*;

/**
 * 独立的 JDBC 数据库连接测试
 * 使用 application-dev.yml 中的数据库配置
 */
public class DbTest {

    public static void main(String[] args) {
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://rm-bp1m5pf44nzonuabqfo.mysql.rds.aliyuncs.com:3306/duoduocode_dev?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=30000&socketTimeout=30000";
        String username = "duoduocode_code";
        String password = "Txd31180!";

        System.out.println("=== 开始测试数据库连接 ===");
        System.out.println("驱动: " + driver);
        System.out.println("URL: " + url);
        System.out.println("用户名: " + username);
        System.out.println();

        Connection conn = null;
        try {
            Class.forName(driver);
            System.out.println("✓ JDBC 驱动加载成功");

            long start = System.currentTimeMillis();
            conn = DriverManager.getConnection(url, username, password);
            long duration = System.currentTimeMillis() - start;
            System.out.println("✓ 数据库连接成功，耗时: " + duration + "ms");
            System.out.println();

            testQuery(conn);

        } catch (ClassNotFoundException e) {
            System.err.println("✗ 未找到 JDBC 驱动: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("✗ 数据库连接/查询失败:");
            System.err.println("  SQLState: " + e.getSQLState());
            System.err.println("  ErrorCode: " + e.getErrorCode());
            System.err.println("  错误信息: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println();
                    System.out.println("✓ 连接已关闭");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void testQuery(Connection conn) throws SQLException {
        System.out.println("--- 执行测试查询 (SELECT 1 + 2) ---");
        String sql = "SELECT 1 + 2 AS result";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int result = rs.getInt("result");
                System.out.println("✓ 查询结果: 1 + 2 = " + result);
            }
        }

        System.out.println();
        System.out.println("--- 查看数据库中的表 ---");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            int count = 0;
            while (rs.next()) {
                String tableName = rs.getString(1);
                System.out.println("  - " + tableName);
                count++;
            }
            System.out.println("✓ 共 " + count + " 个表");
        }

        System.out.println();
        System.out.println("--- 简单测试 accounts 表 ---");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM accounts")) {
            while (rs.next()) {
                int count = rs.getInt("cnt");
                System.out.println("✓ accounts 表共有 " + count + " 条记录");
            }
        }
    }
}
