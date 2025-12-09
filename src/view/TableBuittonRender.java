package view;

import model.User;
import service.CourseService;
import util.LogUtil;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 表格按钮渲染器
 */
class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        setText((value == null) ? "" : value.toString());
        return this;
    }
}

/**
 * 表格按钮编辑器
 */
class ButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String label;
    private boolean isPushed;
    private JTable table;
    private User student;
    private CourseService courseService;
    
    public ButtonEditor(JCheckBox checkBox, JTable table, User student) {
        super(checkBox);
        this.table = table;
        this.student = student;
        this.courseService = new CourseService();
        
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
                handleButtonClick();
            }
        });
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        label = (value == null) ? "" : value.toString();
        button.setText(label);
        isPushed = true;
        return button;
    }
    
    @Override
    public Object getCellEditorValue() {
        if (isPushed) {
            // 执行按钮点击后的操作
        }
        isPushed = false;
        return label;
    }
    
    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }
    
    /**
     * 处理查看详情按钮点击
     */
    private void handleButtonClick() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int modelRow = table.convertRowIndexToModel(row);
            String courseCode = (String) table.getModel().getValueAt(modelRow, 0);
            String courseName = (String) table.getModel().getValueAt(modelRow, 1);
            
            // 获取课程详情
            showCourseDetailDialog(courseCode, courseName);
        }
    }
    
    /**
     * 显示课程详情对话框
     */
    private void showCourseDetailDialog(String courseCode, String courseName) {
        // 获取课程详细信息
        java.util.List<model.Course> allCourses = courseService.getPublishedCourses();
        model.Course courseDetail = null;
        
        for (model.Course course : allCourses) {
            if (course.getCourseCode().equals(courseCode)) {
                courseDetail = course;
                break;
            }
        }
        
        if (courseDetail == null) {
            JOptionPane.showMessageDialog(table, "未找到课程详细信息", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 创建详情面板
        JPanel panel = new JPanel(new GridLayout(8, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        panel.add(new JLabel("课程代码:"));
        panel.add(new JLabel(courseDetail.getCourseCode()));
        panel.add(new JLabel("课程名称:"));
        panel.add(new JLabel(courseDetail.getCourseName()));
        panel.add(new JLabel("授课教师:"));
        panel.add(new JLabel(courseDetail.getTeacherName()));
        panel.add(new JLabel("学    分:"));
        panel.add(new JLabel(String.valueOf(courseDetail.getCredit())));
        panel.add(new JLabel("上课时间:"));
        panel.add(new JLabel(courseDetail.getScheduleTime()));
        panel.add(new JLabel("上课地点:"));
        panel.add(new JLabel(courseDetail.getClassroom() != null ? courseDetail.getClassroom() : "未指定"));
        panel.add(new JLabel("课程容量:"));
        panel.add(new JLabel(courseDetail.getCapacityLimit() + "人"));
        panel.add(new JLabel("已选人数:"));
        panel.add(new JLabel(courseDetail.getCurrentSelected() + "人"));
        
        JOptionPane.showMessageDialog(table, panel, "课程详情 - " + courseName, JOptionPane.INFORMATION_MESSAGE);
        
        // 记录查看日志
        LogUtil.log(student.getUserId(), "查看课程详情", 
                   "查看课程 " + courseName + "(" + courseCode + ") 的详细信息");
    }
}