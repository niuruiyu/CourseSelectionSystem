package view;

import model.User;
import model.Course;
import service.CourseService;
import util.LogUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class StudentMainFrame extends JFrame {

    // 依赖项
    private final CourseService courseService = new CourseService();
    private final User student; // 当前登录的学生对象

    // 主界面组件
    private JTabbedPane tabbedPane;
    
    // 第一个标签页：选课功能组件
    private JTable courseTable;
    private DefaultTableModel courseTableModel;
    private TableRowSorter<DefaultTableModel> courseSorter;
    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private JComboBox<String> creditCombo;
    private JComboBox<String> teacherCombo;
    
    // 第二个标签页：课表功能组件
    private JTable selectedCourseTable;
    private DefaultTableModel selectedCourseTableModel;
    private List<Course> allCourses = new ArrayList<>();
    
    // 表格列名
    private static final String[] COURSE_COLUMNS = {
            "课程代码", "课程名称", "学分", "授课教师", "上课时间", "容量", "已选人数", "剩余名额"
    };
    
    private static final String[] SELECTED_COURSE_COLUMNS = {
            "课程代码", "课程名称", "学分", "授课教师", "上课时间", "上课地点", "操作"
    };

    /**
     * 构造函数：初始化界面
     */
    public StudentMainFrame(User student) {
        this.student = student;
        setTitle("选课系统 - 学生端 - 欢迎：" + student.getUserName());
        setSize(950, 700);  // 增大窗口尺寸
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 【添加日志】学生进入系统
        LogUtil.log(student.getUserId(), "进入学生系统", 
                   "学生 " + student.getUserName() + "(" + student.getUserId() + ") 进入学生主界面");

        setLayout(new BorderLayout());

        // 创建标签页
        tabbedPane = new JTabbedPane();
        
        // 1. 可选课程标签页（原来的选课功能）
        tabbedPane.addTab("可选课程", createCourseSelectionPanel());
        
        // 2. 我的课表标签页（新增功能）
        tabbedPane.addTab("我的课表", createMySchedulePanel());
        
        // 3. 已选课程标签页（新增功能）
        tabbedPane.addTab("已选课程", createSelectedCoursesPanel());

        add(tabbedPane, BorderLayout.CENTER);
        
        // 初始加载数据
        loadAllCourseData();
        setVisible(true);
    }

    // ===================================
    // 1. 创建"可选课程"面板（原选课功能）
    // ===================================
    private JPanel createCourseSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 1. 搜索筛选面板
        JPanel searchPanel = createSearchPanel();
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // 2. 课程表格
        courseTableModel = new DefaultTableModel(null, COURSE_COLUMNS) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) return Double.class; // 学分列
                if (columnIndex == 5 || columnIndex == 6 || columnIndex == 7) return Integer.class;
                return String.class;
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };
        courseTable = new JTable(courseTableModel);
        courseSorter = new TableRowSorter<>(courseTableModel);
        courseTable.setRowSorter(courseSorter);
        
        // 设置列宽
        courseTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        courseTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        courseTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        courseTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        courseTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        
        JScrollPane scrollPane = new JScrollPane(courseTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 3. 按钮面板
        JPanel buttonPanel = new JPanel();
        JButton selectBtn = new JButton("选课");
        JButton refreshBtn = new JButton("刷新");
        
        selectBtn.addActionListener(e -> handleSelectCourse());
        refreshBtn.addActionListener(e -> {
            loadAllCourseData();
            displayCourses(allCourses);
        });
        
        buttonPanel.add(selectBtn);
        buttonPanel.add(refreshBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * 创建搜索筛选面板
     */
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("课程搜索与筛选"));
        
        // 搜索关键词
        panel.add(new JLabel("搜索:"));
        searchField = new JTextField(15);
        panel.add(searchField);
        
        // 搜索类型
        panel.add(new JLabel("搜索类型:"));
        filterCombo = new JComboBox<>(new String[]{"全部", "课程名称", "课程代码", "授课教师"});
        panel.add(filterCombo);
        
        // 学分筛选
        panel.add(new JLabel("学分筛选:"));
        creditCombo = new JComboBox<>(new String[]{"全部", "1学分", "2学分", "3学分", "4学分", "5学分以上"});
        panel.add(creditCombo);
        
        // 教师筛选
        panel.add(new JLabel("教师筛选:"));
        teacherCombo = new JComboBox<>(new String[]{"全部教师"});
        panel.add(teacherCombo);
        
        // 按钮
        JButton searchBtn = new JButton("搜索");
        JButton resetBtn = new JButton("重置");
        searchBtn.addActionListener(e -> handleSearch());
        resetBtn.addActionListener(e -> handleReset());
        
        panel.add(searchBtn);
        panel.add(resetBtn);
        
        return panel;
    }

    /**
     * 加载所有课程数据并缓存
     */
    private void loadAllCourseData() {
        allCourses = courseService.getPublishedCourses();
        
        // 【添加日志】查看课程列表
        LogUtil.log(student.getUserId(), "查看课程列表", 
                   "学生 " + student.getUserName() + " 查看可选课程，共 " + allCourses.size() + " 门");
        
        // 更新教师筛选下拉框
        updateTeacherFilter();
    }

    /**
     * 更新教师筛选下拉框（去重）
     */
    private void updateTeacherFilter() {
        List<String> teachers = new ArrayList<>();
        teachers.add("全部教师");
        
        for (Course course : allCourses) {
            String teacher = course.getTeacherName();
            if (teacher != null && !teacher.isEmpty() && !teachers.contains(teacher)) {
                teachers.add(teacher);
            }
        }
        
        teacherCombo.setModel(new DefaultComboBoxModel<>(teachers.toArray(new String[0])));
    }

    /**
     * 将课程列表显示到表格
     */
    private void displayCourses(List<Course> courses) {
        courseTableModel.setRowCount(0);
        
        for (Course course : courses) {
            Object[] rowData = new Object[] {
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getCredit(),
                    course.getTeacherName(),
                    course.getScheduleTime(),
                    course.getCapacityLimit(),
                    course.getCurrentSelected(),
                    course.getRemainingCapacity()
            };
            courseTableModel.addRow(rowData);
        }

        if (courses.isEmpty()) {
            courseTableModel.addRow(new Object[]{"无符合条件的课程", "", "", "", "", "", "", ""});
        }
    }

    /**
     * 处理搜索按钮点击事件
     */
    private void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        String filterType = (String) filterCombo.getSelectedItem();
        String creditFilter = (String) creditCombo.getSelectedItem();
        String teacherFilter = (String) teacherCombo.getSelectedItem();
        
        List<Course> filteredCourses = new ArrayList<>();
        
        for (Course course : allCourses) {
            boolean matches = true;
            
            // 1. 关键词搜索
            if (!keyword.isEmpty()) {
                boolean keywordMatch = false;
                switch (filterType) {
                    case "课程名称":
                        keywordMatch = course.getCourseName().toLowerCase().contains(keyword);
                        break;
                    case "课程代码":
                        keywordMatch = course.getCourseCode().toLowerCase().contains(keyword);
                        break;
                    case "授课教师":
                        keywordMatch = course.getTeacherName().toLowerCase().contains(keyword);
                        break;
                    default: // "全部"
                        keywordMatch = course.getCourseName().toLowerCase().contains(keyword) ||
                                      course.getCourseCode().toLowerCase().contains(keyword) ||
                                      course.getTeacherName().toLowerCase().contains(keyword);
                        break;
                }
                matches = matches && keywordMatch;
            }
            
            // 2. 学分筛选
            if (!"全部".equals(creditFilter) && matches) {
                double courseCredit = course.getCredit();
                switch (creditFilter) {
                    case "1学分":
                        matches = courseCredit == 1.0;
                        break;
                    case "2学分":
                        matches = courseCredit == 2.0;
                        break;
                    case "3学分":
                        matches = courseCredit == 3.0;
                        break;
                    case "4学分":
                        matches = courseCredit == 4.0;
                        break;
                    case "5学分以上":
                        matches = courseCredit >= 5.0;
                        break;
                }
            }
            
            // 3. 教师筛选
            if (!"全部教师".equals(teacherFilter) && matches) {
                matches = course.getTeacherName().equals(teacherFilter);
            }
            
            if (matches) {
                filteredCourses.add(course);
            }
        }
        
        // 显示筛选结果
        displayCourses(filteredCourses);
        
        // 【添加日志】搜索课程
        LogUtil.log(student.getUserId(), "搜索课程", 
                   "学生 " + student.getUserName() + " 搜索课程，关键词: " + keyword + 
                   ", 类型: " + filterType + ", 找到 " + filteredCourses.size() + " 门课程");
    }

    /**
     * 处理重置按钮点击事件
     */
    private void handleReset() {
        searchField.setText("");
        filterCombo.setSelectedIndex(0);
        creditCombo.setSelectedIndex(0);
        teacherCombo.setSelectedIndex(0);
        displayCourses(allCourses);
        
        // 【添加日志】重置搜索
        LogUtil.log(student.getUserId(), "重置搜索", "学生 " + student.getUserName() + " 重置搜索条件");
    }

    /**
     * 处理选课按钮点击事件
     */
    private void handleSelectCourse() {
        int selectedRow = courseTable.getSelectedRow();
        
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先在表格中选择一门课程。", "操作失败", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 由于可能有排序，需要转换行索引
        int modelRow = courseTable.convertRowIndexToModel(selectedRow);
        String courseCode = (String) courseTableModel.getValueAt(modelRow, 0);
        String courseName = (String) courseTableModel.getValueAt(modelRow, 1);
        
        // 检查是否已经选了这门课
        if (isCourseAlreadySelected(courseCode)) {
            JOptionPane.showMessageDialog(this, 
                "您已经选择了课程《" + courseName + "》，不能重复选课！",
                "重复选课", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要选择课程：《" + courseName + "》吗？",
                "确认选课", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            String result = courseService.selectCourse(student.getUserId(), courseCode);
            
            // 记录选课日志
            if (result.contains("成功")) {
                LogUtil.log(student.getUserId(), "选课成功", 
                    "选择课程：" + courseName + "(" + courseCode + ")");
                JOptionPane.showMessageDialog(this, result, "选课结果", JOptionPane.INFORMATION_MESSAGE);
                
                // 【关键修复】刷新所有页面的数据
                refreshAllData();
                
                // 自动切换到课表页面
                tabbedPane.setSelectedIndex(1);
                
            } else {
                LogUtil.log(student.getUserId(), "选课失败", 
                    "尝试选择课程：" + courseName + "，原因：" + result);
                JOptionPane.showMessageDialog(this, result, "选课失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 刷新所有页面的数据
     */
    private void refreshAllData() {
        // 1. 刷新"可选课程"页面
        loadAllCourseData();
        displayCourses(allCourses);
        
        // 2. 刷新"我的课表"页面
        refreshSchedulePanel();
        
        // 3. 刷新"已选课程"页面
        loadSelectedCourses();
        
        // 记录日志
        LogUtil.log(student.getUserId(), "刷新数据", "刷新所有页面数据");
    }
    /**
 * 刷新课表面板数据
 */
    private void refreshSchedulePanel() {
        // 获取当前选中的标签页索引
        int currentTab = tabbedPane.getSelectedIndex();
        
        // 如果课表面板存在，刷新它
        if (tabbedPane.getTabCount() > 1) {
            Component schedulePanel = tabbedPane.getComponentAt(1);
            if (schedulePanel instanceof JPanel) {
                // 重新创建课表面板
                tabbedPane.setComponentAt(1, createMySchedulePanel());
            }
        }
        
        // 如果已选课程面板存在，刷新它
        if (tabbedPane.getTabCount() > 2) {
            Component selectedPanel = tabbedPane.getComponentAt(2);
            if (selectedPanel instanceof JPanel) {
                // 重新创建已选课程面板
                tabbedPane.setComponentAt(2, createSelectedCoursesPanel());
            }
        }
        
        // 恢复原来的标签页
        tabbedPane.setSelectedIndex(currentTab);
    }

/**
 * 退课成功后也要刷新所有数据
 */
    private void handleDropCourse() {
        int selectedRow = selectedCourseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要退选的课程。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = selectedCourseTable.convertRowIndexToModel(selectedRow);
        String courseCode = (String) selectedCourseTableModel.getValueAt(modelRow, 0);
        String courseName = (String) selectedCourseTableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要退选课程：《" + courseName + "》吗？",
                "确认退课", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String result = courseService.dropCourse(student.getUserId(), courseCode);

            if (result.contains("成功")) {
                LogUtil.log(student.getUserId(), "退课成功", 
                        "学生 " + student.getUserName() + " 成功退选课程：" + courseName + "(" + courseCode + ")");
                JOptionPane.showMessageDialog(this, result, "退课成功", JOptionPane.INFORMATION_MESSAGE);
                
                // 【关键修复】刷新所有数据
                refreshAllData();
                
            } else {
                LogUtil.log(student.getUserId(), "退课失败", 
                        "学生 " + student.getUserName() + " 退课失败：" + courseName + "，原因：" + result);
                JOptionPane.showMessageDialog(this, result, "退课失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 检查是否已经选了这门课
     */
    private boolean isCourseAlreadySelected(String courseCode) {
        return courseService.isCourseSelected(student.getUserId(), courseCode);
    }

    // ===================================
    // 2. 创建"我的课表"面板（新增功能）
    // ===================================
    private JPanel createMySchedulePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 1. 统计信息面板
        JPanel statsPanel = createStatsPanel();
        panel.add(statsPanel, BorderLayout.NORTH);
        
        // 2. 周课表视图
        JPanel schedulePanel = createWeeklySchedulePanel();
        panel.add(schedulePanel, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * 创建统计信息面板
     */
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setBorder(BorderFactory.createTitledBorder("学习概况"));
        panel.setPreferredSize(new Dimension(0, 100));
        
        // 获取统计信息（简化版）
        List<Course> selectedCourses = courseService.getStudentSelectedCourses(student.getUserId());
        int courseCount = selectedCourses.size();
        double totalCredits = 0;
        for (Course course : selectedCourses) {
            totalCredits += course.getCredit();
        }
        
        // 创建统计卡片
        panel.add(createStatCard("已选课程", courseCount + "门", Color.decode("#4CAF50")));
        panel.add(createStatCard("总学分", String.format("%.1f", totalCredits) + "学分", Color.decode("#2196F3")));
        panel.add(createStatCard("剩余学分", String.format("%.1f", 30 - totalCredits) + "学分", Color.decode("#FF9800")));
        panel.add(createStatCard("平均学分", String.format("%.1f", courseCount > 0 ? totalCredits / courseCount : 0) + "学分", Color.decode("#9C27B0")));
        
        return panel;
    }

    /**
     * 创建统计卡片
     */
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        titleLabel.setForeground(Color.GRAY);
        
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        valueLabel.setForeground(color);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }

/**
 * 创建基于实际课程时间的课表面板
 */
private JPanel createWeeklySchedulePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("本周课表（时间网格视图）"));
    
    // 获取学生已选课程
    List<Course> selectedCourses = courseService.getStudentSelectedCourses(student.getUserId());
    
    // 创建课表网格 - 7行6列（6个时间段）
    JPanel gridPanel = new JPanel(new GridLayout(7, 6, 1, 1));
    gridPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    
    // 表头行
    String[] timeSlots = {"时间", "周一", "周二", "周三", "周四", "周五"};
    String[] periods = {
        "1-2节\n8:00-10:00", 
        "3-4节\n10:10-12:10", 
        "午休", 
        "5-6节\n14:00-16:00", 
        "7-8节\n16:10-18:10", 
        "9-10节\n19:00-21:00"
    };
    
    // 添加表头
    for (int i = 0; i < 6; i++) {
        JLabel header = new JLabel(timeSlots[i], SwingConstants.CENTER);
        header.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        header.setBackground(Color.LIGHT_GRAY);
        header.setOpaque(true);
        gridPanel.add(header);
    }
    
    // 初始化课表数据
    String[][] scheduleGrid = new String[6][5]; // 6个时间段 × 5天
    
    // 填充课表数据
    for (Course course : selectedCourses) {
        int[] timeInfo = parseScheduleTime(course.getScheduleTime());
        int day = timeInfo[0];      // 星期几 (0-4)
        int period = timeInfo[1];   // 时间段 (0-5)
        
        if (day >= 0 && day < 5 && period >= 0 && period < 6) {
            String courseName = course.getCourseName();
            if (courseName.length() > 4) {
                courseName = courseName.substring(0, Math.min(4, courseName.length()));
            }
            scheduleGrid[period][day] = courseName;
        }
    }
    
    // 创建课表格子
    for (int period = 0; period < 6; period++) {
        // 时间标签
        JLabel timeLabel = new JLabel("<html><center>" + periods[period].replace("\n", "<br>") + "</center></html>", SwingConstants.CENTER);
        timeLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        timeLabel.setBackground(Color.decode("#F5F5F5"));
        timeLabel.setOpaque(true);
        gridPanel.add(timeLabel);
        
        // 周一到周五的课程格子
        for (int day = 0; day < 5; day++) {
            JLabel cell = new JLabel("", SwingConstants.CENTER);
            cell.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            cell.setOpaque(true);
            
            String courseName = scheduleGrid[period][day];
            if (courseName != null && !courseName.isEmpty()) {
                cell.setText("<html><center>" + courseName + "</center></html>");
                cell.setBackground(Color.decode("#C8E6C9")); // 浅绿色表示有课
                cell.setForeground(Color.DARK_GRAY);
                cell.setToolTipText("点击查看详情");
            } else if (period == 2) { // 午休
                cell.setText("午休");
                cell.setBackground(Color.decode("#FFF3E0"));
            } else {
                cell.setBackground(Color.WHITE);
            }
            
            gridPanel.add(cell);
        }
    }
    
    panel.add(gridPanel, BorderLayout.CENTER);
    
    // 添加统计信息
    JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    infoPanel.add(new JLabel("已选课程: " + selectedCourses.size() + "门"));
    infoPanel.add(new JLabel(" | 课表显示: " + countCoursesOnSchedule(scheduleGrid) + "门"));
    panel.add(infoPanel, BorderLayout.SOUTH);
    
    return panel;
}
/**
 * 解析课程时间字符串，返回星期和时间段
 * 格式示例："周一1-2节"、"周二3-4节"、"周三5-6节"、"周四7-8节"、"周五9-10节"
 * 
 * @param scheduleTime 课程时间字符串
 * @return int[2] 数组，[0]=星期几(0-4)，[1]=时间段(0-5)
 */
private int[] parseScheduleTime(String scheduleTime) {
    // 默认值：不在课表上显示（-1表示无效）
    int[] result = new int[]{-1, -1};
    
    if (scheduleTime == null || scheduleTime.trim().isEmpty()) {
        return result;
    }
    
    try {
        String timeStr = scheduleTime.trim();
        
        // 1. 提取星期几
        if (timeStr.contains("周一")) {
            result[0] = 0; // 周一
        } else if (timeStr.contains("周二")) {
            result[0] = 1; // 周二
        } else if (timeStr.contains("周三")) {
            result[0] = 2; // 周三
        } else if (timeStr.contains("周四")) {
            result[0] = 3; // 周四
        } else if (timeStr.contains("周五")) {
            result[0] = 4; // 周五
        } else if (timeStr.contains("周六")) {
            result[0] = 5; // 周六
        } else if (timeStr.contains("周日")) {
            result[0] = 6; // 周日
        }
        
        // 2. 提取时间段（节次）
        if (timeStr.contains("1-2节") || timeStr.contains("1、2节") || timeStr.contains("1,2节")) {
            result[1] = 0; // 第1-2节（8:00-10:00）
        } else if (timeStr.contains("3-4节") || timeStr.contains("3、4节") || timeStr.contains("3,4节")) {
            result[1] = 1; // 第3-4节（10:10-12:10）
        } else if (timeStr.contains("5-6节") || timeStr.contains("5、6节") || timeStr.contains("5,6节")) {
            result[1] = 3; // 第5-6节（14:00-16:00）（注意：索引3对应午休之后）
        } else if (timeStr.contains("7-8节") || timeStr.contains("7、8节") || timeStr.contains("7,8节")) {
            result[1] = 4; // 第7-8节（16:10-18:10）
        } else if (timeStr.contains("9-10节") || timeStr.contains("9、10节") || timeStr.contains("9,10节")) {
            result[1] = 5; // 第9-10节（19:00-21:00）
        } else if (timeStr.contains("11-12节")) {
            result[1] = 6; // 第11-12节（如果有的话）
        }
        
        // 3. 处理其他格式（如果上面的没匹配到）
        if (result[1] == -1) {
            // 尝试匹配 "1-2" 这样的格式（不带"节"字）
            if (timeStr.contains("1-2") || timeStr.matches(".*1\\s*[-~]\\s*2.*")) {
                result[1] = 0;
            } else if (timeStr.contains("3-4") || timeStr.matches(".*3\\s*[-~]\\s*4.*")) {
                result[1] = 1;
            } else if (timeStr.contains("5-6") || timeStr.matches(".*5\\s*[-~]\\s*6.*")) {
                result[1] = 3;
            } else if (timeStr.contains("7-8") || timeStr.matches(".*7\\s*[-~]\\s*8.*")) {
                result[1] = 4;
            } else if (timeStr.contains("9-10") || timeStr.matches(".*9\\s*[-~]\\s*10.*")) {
                result[1] = 5;
            }
        }
        
    } catch (Exception e) {
        System.err.println("解析课程时间失败 [" + scheduleTime + "]: " + e.getMessage());
        e.printStackTrace();
    }
    
    return result;
}
/**
 * 查找指定时间段的课程（使用实际课程时间）
 */
private String findCourseAt(List<Course> courses, int day, int period) {
    for (Course course : courses) {
        // 解析课程的schedule_time
        int[] timeInfo = parseScheduleTime(course.getScheduleTime());
        
        // 调试输出（可选）
        if (timeInfo[0] >= 0 && timeInfo[1] >= 0) {
            System.out.println("课程解析: " + course.getCourseName() + 
                             " -> 星期" + (timeInfo[0] + 1) + 
                             " 第" + (timeInfo[1] < 2 ? timeInfo[1] + 1 : timeInfo[1] + 2) + "节");
        }
        
        // 检查是否匹配指定的day和period
        // 注意：午休占用了索引2，所以时间段索引需要调整
        if (timeInfo[0] == day && timeInfo[1] == period) {
            // 返回课程简称（避免单元格显示过长）
            String courseName = course.getCourseName();
            if (courseName.length() > 4) {
                // 取前4个字
                courseName = courseName.substring(0, Math.min(4, courseName.length()));
            }
            return courseName;
        }
    }
    return "";
}
/**
 * 计算课表上显示的课程数量
 */
private int countCoursesOnSchedule(String[][] scheduleGrid) {
    int count = 0;
    for (int i = 0; i < 6; i++) {
        for (int j = 0; j < 5; j++) {
            if (scheduleGrid[i][j] != null && !scheduleGrid[i][j].isEmpty()) {
                count++;
            }
        }
    }
    return count;
}

    /**
     * 创建图例项
     */
    private JPanel createLegendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        JLabel colorBox = new JLabel("   ");
        colorBox.setOpaque(true);
        colorBox.setBackground(color);
        colorBox.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        item.add(colorBox);
        item.add(new JLabel(text));
        
        return item;
    }

    // ===================================
    // 3. 创建"已选课程"面板（新增功能）
    // ===================================
    private JPanel createSelectedCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 1. 表格
        selectedCourseTableModel = new DefaultTableModel(null, SELECTED_COURSE_COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // 只有操作列可编辑
            }
        };
        selectedCourseTable = new JTable(selectedCourseTableModel);
        
        // 设置列宽
        selectedCourseTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        selectedCourseTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        selectedCourseTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        selectedCourseTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        
        // 为操作列设置渲染器和编辑器
        selectedCourseTable.getColumn("操作").setCellRenderer(new ButtonRenderer());
        selectedCourseTable.getColumn("操作").setCellEditor(new ButtonEditor(new JCheckBox(), selectedCourseTable, student));
        
        JScrollPane scrollPane = new JScrollPane(selectedCourseTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 2. 按钮面板
        JPanel buttonPanel = new JPanel();
        JButton dropBtn = new JButton("退选课程");
        JButton refreshBtn = new JButton("刷新列表");
        
        dropBtn.addActionListener(e -> handleDropCourse());
        refreshBtn.addActionListener(e -> loadSelectedCourses());
        
        buttonPanel.add(dropBtn);
        buttonPanel.add(refreshBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 加载已选课程数据
        loadSelectedCourses();
        
        return panel;
    }

    /**
     * 加载已选课程数据
     */
    private void loadSelectedCourses() {
        selectedCourseTableModel.setRowCount(0);
        List<Course> selectedCourses = courseService.getStudentSelectedCourses(student.getUserId());
        
        for (Course course : selectedCourses) {
            Object[] rowData = new Object[] {
                course.getCourseCode(),
                course.getCourseName(),
                course.getCredit(),
                course.getTeacherName(),
                course.getScheduleTime(),
                course.getClassroom(),
                "查看详情"
            };
            selectedCourseTableModel.addRow(rowData);
        }
        
        if (selectedCourses.isEmpty()) {
            selectedCourseTableModel.addRow(new Object[]{"暂无已选课程", "", "", "", "", "", ""});
        }
    }

}