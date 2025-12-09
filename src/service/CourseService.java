package service;

import model.Course;
import model.User;
import util.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 学生选课操作，调用数据库存储过程，获取包含详细冲突信息的返回消息。
     * @param studentId 学生ID
     * @param courseCode 课程代码
     * @return 选课结果消息（包含成功、容量满、先修课冲突、时间冲突的详细信息）
     */
    public String selectCourse(String studentId, String courseCode) {
        // 调用存储过程的 SQL 模板，存储过程有两个输入参数，一个输出参数
        String sql = "{CALL sp_student_select_course(?, ?, ?)}";
        Connection conn = null;
        CallableStatement cstmt = null; // 使用 CallableStatement
        String message = "选课失败：系统异常"; // 默认错误信息

        try {
            conn = DBUtils.getConnection();
            cstmt = conn.prepareCall(sql);

            // 1. 绑定输入参数 (IN parameters)
            cstmt.setString(1, studentId);   // p_student_id
            cstmt.setString(2, courseCode);  // p_course_code

            // 2. 注册输出参数 (OUT parameter)
            // 存储过程中的 p_message 是 VARCHAR(100)
            cstmt.registerOutParameter(3, java.sql.Types.VARCHAR); 

            // 3. 执行存储过程
            cstmt.execute();

            // 4. 获取输出参数的值
            message = cstmt.getString(3); // p_message

        } catch (SQLException e) {
            e.printStackTrace();
            message = "选课失败：数据库错误：" + e.getMessage();
        } finally {
            DBUtils.close(conn, cstmt, null); // 关闭资源
        }

        return message;
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
                    rs.getString("course_code"),       // 1. 课程代码
                    rs.getString("course_name"),       // 2. 课程名称
                    rs.getDouble("credit"),            // 3. 学分 (注意：getDouble不是getInt!)
                    rs.getInt("capacity_limit"),       // 4. 容量
                    rs.getInt("current_selected"),     // 5. 已选人数
                    rs.getString("teacher_name"),      // 6. 教师姓名
                    rs.getString("schedule_time")      // 7. 上课时间
                );
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
    public List<User> getStudentsByCourse(String courseCode) {
        List<User> students = new ArrayList<>();
        
            // 修复SQL：添加状态筛选，只查询选课状态为'Selected'的记录
        String sql = "SELECT u.user_id, u.user_name, u.department, sr.selection_time " +
                    "FROM selection_record sr " +
                    "JOIN user_info u ON sr.student_id = u.user_id " +
                    "WHERE sr.course_code = ? AND sr.status = 'Selected' " +
                    "ORDER BY sr.selection_time DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, courseCode);
            rs = pstmt.executeQuery();
            
            // 添加查询调试
            System.out.println("执行SQL: " + sql.replace("?", "'" + courseCode + "'"));
            
            int count = 0;
            while (rs.next()) {
                count++;
                User student = new User(
                    rs.getString("user_id"),
                    rs.getString("user_name"),
                    "Student"
                );
                student.setDepartment(rs.getString("department"));
                students.add(student);
                
                // 调试输出
                System.out.println("找到学生: " + student.getUserId() + " - " + student.getUserName());
            }
            
            System.out.println("总共找到 " + count + " 名学生");
            
        } catch (SQLException e) {
            System.err.println("查询选课学生失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return students;
    }
        /**
     * 获取课程学生名单（包含选课时间）
     * 返回Object数组列表：[userId, userName, department, selectionTime]
     */
    public List<Object[]> getStudentsWithSelectionTime(String courseCode) {
        List<Object[]> result = new ArrayList<>();
        
        String sql = "SELECT u.user_id, u.user_name, u.department, sr.selection_time " +
                    "FROM selection_record sr " +
                    "JOIN user_info u ON sr.student_id = u.user_id " +
                    "WHERE sr.course_code = ? AND sr.status = 'Selected' " +
                    "ORDER BY sr.selection_time DESC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, courseCode);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Object[] row = new Object[4];
                row[0] = rs.getString("user_id");
                row[1] = rs.getString("user_name");
                row[2] = rs.getString("department");
                row[3] = rs.getTimestamp("selection_time");
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("查询选课学生失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return result;
    }
    // 检查学生是否已经选了某门课
    public boolean isCourseSelected(String studentId, String courseCode) {
        String sql = "SELECT COUNT(*) FROM selection_record WHERE student_id = ? AND course_code = ?";
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseCode);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // 如果计数大于0，表示已经选了
            }
        } catch (SQLException e) {
            System.err.println("检查选课记录失败：" + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    public List<Course> getStudentSelectedCourses(String studentId) {
    List<Course> courses = new ArrayList<>();
    
    // 查询学生已选且未退课的课程
    String sql = "SELECT c.*, u.user_name AS teacher_name " +
                 "FROM course_info c " +
                 "JOIN user_info u ON c.teacher_id = u.user_id " +
                 "JOIN selection_record sr ON c.course_code = sr.course_code " +
                 "WHERE sr.student_id = ? AND sr.status = 'Selected' " +
                 "AND c.status = 'Published' " +
                 "ORDER BY c.schedule_time";
    
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    try {
        conn = DBUtils.getConnection();
        pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, studentId);
        rs = pstmt.executeQuery();
        
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
        System.err.println("查询学生已选课程失败: " + e.getMessage());
        e.printStackTrace();
    } finally {
        DBUtils.close(conn, pstmt, rs);
    }
    return courses;
}

/**
 * 获取学生已选课程总数和总学分
 */
    public Map<String, Object> getStudentCourseStats(String studentId) {
        Map<String, Object> stats = new HashMap<>();
        
        String sql = "SELECT COUNT(*) as course_count, SUM(c.credit) as total_credits " +
                    "FROM selection_record sr " +
                    "JOIN course_info c ON sr.course_code = c.course_code " +
                    "WHERE sr.student_id = ? AND sr.status = 'Selected'";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                stats.put("courseCount", rs.getInt("course_count"));
                stats.put("totalCredits", rs.getDouble("total_credits"));
            }
        } catch (SQLException e) {
            System.err.println("查询学生课程统计失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return stats;
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