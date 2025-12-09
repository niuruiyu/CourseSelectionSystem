package view;

import model.User;
import model.Course;
import service.CourseService;
import service.UserService;
import util.LogUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class TeacherMainFrame extends JFrame {

    private final CourseService courseService = new CourseService();
    private final UserService userService = new UserService();
    private final User teacher;
    private JTabbedPane tabbedPane;

    // “我的课程” 面板组件
    private JTable myCourseTable;
    private DefaultTableModel myCourseTableModel;
    private final String[] MY_COURSE_COLUMNS = {"代码", "名称", "学分", "状态", "容量", "已选", "时间", "地点"};

    /**
     * 构造函数：初始化教师主界面
     * @param teacher 登录成功的教师用户信息
     */
    public TeacherMainFrame(User teacher) {
        this.teacher = teacher;
        setTitle("选课系统 - 教师端 - 欢迎：" + teacher.getUserName() + " (工号: " + teacher.getUserId() + ")");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //  【添加日志】教师进入系统
        LogUtil.log(teacher.getUserId(), "进入教师系统", 
                   "教师 " + teacher.getUserName() + "(" + teacher.getUserId() + ") 进入教师主界面");

        tabbedPane = new JTabbedPane();

        // 1. 添加功能模块
        tabbedPane.addTab("我的课程", createMyCoursesPanel());
        tabbedPane.addTab("开设新课程", createNewCoursePanel());

        add(tabbedPane, BorderLayout.CENTER);
        setLocationRelativeTo(null); // 窗口居中
        
        // 创建菜单栏
        createMenuBar();
        
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
        
        // 【关键修改】使用新的validatePassword方法验证旧密码
        boolean oldPasswordCorrect = userService.validatePassword(teacher.getUserId(), oldPassword);
        
        if (!oldPasswordCorrect) {
            JOptionPane.showMessageDialog(this, "当前密码错误", "验证失败", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 更新密码 - 需要先在UserService中添加updateTeacherPassword方法
        boolean success = userService.updateTeacherPassword(teacher.getUserId(), newPassword);
        
        if (success) {
            // 记录日志
            LogUtil.log(teacher.getUserId(), "修改密码", 
                       "教师 " + teacher.getUserName() + " 修改密码成功");
            
            JOptionPane.showMessageDialog(this, 
                "密码修改成功！\n" +
                "请记住新密码，下次登录时使用。",
                "修改成功", JOptionPane.INFORMATION_MESSAGE);
        } else {
            LogUtil.log(teacher.getUserId(), "修改密码失败", 
                       "教师 " + teacher.getUserName() + " 修改密码失败");
            
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
            LogUtil.logLogout(teacher.getUserId(), teacher.getUserName());
            
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
    // 1. "我的课程" 面板实现
    // ===================================
    private JPanel createMyCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 初始化表格模型
        myCourseTableModel = new DefaultTableModel(null, MY_COURSE_COLUMNS);
        myCourseTable = new JTable(myCourseTableModel);

        JScrollPane scrollPane = new JScrollPane(myCourseTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 底部按钮面板
        JButton refreshButton = new JButton("刷新课程列表");
        JButton viewStudentButton = new JButton("查看选课学生名单");

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadMyCoursesData();
            }
        });
        
        viewStudentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleViewStudents();
            }
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(refreshButton);
        bottomPanel.add(viewStudentButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 初始加载数据
        loadMyCoursesData();

        return panel;
    }

    /**
     * 加载该教师的课程数据并刷新表格
     */
    private void loadMyCoursesData() {
        myCourseTableModel.setRowCount(0);

        // 调用服务层方法获取数据
        List<Course> courses = courseService.getCoursesByTeacher(teacher.getUserId());

        //  【添加日志】查看课程列表
        LogUtil.log(teacher.getUserId(), "查看我的课程", 
                   "教师 " + teacher.getUserName() + " 查看自己的课程列表，共 " + courses.size() + " 门课程");

        for (Course course : courses) {
            Object[] rowData = new Object[] {
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getCredit(),
                    course.getStatus(),
                    course.getCapacityLimit(),
                    course.getCurrentSelected(),
                    course.getScheduleTime(),
                    course.getClassroom()
            };
            myCourseTableModel.addRow(rowData);
        }
    }

    /**
     * 处理 "查看选课学生名单" 按钮事件
     */
    private void handleViewStudents() {
        int selectedRow = myCourseTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择一门课程。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String courseCode = (String) myCourseTable.getValueAt(selectedRow, 0);
        String courseName = (String) myCourseTable.getValueAt(selectedRow, 1);

        // 检查课程状态，只有已发布的课程才能查看学生名单
        String status = (String) myCourseTable.getValueAt(selectedRow, 3);
        if (!"Published".equals(status) && !"发布".equals(status)) {
            JOptionPane.showMessageDialog(this,
                "课程《" + courseName + "》还未发布，暂无选课学生。\n" +
                "当前状态: " + status,
                "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 【添加日志】查看学生名单
        LogUtil.log(teacher.getUserId(), "查看学生名单", 
                "教师 " + teacher.getUserName() + " 查看课程 " + 
                courseName + "(" + courseCode + ") 的选课学生");

        // 弹出新的窗口显示学生名单
        new CourseStudentsFrame(courseCode, courseName, teacher.getUserId());
    }

    // ===================================
    // 2. "开设新课程" 面板实现
    // ===================================
    private JPanel createNewCoursePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // 设置组件间距

        // 定义表单组件
        JTextField codeField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JTextField creditField = new JTextField(15);
        JTextField hourField = new JTextField(15);
        JTextField timeField = new JTextField(15);
        JTextField classroomField = new JTextField(15);
        JTextField limitField = new JTextField(15);
        JTextField typeField = new JTextField(15);
        JTextArea descArea = new JTextArea(3, 15);

        JButton submitButton = new JButton("提交开课申请");

        // 布局 (使用 GridBagLayout 方便对齐)
        gbc.anchor = GridBagConstraints.EAST;
        int y = 0;
        addRow(panel, gbc, y++, new JLabel("课程代码:"), codeField);
        addRow(panel, gbc, y++, new JLabel("课程名称:"), nameField);
        addRow(panel, gbc, y++, new JLabel("学分:"), creditField);
        addRow(panel, gbc, y++, new JLabel("课时:"), hourField);
        addRow(panel, gbc, y++, new JLabel("上课时间:"), timeField);
        addRow(panel, gbc, y++, new JLabel("上课地点:"), classroomField);
        addRow(panel, gbc, y++, new JLabel("容量上限:"), limitField);
        addRow(panel, gbc, y++, new JLabel("课程类型:"), typeField);

        gbc.gridy = y; gbc.gridx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("课程简介:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JScrollPane(descArea), gbc);

        gbc.gridy = ++y; gbc.gridx = 1; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(submitButton, gbc);

        // 提交按钮事件处理
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSubmitApplication(
                    codeField, nameField, creditField, hourField, timeField, 
                    classroomField, limitField, typeField, descArea
                );
            }
        });

        return panel;
    }

    /**
     * 辅助方法：简化 GridBagLayout 的行添加
     */
    private void addRow(JPanel panel, GridBagConstraints gbc, int y, JComponent label, JComponent field) {
        gbc.gridy = y; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(label, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }

    /**
     * 处理开课申请表单提交逻辑 (健壮性检查)
     */
    private void handleSubmitApplication(
            JTextField codeField, JTextField nameField, JTextField creditField, JTextField hourField,
            JTextField timeField, JTextField classroomField, JTextField limitField, JTextField typeField, JTextArea descArea) {

        // 1. 简单的非空检查
        if (codeField.getText().isEmpty() || nameField.getText().isEmpty() || creditField.getText().isEmpty() || limitField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "课程代码、名称、学分和容量为必填项。", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. 健壮性检查：数字格式验证
        double credit;
        int capacity, hour;
        try {
            credit = Double.parseDouble(creditField.getText());
            capacity = Integer.parseInt(limitField.getText());
            hour = Integer.parseInt(hourField.getText()); // 假设课时也必须是数字
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "学分、课时和容量上限必须是有效的数字！", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 3. 封装 Course 对象 (需要 Course.java 支持这些设置)
            Course newCourse = new Course(
                    codeField.getText(), nameField.getText(), credit, capacity, 0, teacher.getUserName(), timeField.getText()
            );
            newCourse.setClassHour(hour);
            newCourse.setClassroom(classroomField.getText());
            newCourse.setCourseType(typeField.getText());
            newCourse.setDescription(descArea.getText());

            // 4. 调用服务层提交申请
            boolean success = courseService.applyForNewCourse(newCourse, teacher.getUserId());

            //  【添加日志】开课申请
            String courseCode = codeField.getText();
            String courseName = nameField.getText();
            
            if (success) {
                LogUtil.log(teacher.getUserId(), "开课申请提交", 
                           "教师 " + teacher.getUserName() + " 申请开设课程：" + 
                           courseName + "(" + courseCode + ")，学分：" + credit + "，容量：" + capacity + "，课时：" + hour);
                
                JOptionPane.showMessageDialog(this, "课程申请提交成功！请等待教务员审核。", "成功", JOptionPane.INFORMATION_MESSAGE);
                
                // 成功后清空表单
                codeField.setText(""); 
                nameField.setText(""); 
                creditField.setText(""); 
                hourField.setText("");
                timeField.setText(""); 
                classroomField.setText(""); 
                limitField.setText(""); 
                typeField.setText("");
                descArea.setText("");

                // 切换到"我的课程"面板查看状态
                tabbedPane.setSelectedIndex(0);
                loadMyCoursesData();
            } else {
                LogUtil.log(teacher.getUserId(), "开课申请失败", 
                           "教师 " + teacher.getUserName() + " 申请课程 " + 
                           courseName + "(" + courseCode + ") 失败，可能代码重复");
                
                JOptionPane.showMessageDialog(this, "提交失败：课程代码可能已存在，请检查。", "失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            // 【添加日志】开课申请异常
            LogUtil.log(teacher.getUserId(), "开课申请异常", 
                       "教师 " + teacher.getUserName() + " 开课申请异常：" + ex.getMessage());
            
            JOptionPane.showMessageDialog(this, "发生未知错误：" + ex.getMessage(), "系统错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}