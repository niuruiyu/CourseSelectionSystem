package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
public class DBUtils {

    // 【注意：请替换成你的实际数据库配置】
//    private static final String URL = "jdbc:mysql://localhost:3307/course_selection_system?useSSL=false&serverTimezone=UTC";
//    private static final String USER = "root";       // 你的 MySQL 用户名
//    private static final String PASSWORD = "root"; // 你的 MySQL 密码
    private static String URL;
    private static String USER;
    private static String PASSWORD;
    static {
        try {
            // 关键：读取「JAR包同级目录」的dbconfig.properties
            Properties props = new Properties();
            // 这里用FileInputStream，会优先读取JAR包所在目录的配置文件
            props.load(new FileInputStream("dbconfig.properties"));

            // 从配置文件取值
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");

            // 加载MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (FileNotFoundException e) {
            // 配置文件找不到时的提示（方便用户排查）
            System.err.println("未找到dbconfig.properties配置文件，请确认文件和JAR包在同一目录！");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("配置文件读取失败！");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL驱动缺失！");
            e.printStackTrace();
        }
    }
    /**
     * 获取数据库连接
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            // 1. 加载 JDBC 驱动（现代 JDBC 驱动通常会自动加载，但显式调用更保险）
            Class.forName("com.mysql.jdbc.Driver");

            // 2. 建立连接
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("数据库连接成功!");
        } catch (ClassNotFoundException e) {
            System.err.println("错误：未找到 MySQL JDBC 驱动。请检查 jar 包是否正确导入。");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("错误：数据库连接失败，请检查 URL, 用户名和密码。");
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * 关闭资源，避免资源泄漏
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            System.err.println("关闭 ResultSet 失败: " + e.getMessage());
        }
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            System.err.println("关闭 Statement 失败: " + e.getMessage());
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("关闭 Connection 失败: " + e.getMessage());
        }
    }
}