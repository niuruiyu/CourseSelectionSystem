package view;

import model.User;
import service.UserService;
import util.LogUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentMaintenanceFrame extends JFrame {
    private final UserService userService = new UserService();
    private DefaultTableModel model;
    private JTable studentTable;

    public StudentMaintenanceFrame() {
        setTitle("学生信息维护");
        setSize(700, 500);
        setLocationRelativeTo(null); // 居中显示
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // 【添加日志】打开学生维护窗口
        LogUtil.log("管理员", "打开学生维护", "打开学生信息维护窗口");

        initUI();
        loadStudentData();
        setVisible(true);
    }

    // 初始化UI组件
    private void initUI() {
        // 表格模型（学号、姓名、院系）
        String[] columns = {"学号", "姓名", "院系"};
        model = new DefaultTableModel(null, columns);
        studentTable = new JTable(model);
        studentTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 单选

        // 按钮面板
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("添加学生");
        JButton updateBtn = new JButton("修改信息");
        JButton deleteBtn = new JButton("删除学生");

        // 绑定事件
        addBtn.addActionListener(e -> showAddStudentDialog());
        updateBtn.addActionListener(e -> showUpdateStudentDialog());
        deleteBtn.addActionListener(e -> handleDeleteStudent());

        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);

        // 布局
        add(new JScrollPane(studentTable), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void loadStudentData() {
        model.setRowCount(0);
        List<User> students = userService.getAllStudents();
        
        // 【添加日志】加载学生数据
        LogUtil.log("管理员", "加载学生数据", "加载学生列表，共 " + students.size() + " 名学生");

        for (User s : students) {
            model.addRow(new Object[]{s.getUserId(), s.getUserName(), s.getDepartment()});
        }
    }

    // 添加学生对话框
    private void showAddStudentDialog() {
        JTextField idField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JTextField deptField = new JTextField(15);
        JTextField accountField = new JTextField(15); // 新增：登录账号字段
        JPasswordField pwdField = new JPasswordField(15); // 新增：密码字段

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("学号:"));
        panel.add(idField);
        panel.add(new JLabel("姓名:"));
        panel.add(nameField);
        panel.add(new JLabel("院系:"));
        panel.add(deptField);
        panel.add(new JLabel("登录账号:"));
        panel.add(accountField);
        panel.add(new JLabel("登录密码:"));
        panel.add(pwdField);

        int result = JOptionPane.showConfirmDialog(this, panel, "添加学生", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            // 验证必填项
            String userId = idField.getText().trim();
            String userName = nameField.getText().trim();
            String department = deptField.getText().trim();
            String account = accountField.getText().trim();
            String password = new String(pwdField.getPassword()).trim();

            if (userId.isEmpty() || userName.isEmpty() || account.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "学号、姓名、账号、密码为必填项", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            User student = new User(userId, userName, "Student");
            student.setDepartment(department);
            
            boolean success = userService.addStudent(student, account, password);
            
            // 【添加日志】添加学生
            if (success) {
                LogUtil.log("管理员", "添加学生成功", 
                           "添加学生：" + userName + "(" + userId + ")，院系：" + department + "，账号：" + account);
                JOptionPane.showMessageDialog(this, "添加成功");
                loadStudentData();
            } else {
                LogUtil.log("管理员", "添加学生失败", 
                           "尝试添加学生：" + userName + "(" + userId + ") 失败，可能学号或账号重复");
                JOptionPane.showMessageDialog(this, "添加失败（可能学号或账号重复）");
            }
        }
    }

    // 修改学生信息对话框
    private void showUpdateStudentDialog() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请选择要修改的学生", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 获取选中行数据
        String userId = (String) model.getValueAt(selectedRow, 0);
        String userName = (String) model.getValueAt(selectedRow, 1);
        String department = (String) model.getValueAt(selectedRow, 2);

        // 填充到输入框
        JTextField nameField = new JTextField(userName, 15);
        JTextField deptField = new JTextField(department, 15);

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("姓名:"));
        panel.add(nameField);
        panel.add(new JLabel("院系:"));
        panel.add(deptField);

        int result = JOptionPane.showConfirmDialog(this, panel, "修改学生信息（学号不可改）", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            // 封装修改后的数据
            User updatedStudent = new User(userId, nameField.getText().trim(), "Student");
            updatedStudent.setDepartment(deptField.getText().trim());

            boolean success = userService.updateStudent(updatedStudent);
            
            // 【添加日志】修改学生信息
            if (success) {
                LogUtil.log("管理员", "修改学生信息", 
                           "修改学生 " + userId + " 的信息，" + 
                           "原姓名：" + userName + "，新姓名：" + nameField.getText().trim() + "，" +
                           "新院系：" + deptField.getText().trim());
                JOptionPane.showMessageDialog(this, "修改成功");
                loadStudentData();
            } else {
                LogUtil.log("管理员", "修改学生信息失败", 
                           "修改学生 " + userId + " 的信息失败");
                JOptionPane.showMessageDialog(this, "修改失败", "操作失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 删除学生
    private void handleDeleteStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请选择要删除的学生", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) model.getValueAt(selectedRow, 0);
        String userName = (String) model.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除学生【" + userName + "（" + userId + "）】吗？\n删除后选课记录也将被删除！",
                "危险操作确认", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = userService.deleteStudent(userId);
            
            // 【添加日志】删除学生
            if (success) {
                LogUtil.log("管理员", "删除学生成功", 
                           "删除学生：" + userName + "(" + userId + ")");
                JOptionPane.showMessageDialog(this, "删除成功");
                loadStudentData();
            } else {
                LogUtil.log("管理员", "删除学生失败", 
                           "删除学生：" + userName + "(" + userId + ") 失败");
                JOptionPane.showMessageDialog(this, "删除失败（可能有关联选课记录）", "操作失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}