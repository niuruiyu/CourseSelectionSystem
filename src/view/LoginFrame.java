package view;

import model.User;
import service.UserService;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {

    private final UserService userService = new UserService();

    private JTextField accountField = new JTextField(15);
    private JPasswordField passwordField = new JPasswordField(15);
    private JButton loginButton = new JButton("登录");

    public LoginFrame() {
        setTitle("学校选课系统 - 登录");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new java.awt.FlowLayout());

        // 界面布局
        add(new JLabel("账号:"));
        add(accountField);
        add(new JLabel("密码:"));
        add(passwordField);
        add(loginButton);

        // 绑定事件
        loginButton.addActionListener(new LoginActionListener());

        setVisible(true);
    }

    private class LoginActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String account = accountField.getText();
            String password = new String(passwordField.getPassword());

            // 调用业务逻辑服务
            User user = userService.login(account, password);

            if (user != null) {
                JOptionPane.showMessageDialog(LoginFrame.this, user.getUserName() + "，欢迎您！");

                // 登录成功，关闭登录窗口
                LoginFrame.this.dispose();

                // 【核心步骤】根据角色跳转到不同的主界面
                openMainFrame(user);

            } else {
                JOptionPane.showMessageDialog(LoginFrame.this, "账号或密码错误！", "登录失败", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 根据用户角色跳转到不同的主界面
     */
    private void openMainFrame(User user) {
        String role = user.getRole();
        JFrame mainFrame = null; // 用于存储要创建的主界面实例

        if ("Student".equals(role)) {
            // 修正：实际创建并显示学生主界面
            mainFrame = new StudentMainFrame(user);
        } else if ("Teacher".equals(role)) {
            // 修正：实际创建并显示教师主界面
            mainFrame = new TeacherMainFrame(user);
        } else if ("EduAdmin".equals(role)) {
            // 修正：实际创建并显示教务管理员主界面
            // 假设您已将 AdminMainFrame 用于 EduAdmin
            mainFrame = new AdminMainFrame(user);
        } else if ("SysAdmin".equals(role)) {
            // 如果有系统管理员角色，可以创建单独的 SysAdminFrame，这里仍用 AdminMainFrame 示例
            mainFrame = new AdminMainFrame(user);
            mainFrame.setTitle("选课系统 - 系统管理员 - 欢迎：" + user.getUserName());
        } else {
            JOptionPane.showMessageDialog(null, "角色信息异常！", "登录失败", JOptionPane.ERROR_MESSAGE);
            return; // 角色异常则退出
        }

        // 由于 StudentMainFrame, TeacherMainFrame 等构造函数中已包含 setVisible(true)，
        // 且 LoginFrame 在此之前已调用 dispose()，这里不再需要 mainFrame.setVisible(true)。
        // 如果您在主界面构造函数中没有 setVisible(true)，则需要在这里添加。
    }

    public static void main(String[] args) {
        // 设置 LookAndFeel 使界面更美观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 忽略
        }
        new LoginFrame();
    }
}