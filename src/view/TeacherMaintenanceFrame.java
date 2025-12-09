package view;

import model.User;
import service.UserService;
import util.LogUtil;  // 导入日志工具类
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * 教师信息维护窗口（支持增删改查）
 */
public class TeacherMaintenanceFrame extends JFrame {
    private final UserService userService = new UserService();
    private DefaultTableModel tableModel;
    private JTable teacherTable;

    public TeacherMaintenanceFrame() {
        setTitle("教师信息维护");
        setSize(700, 500);
        setLocationRelativeTo(null); // 居中显示
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        //   【添加日志】打开教师维护窗口
        LogUtil.log("管理员", "打开教师维护", "打开教师信息维护窗口");
        
        initUI();
        loadTeacherData();
        setVisible(true);
    }

    // 初始化UI组件
    private void initUI() {
        // 1. 表格模型（工号、姓名、院系、联系方式）
        String[] columnNames = {"教师工号", "姓名", "所属学院", "联系方式"};
        tableModel = new DefaultTableModel(null, columnNames);
        teacherTable = new JTable(tableModel);
        teacherTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 单选

        // 2. 按钮面板
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("新增教师");
        JButton updateBtn = new JButton("修改信息");
        JButton deleteBtn = new JButton("删除教师");

        // 绑定事件
        addBtn.addActionListener(e -> showAddTeacherDialog());
        updateBtn.addActionListener(e -> showUpdateTeacherDialog());
        deleteBtn.addActionListener(e -> handleDeleteTeacher());

        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);

        // 3. 布局
        add(new JScrollPane(teacherTable), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    // 加载教师数据到表格
    private void loadTeacherData() {
        tableModel.setRowCount(0); // 清空表格
        List<User> teachers = userService.getAllTeachers();
        
        //   【添加日志】加载教师数据
        LogUtil.log("管理员", "加载教师数据", "加载教师列表，共 " + teachers.size() + " 名教师");
        
        for (User teacher : teachers) {
            // 从User对象中获取数据（需User类支持contact字段的getter）
            tableModel.addRow(new Object[]{
                    teacher.getUserId(),
                    teacher.getUserName(),
                    teacher.getDepartment(),
                    teacher.getContact() // 需在User类中补充contact的getter/setter
            });
        }
    }

    // 新增教师对话框
    private void showAddTeacherDialog() {
        // 输入表单
        JTextField idField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JTextField deptField = new JTextField(15);
        JTextField contactField = new JTextField(15);
        JTextField accountField = new JTextField(15);
        JPasswordField pwdField = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("教师工号:"));
        panel.add(idField);
        panel.add(new JLabel("姓名:"));
        panel.add(nameField);
        panel.add(new JLabel("所属学院:"));
        panel.add(deptField);
        panel.add(new JLabel("联系方式:"));
        panel.add(contactField);
        panel.add(new JLabel("登录账号:"));
        panel.add(accountField);
        panel.add(new JLabel("登录密码:"));
        panel.add(pwdField);

        int result = JOptionPane.showConfirmDialog(this, panel, "新增教师", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            // 验证必填项
            String userId = idField.getText().trim();
            String userName = nameField.getText().trim();
            String account = accountField.getText().trim();
            String password = new String(pwdField.getPassword()).trim();

            if (userId.isEmpty() || userName.isEmpty() || account.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "工号、姓名、账号、密码为必填项", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 封装用户对象
            User teacher = new User(userId, userName, "Teacher");
            teacher.setDepartment(deptField.getText().trim());
            teacher.setContact(contactField.getText().trim());

            // 调用服务层添加
            boolean success = userService.addTeacher(teacher, account, password);
            
            //   【添加日志】新增教师
            if (success) {
                LogUtil.log("管理员", "新增教师成功", 
                           "新增教师：" + userName + "(" + userId + ")，学院：" + deptField.getText().trim() + 
                           "，账号：" + account);
                JOptionPane.showMessageDialog(this, "新增成功");
                loadTeacherData(); // 刷新表格
            } else {
                LogUtil.log("管理员", "新增教师失败", 
                           "尝试新增教师：" + userName + "(" + userId + ") 失败，可能工号或账号重复");
                JOptionPane.showMessageDialog(this, "新增失败（可能工号或账号重复）", "操作失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 修改教师信息对话框
    private void showUpdateTeacherDialog() {
        int selectedRow = teacherTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请选择要修改的教师", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 获取选中行数据
        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        String userName = (String) tableModel.getValueAt(selectedRow, 1);
        String dept = (String) tableModel.getValueAt(selectedRow, 2);
        String contact = (String) tableModel.getValueAt(selectedRow, 3);

        // 填充到输入框
        JTextField nameField = new JTextField(userName, 15);
        JTextField deptField = new JTextField(dept, 15);
        JTextField contactField = new JTextField(contact, 15);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("姓名:"));
        panel.add(nameField);
        panel.add(new JLabel("所属学院:"));
        panel.add(deptField);
        panel.add(new JLabel("联系方式:"));
        panel.add(contactField);

        int result = JOptionPane.showConfirmDialog(this, panel, "修改教师信息（工号不可改）", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            // 封装修改后的数据
            User updatedTeacher = new User(userId, nameField.getText().trim(), "Teacher");
            updatedTeacher.setDepartment(deptField.getText().trim());
            updatedTeacher.setContact(contactField.getText().trim());

            boolean success = userService.updateTeacher(updatedTeacher);
            
            //   【添加日志】修改教师信息
            if (success) {
                LogUtil.log("管理员", "修改教师信息", 
                           "修改教师 " + userId + " 的信息，" + 
                           "原姓名：" + userName + "，新姓名：" + nameField.getText().trim() + "，" +
                           "新学院：" + deptField.getText().trim());
                JOptionPane.showMessageDialog(this, "修改成功");
                loadTeacherData();
            } else {
                LogUtil.log("管理员", "修改教师信息失败", 
                           "修改教师 " + userId + " 的信息失败");
                JOptionPane.showMessageDialog(this, "修改失败", "操作失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 删除教师
    private void handleDeleteTeacher() {
        int selectedRow = teacherTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请选择要删除的教师", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        String userName = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除教师【" + userName + "（" + userId + "）】吗？\n删除后关联课程可能失效！",
                "危险操作确认", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = userService.deleteTeacher(userId);
            
            //   【添加日志】删除教师
            if (success) {
                LogUtil.log("管理员", "删除教师成功", 
                           "删除教师：" + userName + "(" + userId + ")");
                JOptionPane.showMessageDialog(this, "删除成功");
                loadTeacherData();
            } else {
                LogUtil.log("管理员", "删除教师失败", 
                           "删除教师：" + userName + "(" + userId + ") 失败，可能有关联课程");
                JOptionPane.showMessageDialog(this, "删除失败（可能存在关联课程）", "操作失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}