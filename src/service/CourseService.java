package service;

import model.Course;
import util.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseService {

    /**
     * 查询所有已发布的课程及其教师名称
     */

    public List<Course> getPublishedCourses() {
        List<Course> courses = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        // 使用 JOIN user_info 表获取教师姓名
        String sql = "SELECT c.*, u.user_name AS teacher_name " +
                "FROM course_info c " +
                "JOIN user_info u ON c.teacher_id = u.user_id " +
                "WHERE c.status = 'Published'";

        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getDouble("credit"),
                        rs.getInt("capacity_limit"),
                        rs.getInt("current_selected"),
                        rs.getString("teacher_name"), // 对应 JOIN 后的别名
                        rs.getString("schedule_time")
                );
                // 确保 Course Model 有这些 Setter
                course.setStatus(rs.getString("status"));
                course.setClassHour(rs.getInt("class_hour"));
                course.setClassroom(rs.getString("classroom"));

                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 【修正：补全资源关闭】
            DBUtils.close(conn, stmt, rs);
        }
        return courses;
    }

    /**
     * 执行选课操作，调用 MySQL 存储过程
     * @param studentId 当前登录的学生ID
     * @param courseCode 学生选择的课程代码
     * @return 存储过程返回的消息 (成功或失败原因)
     */
    public String selectCourse(String studentId, String courseCode) {
        // SQL 语句：调用存储过程并接收输出参数
        String sql = "{CALL sp_student_select_course(?, ?, ?)}";
        Connection conn = null;
        CallableStatement cstmt = null;
        String resultMessage = "系统错误";

        try {
            conn = DBUtils.getConnection();
            cstmt = conn.prepareCall(sql);

            // 1. 设置输入参数 (IN parameters)
            cstmt.setString(1, studentId);
            cstmt.setString(2, courseCode);

            // 2. 注册输出参数 (OUT parameter)
            cstmt.registerOutParameter(3, Types.VARCHAR);

            // 3. 执行存储过程
            cstmt.execute();

            // 4. 获取输出参数的值
            resultMessage = cstmt.getString(3);

        } catch (SQLException e) {
            e.printStackTrace();
            resultMessage = "数据库执行异常: " + e.getMessage();
        } finally {
            // 【修正：补全资源关闭】
            DBUtils.close(conn, cstmt, null);
        }
        return resultMessage;
    }

    /**
     * 根据教师ID查询该教师开设的所有课程
     */
    public List<Course> getCoursesByTeacher(String teacherId) {
        List<Course> courses = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // 【修正：使用 JOIN 获取教师姓名，保证 Course 构造完整性】
        String sql = "SELECT c.*, u.user_name AS teacher_name " +
                "FROM course_info c " +
                "JOIN user_info u ON c.teacher_id = u.user_id " +
                "WHERE c.teacher_id = ?";

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacherId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getDouble("credit"),
                        rs.getInt("capacity_limit"),
                        rs.getInt("current_selected"),
                        rs.getString("teacher_name"), // 从 JOIN 结果中获取教师姓名
                        rs.getString("schedule_time")
                );
                // 必须使用 Setter 补全 TeacherMainFrame 需要的状态等信息
                course.setStatus(rs.getString("status"));
                course.setClassHour(rs.getInt("class_hour"));
                course.setClassroom(rs.getString("classroom"));
                course.setCourseType(rs.getString("course_type"));

                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 【修正：补全资源关闭】
            DBUtils.close(conn, pstmt, rs);
        }
        return courses;
    }

    /**
     * 教师提交开设新课程的申请
     * @param course 新课程对象，包含所有必要信息
     * @param teacherId 提交申请的教师工号
     * @return 成功返回 true，失败返回 false
     */
    public boolean applyForNewCourse(Course course, String teacherId) {
        String sql = "INSERT INTO course_info " +
                "(course_code, course_name, credit, class_hour, teacher_id, schedule_time, classroom, capacity_limit, course_type, status, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending', ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);

            // 绑定参数
            pstmt.setString(1, course.getCourseCode());
            pstmt.setString(2, course.getCourseName());
            pstmt.setDouble(3, course.getCredit());
            pstmt.setInt(4, course.getClassHour());
            pstmt.setString(5, teacherId); // 绑定当前教师工号
            pstmt.setString(6, course.getScheduleTime());
            pstmt.setString(7, course.getClassroom());
            pstmt.setInt(8, course.getCapacityLimit());
            pstmt.setString(9, course.getCourseType());
            pstmt.setString(10, course.getDescription());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("教师申请开课异常：" + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 【修正：补全资源关闭】
            DBUtils.close(conn, pstmt, null);
        }
    }
    // 延续 package service; 中的 CourseService.java 类

    /**
     * 教务管理员审核课程申请
     * @param courseCode 课程代码
     * @param newStatus 新状态 ('Approved' 或 'Rejected')
     * @return 成功返回 true，失败返回 false
     */
    public boolean auditCourse(String courseCode, String newStatus) {
        // newStatus 应该对应数据库 ENUM 中的 'Published' 或 'Rejected'
        String statusToSet = "Published".equals(newStatus) ? "Published" : "Rejected";

        String sql = "UPDATE course_info SET status = ? WHERE course_code = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, statusToSet);
            pstmt.setString(2, courseCode);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("课程审核操作异常：" + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt, null); // 确保关闭
        }
    }

    /**
     * 获取所有待审核的课程列表
     */
    public List<Course> getPendingCourses() {
        List<Course> courses = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        String sql = "SELECT c.*, u.user_name AS teacher_name " +
                "FROM course_info c " +
                "JOIN user_info u ON c.teacher_id = u.user_id " +
                "WHERE c.status = 'Pending'";

        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getDouble("credit"),
                        rs.getInt("capacity_limit"),
                        rs.getInt("current_selected"),
                        rs.getString("teacher_name"),
                        rs.getString("schedule_time")
                );
                course.setStatus(rs.getString("status"));
                course.setClassHour(rs.getInt("class_hour"));
                course.setClassroom(rs.getString("classroom"));
                course.setCourseType(rs.getString("course_type"));
                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, stmt, rs);
        }
        return courses;
    }
    // 延续 package service; 中的 CourseService.java 类

    // 需要一个新的 DTO 来封装统计结果，或者扩展 Course DTO
    // 为了简化，我们直接返回一个 Map 或 List<Object[]>，或者创建一个 StatsCourse DTO

    /**
     * 查询课程选课统计数据 (查询视图 v_course_stats)
     */
    public List<Course> getCourseStatistics() {
        List<Course> courses = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        // 假设 v_course_stats 视图包含 course_code, course_name, teacher_name, capacity_limit, current_selected, saturation_rate
        String sql = "SELECT * FROM v_course_stats";

        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        0, // 视图中无学分，可忽略
                        rs.getInt("capacity_limit"),
                        rs.getInt("current_selected"),
                        rs.getString("teacher_name"),
                        "" // 视图中无时间，可忽略
                );
                course.setDescription(rs.getString("saturation_rate")); // 用description暂存饱和度
                courses.add(course);
                // ... 将 rs 结果封装到 Course 或新的 Stats DTO 中
                // ... courses.add(...)
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, stmt, rs); // 确保关闭
        }
        return courses;
    }

    //学生退课功能的实现
    public String dropCourse(String studentId, String courseCode) {
        String sql = "UPDATE selection_record SET status = 'Dropped' WHERE student_id = ? AND course_code = ? AND status = 'Selected'";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseCode);
            int rows = pstmt.executeUpdate();
            return rows > 0 ? "退课成功" : "退课失败：未找到选课记录";
        } catch (SQLException e) {
            return "退课失败：" + e.getMessage();
        } finally {
            DBUtils.close(conn, pstmt, null);
        }
    }
}