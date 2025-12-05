package view;

import model.User;
import model.Course;
import service.CourseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentMainFrame extends JFrame {

    // 依赖项
    private final CourseService courseService = new CourseService();
    private final User student; // 当前登录的学生对象

    // 界面组件
    private JTable courseTable;
    private DefaultTableModel tableModel;
    private JButton selectCourseButton = new JButton("选课");
    private JButton dropCourseButton = new JButton("退课"); // 新增退课按钮
    // 表格列名
    private static final String[] TABLE_COLUMNS = {
            "课程代码", "课程名称", "学分", "授课教师", "上课时间", "容量", "已选人数", "剩余名额"
    };

    /**
     * 构造函数：初始化界面
     * @param student 登录成功的学生用户信息
     */
    public StudentMainFrame(User student) {
        this.student = student;
        setTitle("选课系统 - 学生端 - 欢迎：" + student.getUserName());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 使用 BorderLayout 布局
        setLayout(new BorderLayout());

        // 1. 初始化表格模型
        tableModel = new DefaultTableModel(null, TABLE_COLUMNS);
        courseTable = new JTable(tableModel);

        // 2. 将表格放入滚动面板
        JScrollPane scrollPane = new JScrollPane(courseTable);
        add(scrollPane, BorderLayout.CENTER);

        // 3. 添加按钮面板 (在底部)
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectCourseButton);
        buttonPanel.add(dropCourseButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 4. 绑定事件和加载数据
        selectCourseButton.addActionListener(e -> handleSelectCourse());
        dropCourseButton.addActionListener(e -> handleDropCourse()); // 绑定退课事件
        loadCourseData();
        setVisible(true);
    }
    /**
     * 处理退课按钮点击事件
     */
    private void handleDropCourse() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择一门课程。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String courseCode = (String) courseTable.getValueAt(selectedRow, 0);
        String courseName = (String) courseTable.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要退选课程：《" + courseName + "》吗？",
                "确认退课", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String result = courseService.dropCourse(student.getUserId(), courseCode);
            JOptionPane.showMessageDialog(this, result, "退课结果",
                    result.contains("成功") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            loadCourseData(); // 刷新数据
        }
    }
    private void loadCourseData() {
        // 清除表格旧数据
        tableModel.setRowCount(0);

        // 1. 调用服务层方法获取课程列表
        List<Course> courses = courseService.getPublishedCourses();

        // 2. 遍历列表，将每个 Course 对象转换为表格行数据
        for (Course course : courses) {
            Object[] rowData = new Object[] {
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getCredit(),
                    course.getTeacherName(),
                    course.getScheduleTime(),
                    course.getCapacityLimit(),
                    course.getCurrentSelected(),
                    course.getRemainingCapacity() // 使用模型中计算的剩余名额
            };
            // 3. 添加行到表格模型
            tableModel.addRow(rowData);
        }

        // 提示信息 (可选)
        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "当前没有开放选课的课程。", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }
// 延续 StudentMainFrame 类

    /**
     * 处理选课按钮点击事件
     */
    private void handleSelectCourse() {
        int selectedRow = courseTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先在表格中选择一门课程。", "操作失败", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. 获取选中的课程代码 (课程代码在表格的第一列，索引为 0)
        String courseCode = (String) courseTable.getValueAt(selectedRow, 0);
        String courseName = (String) courseTable.getValueAt(selectedRow, 1);

        // 2. 确认提示
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要选择课程：《" + courseName + "》吗？",
                "确认选课", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // 3. 调用 CourseService 中的选课方法 (它会调用 MySQL 存储过程)
            // student.getUserId() 是当前登录学生的学号
            String result = courseService.selectCourse(student.getUserId(), courseCode);

            // 4. 根据存储过程返回的消息给出提示
            if (result.contains("成功")) {
                JOptionPane.showMessageDialog(this, result, "选课结果", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // 如果是容量满或时间冲突，存储过程会返回失败信息
                JOptionPane.showMessageDialog(this, result, "选课失败", JOptionPane.ERROR_MESSAGE);
            }

            // 5. 无论成功失败，都需要刷新数据以显示最新的选课人数
            loadCourseData();
        }
    }

    // 可以在 LoginFrame 中替换原来的跳转逻辑，以便测试
    /*
    public static void main(String[] args) {
        // 假设已经登录成功，获取了学生信息
        User testStudent = new User("S2021001", "小明", "Student");
        new StudentMainFrame(testStudent);
    }
    */
}
// ... 下面是两个核心方法