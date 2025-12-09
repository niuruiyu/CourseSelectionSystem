package view;

import util.LogUtil;
import model.User;
import model.Course;
import service.CourseService;
import service.UserService;
import util.CSVExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AdminMainFrame extends JFrame {

    private final CourseService courseService = new CourseService();
    private final UserService userService = new UserService();
    private final User admin;
    private JTabbedPane tabbedPane;
    private JTable statisticsTable;

    public AdminMainFrame(User admin) {
        this.admin = admin;
        setTitle("选课系统 - 教务管理员 - 欢迎：" + admin.getUserName());
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 【添加日志】管理员进入系统
        LogUtil.log(admin.getUserId(), "进入管理员系统", 
                   "管理员 " + admin.getUserName() + "(" + admin.getUserId() + ") 进入管理员主界面");

        // 创建菜单栏
        createMenuBar();
        
        tabbedPane = new JTabbedPane();

        tabbedPane.addTab("课程开设审核", createAuditPanel());
        tabbedPane.addTab("选课统计报表", createStatisticsPanel());
        tabbedPane.addTab("用户数据维护", createUserMaintenancePanel());

        add(tabbedPane, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * 创建菜单栏
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        
        JMenuItem changePwdItem = new JMenuItem("修改密码");
        JMenuItem logoutItem = new JMenuItem("退出登录");
        
        changePwdItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showChangePasswordDialog();
            }
        });
        
        logoutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogout();
            }
        });
        
        helpMenu.add(changePwdItem);
        helpMenu.addSeparator();
        helpMenu.add(logoutItem);
        
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    /**
     * 显示修改密码对话框
     */
    private void showChangePasswordDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPasswordField oldPwdField = new JPasswordField(15);
        JPasswordField newPwdField = new JPasswordField(15);
        JPasswordField confirmPwdField = new JPasswordField(15);
        
        panel.add(new JLabel("当前密码:"));
        panel.add(oldPwdField);
        panel.add(new JLabel("新密码:"));
        panel.add(newPwdField);
        panel.add(new JLabel("确认新密码:"));
        panel.add(confirmPwdField);
        panel.add(new JLabel(""));
        panel.add(new JLabel("（密码长度至少6位）"));
        
        int result = JOptionPane.showConfirmDialog(this, panel, "修改密码", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String oldPassword = new String(oldPwdField.getPassword());
            String newPassword = new String(newPwdField.getPassword());
            String confirmPassword = new String(confirmPwdField.getPassword());
            
            // 验证输入
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "所有字段都不能为空", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "两次输入的新密码不一致", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(this, "新密码长度至少6位", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (oldPassword.equals(newPassword)) {
                JOptionPane.showMessageDialog(this, "新密码不能与旧密码相同", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 【关键修改】使用validatePassword方法验证旧密码
            boolean oldPasswordCorrect = userService.validatePassword(admin.getUserId(), oldPassword);
            
            if (!oldPasswordCorrect) {
                JOptionPane.showMessageDialog(this, "当前密码错误", "验证失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 更新密码 - 需要在UserService中添加updateAdminPassword方法
            boolean success = userService.updateAdminPassword(admin.getUserId(), newPassword);
            
            if (success) {
                // 记录日志
                LogUtil.log(admin.getUserId(), "修改密码", 
                           "管理员 " + admin.getUserName() + " 修改密码成功");
                
                JOptionPane.showMessageDialog(this, 
                    "密码修改成功！\n" +
                    "请记住新密码，下次登录时使用。",
                    "修改成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                LogUtil.log(admin.getUserId(), "修改密码失败", 
                           "管理员 " + admin.getUserName() + " 修改密码失败");
                
                JOptionPane.showMessageDialog(this, 
                    "密码修改失败，请稍后重试",
                    "修改失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * 退出登录
     */
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要退出登录吗？",
            "确认退出", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // 记录登出日志
            LogUtil.logLogout(admin.getUserId(), admin.getUserName());
            
            // 关闭当前窗口
            this.dispose();
            
            // 重新打开登录窗口
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        new LoginFrame();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    // ===================================
    // 课程审核面板 (Audit Panel)
    // ===================================
    private JPanel createAuditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"代码", "名称", "教师", "学分", "容量", "时间"};
        DefaultTableModel model = new DefaultTableModel(null, columns);
        JTable table = new JTable(model);

        // 刷新待审核数据
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

    // 在 handleAuditAction 方法中添加
    private void handleAuditAction(JTable table, DefaultTableModel model, String action) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请选择要审核的课程。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String courseCode = (String) model.getValueAt(selectedRow, 0);
        String courseName = (String) model.getValueAt(selectedRow, 1);

        boolean success = courseService.auditCourse(courseCode, action);

        // 【添加日志】课程审核
        if (success) {
            String actionText = "Published".equals(action) ? "审核通过" : "审核驳回";
            LogUtil.log(admin.getUserId(), "课程审核", 
                       "管理员 " + admin.getUserName() + " " + actionText + 
                       "课程：" + courseName + "(" + courseCode + ")");
            
            JOptionPane.showMessageDialog(this, "课程：" + courseCode + " 已审核" + 
                ("Published".equals(action) ? "通过并发布" : "驳回") + "。", "成功", JOptionPane.INFORMATION_MESSAGE);
            
            refreshAuditTableData(model);
        } else {
            LogUtil.log(admin.getUserId(), "课程审核失败", 
                       "管理员 " + admin.getUserName() + " 审核课程 " + 
                       courseName + "(" + courseCode + ") 失败");
            
            JOptionPane.showMessageDialog(this, "审核失败。", "失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 刷新审核表格数据
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
    // 选课统计面板 (Statistics Panel) - 主要修改这里！
    // ===================================
    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 修改1：增加更多列，包括学分和上课时间
        String[] columns = {"课程代码", "课程名称", "授课教师", "学分", "上课时间", "容量", "已选人数", "饱和度 (%)"};
        DefaultTableModel model = new DefaultTableModel(null, columns);
        statisticsTable = new JTable(model); // 保存引用

        // 修改2：重新写数据填充逻辑
        refreshStatisticsTableData(model);

        // 修改3：优化导出按钮
        JButton exportBtn = new JButton("导出报表");
        exportBtn.addActionListener(e -> handleExportAction());

        // 添加刷新按钮
        JButton refreshBtn = new JButton("刷新数据");
        refreshBtn.addActionListener(e -> {
            refreshStatisticsTableData(model);
            JOptionPane.showMessageDialog(this, "数据已刷新！", "提示", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(refreshBtn);
        topPanel.add(exportBtn);

        panel.add(new JScrollPane(statisticsTable), BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        return panel;
    }

    // 新增方法：刷新统计表格数据
    private void refreshStatisticsTableData(DefaultTableModel model) {
        model.setRowCount(0); // 清空表格
        List<Course> stats = courseService.getCourseStatistics();
        
        if (stats == null || stats.isEmpty()) {
            // 添加提示行
            model.addRow(new Object[]{"暂无数据", "", "", "", "", "", "", ""});
            return;
        }
        
        for (Course course : stats) {
            // 计算饱和度百分比
            double saturation = 0.0;
            if (course.getCapacityLimit() > 0) {
                saturation = (course.getCurrentSelected() * 100.0) / course.getCapacityLimit();
            }
            
            Object[] row = {
                course.getCourseCode(),
                course.getCourseName(),
                course.getTeacherName(),
                course.getCredit(),           // 学分
                course.getScheduleTime(),     // 上课时间
                course.getCapacityLimit(),
                course.getCurrentSelected(),
                String.format("%.2f%%", saturation)  // 格式化百分比
            };
            model.addRow(row);
        }
    }

    // 新增方法：处理导出操作
    private void handleExportAction() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String defaultFileName = "选课统计_" + timestamp;
        
        String[] options = {"导出当前视图", "导出完整数据", "取消"};
        int choice = JOptionPane.showOptionDialog(this,
            "请选择导出内容：\n" +
            "• 当前视图：导出当前表格显示的数据（" + statisticsTable.getRowCount() + "行）\n" +
            "• 完整数据：从数据库导出所有课程统计（包含所有字段）",
            "导出选项",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 0) {
            // 【添加日志】导出报表
            LogUtil.log(admin.getUserId(), "导出统计报表", 
                       "管理员 " + admin.getUserName() + " 导出选课统计报表（视图）");
            
            boolean success = CSVExporter.exportToCSV(statisticsTable, defaultFileName + "_视图");
            if (success) {
                JOptionPane.showMessageDialog(this,
                    " CSV导出成功！\n" +
                    "文件已保存，包含以下字段：\n" +
                    "课程代码、课程名称、授课教师、学分、上课时间、容量、已选人数、饱和度",
                    "导出成功",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (choice == 1) {
            // 【添加日志】导出完整数据
            LogUtil.log(admin.getUserId(), "导出完整数据", 
                       "管理员 " + admin.getUserName() + " 导出所有课程完整数据");
            
            List<Course> allCourses = courseService.getCourseStatistics();
            if (allCourses == null || allCourses.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "没有可导出的课程数据！",
                    "提示",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            boolean success = CSVExporter.exportCoursesToCSV(allCourses, defaultFileName + "_完整数据");
            if (success) {
                JOptionPane.showMessageDialog(this,
                    " 课程数据导出成功！\n" +
                    "共导出 " + allCourses.size() + " 门课程。\n" +
                    "文件包含完整的课程信息，包括学分和上课时间。",
                    "导出成功",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
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

        return panel;
    }
}