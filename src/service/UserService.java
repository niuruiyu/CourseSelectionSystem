package service;

import model.User;
import util.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
    /**
     * 获取所有学生
     */
    public List<User> getAllStudents() {
        List<User> students = new ArrayList<>();
        String sql = "SELECT user_id, user_name, department FROM user_info WHERE role = 'Student'";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                User user = new User(rs.getString("user_id"), rs.getString("user_name"), "Student");
                user.setDepartment(rs.getString("department"));
                students.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, stmt, rs);
        }
        return students;
    }

    /**
     * 添加学生
     */
    public boolean addStudent(User student, String account, String password) {
        String sql = "INSERT INTO user_info (user_id, user_name, account, password, role, department) " +
                "VALUES (?, ?, ?, ?, 'Student', ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getUserId());
            pstmt.setString(2, student.getUserName());
            pstmt.setString(3, account);
            pstmt.setString(4, password); // 实际项目需加密
            pstmt.setString(5, student.getDepartment());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }
    /**
     * 获取所有教师
     */
    public List<User> getAllTeachers() {
        List<User> teachers = new ArrayList<>();
        String sql = "SELECT user_id, user_name, department, contact FROM user_info WHERE role = 'Teacher'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                User teacher = new User(
                        rs.getString("user_id"),
                        rs.getString("user_name"),
                        "Teacher"
                );
                teacher.setDepartment(rs.getString("department"));
                teacher.setContact(rs.getString("contact")); // 需在User类中补充contact字段
                teachers.add(teacher);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return teachers;
    }

    /**
     * 添加教师
     */
    public boolean addTeacher(User teacher, String account, String password) {
        String sql = "INSERT INTO user_info (" +
                "user_id, user_name, account, password, role, department, contact" +
                ") VALUES (?, ?, ?, ?, 'Teacher', ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacher.getUserId());
            pstmt.setString(2, teacher.getUserName());
            pstmt.setString(3, account);
            pstmt.setString(4, password); // 实际项目需加密（如MD5）
            pstmt.setString(5, teacher.getDepartment());
            pstmt.setString(6, teacher.getContact());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }

    /**
     * 更新教师信息
     */
    public boolean updateTeacher(User teacher) {
        String sql = "UPDATE user_info SET " +
                "user_name = ?, department = ?, contact = ? " +
                "WHERE user_id = ? AND role = 'Teacher'";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacher.getUserName());
            pstmt.setString(2, teacher.getDepartment());
            pstmt.setString(3, teacher.getContact());
            pstmt.setString(4, teacher.getUserId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }

    /**
     * 删除教师
     */
    public boolean deleteTeacher(String teacherId) {
        String sql = "DELETE FROM user_info WHERE user_id = ? AND role = 'Teacher'";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacherId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // 外键约束异常（如该教师有未删除的课程）
            if (e.getErrorCode() == 1451) { // MySQL外键约束错误码
                System.err.println("删除失败：教师存在关联课程");
            }
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }
}