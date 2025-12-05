package model;

import java.sql.Timestamp;

/**
 * 用户信息模型类（对应数据库 user_info 表）
 * 包含学生、教师、管理员等所有角色的用户信息
 */
public class User {
    // 对应数据库字段
    private String userId;         // 用户ID/学号/工号（主键）
    private String userName;       // 姓名
    private String account;        // 登录账号（唯一）
    private String password;       // 登录密码（注意：仅在必要时使用，避免明文传输）
    private String role;           // 角色：Student/Teacher/EduAdmin/SysAdmin
    private String contact;        // 联系方式（电话等）
    private String department;     // 所属学院/部门
    private Timestamp createTime;  // 创建时间（数据库自动生成）

    // 1. 空构造函数（JSON反序列化、ORM映射等场景需要）
    public User(String userId, String userName, String role) {
        this.userId = userId;
        this.userName = userName;
        this.role = role;
    }

    // 4. Getter 和 Setter 方法（按字段顺序）
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * 密码字段注意事项：
     * 1. 仅在登录验证、密码修改等场景使用
     * 2. 传输时需加密，展示时需隐藏（如用***代替）
     */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    // 创建时间由数据库自动生成，一般不允许手动修改，故只提供getter
    // public void setCreateTime(Timestamp createTime) {
    //     this.createTime = createTime;
    // }

    // 5. 重写toString() 方便调试（隐藏密码细节）
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", account='" + account + '\'' +
                ", role='" + role + '\'' +
                ", contact='" + contact + '\'' +
                ", department='" + department + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}