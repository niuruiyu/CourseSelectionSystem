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
        String sql = "SELECT user_id, user_name, role, contact, department, create_time " +
                     "FROM user_info " +
                     "WHERE account = ? AND password = SHA2(?, 256)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, account);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String userId = rs.getString("user_id");
                String userName = rs.getString("user_name");
                String role = rs.getString("role");
                String contact = rs.getString("contact");
                String department = rs.getString("department");
                Timestamp createTime = rs.getTimestamp("create_time");

                user = new User(userId, userName, role);
                user.setContact(contact);
                user.setDepartment(department);
                System.out.println("登录成功: " + user.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
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
            pstmt.setString(4, password);
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
     * 更新学生信息
     */
    public boolean updateStudent(User student) {
        String sql = "UPDATE user_info SET " +
                     "user_name = ?, department = ? " +
                     "WHERE user_id = ? AND role = 'Student'";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getUserName());
            pstmt.setString(2, student.getDepartment());
            pstmt.setString(3, student.getUserId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }

    /**
     * 删除学生
     */
    public boolean deleteStudent(String studentId) {
        String deleteSelectionSql = "DELETE FROM selection_record WHERE student_id = ?";
        String deleteUserSql = "DELETE FROM user_info WHERE user_id = ? AND role = 'Student'";
        
        Connection conn = null;
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        
        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);
            
            // 1. 先删除选课记录
            pstmt1 = conn.prepareStatement(deleteSelectionSql);
            pstmt1.setString(1, studentId);
            pstmt1.executeUpdate();
            
            // 2. 再删除学生信息
            pstmt2 = conn.prepareStatement(deleteUserSql);
            pstmt2.setString(1, studentId);
            int result = pstmt2.executeUpdate();
            
            conn.commit();
            return result > 0;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            
            if (e.getErrorCode() == 1451) {
                System.err.println("删除失败：学生存在关联选课记录");
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            
            // 关闭资源
            try {
                if (pstmt1 != null) pstmt1.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (pstmt2 != null) pstmt2.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 修改学生密码
     */
    public boolean updateStudentPassword(String studentId, String newPassword) {
        String sql = "UPDATE user_info SET password = SHA2(?, 256) WHERE user_id = ? AND role = 'Student'";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newPassword);
            pstmt.setString(2, studentId);
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
                teacher.setContact(rs.getString("contact"));
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
        String sql = "INSERT INTO user_info (user_id, user_name, account, password, role, department, contact) " +
                     "VALUES (?, ?, ?, ?, 'Teacher', ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacher.getUserId());
            pstmt.setString(2, teacher.getUserName());
            pstmt.setString(3, account);
            pstmt.setString(4, password);
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
            if (e.getErrorCode() == 1451) {
                System.err.println("删除失败：教师存在关联课程");
            }
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }
}