package model;

// 对应 course_info 表的部分字段
public class Course {
    private String courseCode;
    private String courseName;
    private double credit;
    private int capacityLimit;
    private int currentSelected;
    private String teacherName; // 数据库中是 teacher_id，这里用 name 方便显示
    private String scheduleTime;

    // 构造函数
    public Course(String courseCode, String courseName, double credit, int capacityLimit, int currentSelected, String teacherName, String scheduleTime) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credit = credit;
        this.capacityLimit = capacityLimit;
        this.currentSelected = currentSelected;
        this.teacherName = teacherName;
        this.scheduleTime = scheduleTime;
    }
    private String status;         // 课程状态
    private int classHour;         // 课时
    private String classroom;      // 上课地点
    private String courseType;     // 课程类型
    private String description;    // 课程简介
    // Getter方法 (为简洁省略Setter)
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getClassHour() { return classHour; }
    public void setClassHour(int classHour) { this.classHour = classHour; }

    public String getClassroom() { return classroom; }
    public void setClassroom(String classroom) { this.classroom = classroom; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public double getCredit() { return credit; }
    public int getCapacityLimit() { return capacityLimit; }
    public int getCurrentSelected() { return currentSelected; }
    public String getTeacherName() { return teacherName; }
    public String getScheduleTime() { return scheduleTime; }

    // 计算剩余名额
    public int getRemainingCapacity() {
        return capacityLimit - currentSelected;
    }
}