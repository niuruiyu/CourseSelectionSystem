package view;

import model.User;
import service.UserService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentMaintenanceFrame extends JFrame {
    private final UserService userService = new UserService();
    private DefaultTableModel model;

    public StudentMaintenanceFrame() {
        setTitle("学生信息维护");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // 表格初始化
        String[] columns = {"学号", "姓名", "院系"};
        model = new DefaultTableModel(null, columns);
        JTable table = new JTable(model);
        loadStudentData();

        // 按钮面板
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("添加");
        JButton updateBtn = new JButton("修改");
        JButton deleteBtn = new JButton("删除");
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);

        // 绑定事件（示例：添加学生）
        addBtn.addActionListener(e -> showAddStudentDialog());

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void loadStudentData() {
        model.setRowCount(0);
        List<User> students = userService.getAllStudents();
        for (User s : students) {
            model.addRow(new Object[]{s.getUserId(), s.getUserName(), s.getDepartment()});
        }
    }

    private void showAddStudentDialog() {
        // 简单的添加学生对话框（实际需完善输入验证）
        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(10);
        JTextField deptField = new JTextField(10);
        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("学号:"));
        panel.add(idField);
        panel.add(new JLabel("姓名:"));
        panel.add(nameField);
        panel.add(new JLabel("院系:"));
        panel.add(deptField);

        int result = JOptionPane.showConfirmDialog(this, panel, "添加学生", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            User student = new User(idField.getText(), nameField.getText(), "Student");
            student.setDepartment(deptField.getText());
            boolean success = userService.addStudent(student, idField.getText(), "123456"); // 默认密码
            if (success) {
                JOptionPane.showMessageDialog(this, "添加成功");
                loadStudentData();
            } else {
                JOptionPane.showMessageDialog(this, "添加失败");
            }
        }
    }
}