package service;

import model.User;
import util.DBUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    /**
     * 验证用户登录（使用加密密码验证）
     */
    public User login(String account, String password) {
        // 先查询用户信息
        String sql = "SELECT user_id, user_name, role, contact, department, create_time, password " +
                     "FROM user_info " +
                     "WHERE account = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        User user = null;
        
        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, account);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // 获取数据库中存储的密码hash
                String storedPasswordHash = rs.getString("password");
                
                // 验证密码
                if (verifyPassword(password, storedPasswordHash)) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return user;
    }
/**
 * 验证用户密码（通用方法，可以用于修改密码时的旧密码验证）
 * @param userId 用户ID
 * @param password 明文密码
 * @return 密码是否正确
 */
public boolean validatePassword(String userId, String password) {
    String sql = "SELECT password FROM user_info WHERE user_id = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    try {
        conn = DBUtils.getConnection();
        pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, userId);
        rs = pstmt.executeQuery();
        
        if (rs.next()) {
            // 获取数据库中存储的加密密码
            String storedHash = rs.getString("password");
            
            // 将输入的密码加密后比较
            String inputHash = encryptPassword(password);
            
            // 如果加密失败或者不匹配，返回false
            if (inputHash == null) {
                return false;
            }
            
            // 安全地比较两个hash值
            return inputHash.equals(storedHash);
        }
    } catch (SQLException e) {
        System.err.println("验证密码失败: " + e.getMessage());
        e.printStackTrace();
    } finally {
        DBUtils.close(conn, pstmt, rs);
    }
    return false;
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
     * 密码加密方法（使用SHA-256）
     */
    private String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("密码加密失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
        /**
     * 添加学生（密码加密存储）
     */
    public boolean addStudent(User student, String account, String password) {
        // 加密密码
        String encryptedPassword = encryptPassword(password);
        if (encryptedPassword == null) {
            return false;
        }
        
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
            pstmt.setString(4, encryptedPassword);  // 使用加密后的密码
            pstmt.setString(5, student.getDepartment());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("添加学生失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }
        /**
     * 修改学生密码（加密存储）
     */
    public boolean updateStudentPassword(String studentId, String newPassword) {
        // 加密新密码
        String encryptedPassword = encryptPassword(newPassword);
        if (encryptedPassword == null) {
            return false;
        }
        
        String sql = "UPDATE user_info SET password = ? WHERE user_id = ? AND role = 'Student'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, encryptedPassword);  // 使用加密后的密码
            pstmt.setString(2, studentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("修改学生密码失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }
            /**
     * 修改管理员密码（加密存储）
     */
    public boolean updateAdminPassword(String adminId, String newPassword) {
        // 加密新密码
        String encryptedPassword = encryptPassword(newPassword);
        if (encryptedPassword == null) {
            return false;
        }
        
        String sql = "UPDATE user_info SET password = ? WHERE user_id = ? AND role = 'EduAdmin'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, encryptedPassword);  // 使用加密后的密码
            pstmt.setString(2, adminId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("修改管理员密码失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }
    /**
     * 验证密码（比较加密后的密码）
     */
    public boolean verifyPassword(String inputPassword, String storedHash) {
        String inputHash = encryptPassword(inputPassword);
        return inputHash != null && inputHash.equals(storedHash);
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
     * 更新教师信息
     */
    public boolean updateTeacher(User teacher) {
        String sql = "UPDATE user_info SET " +
                     "user_name = ?, department = ? " +
                     "WHERE user_id = ? AND role = 'Teacher'";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacher.getUserName());
            pstmt.setString(2, teacher.getDepartment());
            pstmt.setString(3, teacher.getUserId());
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
 * 添加教师（密码加密存储）
 */
public boolean addTeacher(User teacher, String account, String password) {
    // 加密密码
    String encryptedPassword = encryptPassword(password);
    if (encryptedPassword == null) {
        return false;
    }
    
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
        pstmt.setString(4, encryptedPassword);  // 使用加密后的密码
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
 * 修改教师密码（加密存储）
 */
    public boolean updateTeacherPassword(String teacherId, String newPassword) {
        // 加密新密码
        String encryptedPassword = encryptPassword(newPassword);
        if (encryptedPassword == null) {
            return false;
        }
        
        String sql = "UPDATE user_info SET password = ? WHERE user_id = ? AND role = 'Teacher'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, encryptedPassword);  // 使用加密后的密码
            pstmt.setString(2, teacherId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("修改教师密码失败: " + e.getMessage());
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