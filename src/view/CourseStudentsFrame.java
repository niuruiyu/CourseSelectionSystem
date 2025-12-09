package view;

import model.User;
import service.CourseService;
import util.LogUtil;
import util.CSVExporter;  // 导入CSV导出工具类
import javax.swing.*;
import java.sql.*; 
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 课程选课学生名单查看窗口
 */
public class CourseStudentsFrame extends JFrame {
    
    private final CourseService courseService = new CourseService();
    private DefaultTableModel tableModel;
    private JTable studentTable;  // 将表格定义为成员变量以便导出
    
    public CourseStudentsFrame(String courseCode, String courseName, String teacherId) {
        setTitle("课程选课学生名单 - " + courseName + "(" + courseCode + ")");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // 记录查看学生名单日志
        LogUtil.log(teacherId, "查看课程学生名单", 
                   "查看课程 " + courseName + "(" + courseCode + ") 的选课学生名单");
        
        initUI(courseCode, courseName);
        loadStudentData(courseCode);
        setVisible(true);
    }
    
    // 初始化UI组件
    private void initUI(String courseCode, String courseName) {
        // 1. 顶部信息面板
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("课程代码: " + courseCode));
        infoPanel.add(new JLabel("课程名称: " + courseName));
        
        // 2. 表格模型（学号、姓名、院系）
        String[] columnNames = {"学号", "姓名", "院系", "选课时间"};
        tableModel = new DefaultTableModel(null, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 不可编辑
            }
        };
        studentTable = new JTable(tableModel);  // 保存为成员变量
        
        // 3. 底部按钮面板
        JPanel btnPanel = new JPanel();
        JButton refreshBtn = new JButton("刷新名单");
        JButton exportBtn = new JButton("导出名单");
        
        refreshBtn.addActionListener(e -> loadStudentData(courseCode));
        exportBtn.addActionListener(e -> exportStudentList(courseCode, courseName));
        
        btnPanel.add(refreshBtn);
        btnPanel.add(exportBtn);
        
        // 4. 布局
        add(infoPanel, BorderLayout.NORTH);
        add(new JScrollPane(studentTable), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }
    
    // 加载学生数据
    private void loadStudentData(String courseCode) {
        tableModel.setRowCount(0);
        
        // 调用新方法获取包含时间的数据
        List<Object[]> studentData = courseService.getStudentsWithSelectionTime(courseCode);
        
        if (studentData == null || studentData.isEmpty()) {
            tableModel.addRow(new Object[]{"暂无学生选课", "", "", ""});
            return;
        }
        
        for (Object[] row : studentData) {
            // 格式化时间
            String timeStr = "";
            if (row[3] != null && row[3] instanceof Timestamp) {
                Timestamp ts = (Timestamp) row[3];
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                timeStr = sdf.format(ts);
            }
            
            tableModel.addRow(new Object[]{
                row[0],  // userId
                row[1],  // userName
                row[2],  // department
                timeStr  // 格式化后的时间
            });
        }
    }
    
    // 导出学生名单 - 使用已有的 CSVExporter
    private void exportStudentList(String courseCode, String courseName) {
        if (tableModel.getRowCount() == 0 || 
            "暂无学生选课".equals(tableModel.getValueAt(0, 0))) {
            JOptionPane.showMessageDialog(this,
                "没有学生数据可以导出！",
                "导出失败", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 使用CSVExporter导出当前表格数据
        boolean success = CSVExporter.exportToCSV(studentTable, courseName + "_学生名单");
        
        if (success) {
            // 记录导出日志
            LogUtil.log("教师", "导出学生名单", 
                       "导出课程 " + courseName + "(" + courseCode + 
                       ") 的学生名单，共 " + (tableModel.getRowCount()) + " 名学生");
            
            JOptionPane.showMessageDialog(this,
                "   学生名单导出成功！\n" +
                "课程: " + courseName + "\n" +
                "代码: " + courseCode + "\n" +
                "选课人数: " + (tableModel.getRowCount()) + "\n" +
                "文件包含字段: 学号, 姓名, 院系, 选课时间",
                "导出成功", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "导出失败，请检查文件权限或磁盘空间。",
                "导出失败", JOptionPane.ERROR_MESSAGE);
        }
    }
}