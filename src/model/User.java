package model;

// 这是一个简单的数据传输对象（DTO），用于在程序中传递用户信息
public class User {
    private String userId;
    private String userName;
    private String account;
    // 密码通常不从数据库读取到模型中，出于安全考虑
    private String role; // 对应数据库中的 ENUM('Student', 'Teacher', 'EduAdmin', 'SysAdmin')
    private String department;

    // 1. 构造函数
    public User(String userId, String userName, String role) {
        this.userId = userId;
        this.userName = userName;
        this.role = role;
    }

    // 2. Getter 和 Setter 方法 (省略部分，你需要在实际项目中全部添加)
    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getRole() {
        return role;
    }

    // 方便调试
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}