package view;
import model.User;
import model.Course;
import service.CourseService;
import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class AdminMainFrame extends JFrame {

    private final CourseService courseService = new CourseService();
    private final User admin;
    private JTabbedPane tabbedPane;

    public AdminMainFrame(User admin) {
        this.admin = admin;
        setTitle("选课系统 - 教务管理员 - 欢迎：" + admin.getUserName());
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();

        // 1. 课程开设审核面板
        tabbedPane.addTab("课程开设审核", createAuditPanel());

        // 2. 选课统计报表面板
        tabbedPane.addTab("选课统计报表", createStatisticsPanel());

        // 3. 用户数据维护面板 (简化，仅显示功能名)
        tabbedPane.addTab("用户数据维护", createUserMaintenancePanel());

        add(tabbedPane, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ===================================
    // 课程审核面板 (Audit Panel)
    // ===================================
    private JPanel createAuditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"代码", "名称", "教师", "学分", "容量", "时间"};
        DefaultTableModel model = new DefaultTableModel(null, columns);
        JTable table = new JTable(model);


        // 刷新待审核数据（补充实现）
        refreshAuditTableData(model);
        JButton approveBtn = new JButton("通过 (发布)");
        JButton rejectBtn = new JButton("驳回");

        // 绑定审核事件
        approveBtn.addActionListener(e -> handleAuditAction(table, model, "Published"));
        rejectBtn.addActionListener(e -> handleAuditAction(table, model, "Rejected"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(approveBtn);
        buttonPanel.add(rejectBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 审核事件处理方法
    private void handleAuditAction(JTable table, DefaultTableModel model, String action) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请选择要审核的课程。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String courseCode = (String) model.getValueAt(selectedRow, 0);

        boolean success = courseService.auditCourse(courseCode, action);

        if (success) {
            JOptionPane.showMessageDialog(this, "课程：" + courseCode + " 已审核" + ("Published".equals(action) ? "通过并发布" : "驳回") + "。", "成功", JOptionPane.INFORMATION_MESSAGE);
            // 刷新表格数据
            refreshAuditTableData(model);
        } else {
            JOptionPane.showMessageDialog(this, "审核失败。", "失败", JOptionPane.ERROR_MESSAGE);
        }
    }
    // 刷新审核表格数据（新增方法）
    private void refreshAuditTableData(DefaultTableModel model) {
        model.setRowCount(0); // 清空表格
        List<Course> pendingCourses = courseService.getPendingCourses();
        for (Course course : pendingCourses) {
            Object[] row = {
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getTeacherName(),
                    course.getCredit(),
                    course.getCapacityLimit(),
                    course.getScheduleTime()
            };
            model.addRow(row);
        }
    }
    // ===================================
    // 选课统计面板 (Statistics Panel)
    // ===================================
    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"代码", "名称", "教师", "容量", "已选人数", "饱和度 (%)"};
        DefaultTableModel model = new DefaultTableModel(null, columns);
        JTable table = new JTable(model);

        // 刷新统计数据（补充实现）
        List<Course> stats = courseService.getCourseStatistics();
        for (Course course : stats) {
            Object[] row = {
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getTeacherName(),
                    course.getCapacityLimit(),
                    course.getCurrentSelected(),
                    course.getDescription() // 饱和度
            };
            model.addRow(row);
        }

        JButton exportBtn = new JButton("导出报表 (Excel)");
        // 导出功能是加分项，需要用到 Apache POI 等库
        exportBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Excel导出功能开发中"));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(exportBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        return panel;
    }

    // ===================================
    // 用户数据维护面板 (Maintenance Panel)
    // ===================================
    private JPanel createUserMaintenancePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 20)); // 分为学生、教师、管理员维护
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JButton studentBtn = new JButton("维护学生信息 (增删改)");
        JButton teacherBtn = new JButton("维护教师信息 (增删改)");
        JButton logBtn = new JButton("查看系统操作日志");
        // 绑定事件（打开对应维护窗口）
        studentBtn.addActionListener(e -> new StudentMaintenanceFrame());
        teacherBtn.addActionListener(e -> new TeacherMaintenanceFrame());
        logBtn.addActionListener(e -> new OperationLogFrame());
        panel.add(studentBtn);
        panel.add(teacherBtn);
        panel.add(logBtn);

        // ... 绑定事件

        return panel;
    }
}