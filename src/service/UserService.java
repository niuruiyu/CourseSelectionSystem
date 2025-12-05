package service;

import model.User;
import util.DBUtils;
import java.sql.*;

public class UserService {

    /**
     * 验证用户登录，并返回用户信息（包含角色）
     * @param account 用户输入的账号
     * @param password 用户输入的密码
     * @return 登录成功返回 User 对象，失败返回 null
     */
    public User login(String account, String password) {
        // 使用 PreparedStatement 防止 SQL 注入
        String sql = "SELECT user_id, user_name, role FROM user_info WHERE account = ? AND password = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);

            // 绑定参数
            pstmt.setString(1, account);
            pstmt.setString(2, password); // 注意：实际项目中密码需要先加密再对比！

            rs = pstmt.executeQuery();

            if (rs.next()) {
                // 登录成功，从结果集中提取数据
                String userId = rs.getString("user_id");
                String userName = rs.getString("user_name");
                String role = rs.getString("role");

                user = new User(userId, userName, role);
                System.out.println("登录成功: " + user.toString());
            } else {
                // 账号或密码错误
                System.out.println("登录失败：账号或密码错误");
            }

        } catch (SQLException e) {
            System.err.println("数据库操作异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 确保资源被关闭
            DBUtils.close(conn, pstmt, rs);
        }
        return user;
    }
}