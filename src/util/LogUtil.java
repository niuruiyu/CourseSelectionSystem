package util;

import service.LogService;
import java.sql.Timestamp;

/**
 * 系统日志工具类
 * 提供统一的日志记录接口，避免在各个类中重复实例化 LogService
 */
public class LogUtil {
    
    // 私有化构造方法，防止实例化（工具类通常不需要实例化）
    private LogUtil() {}
    
    // 静态的 LogService 实例
    private static final LogService logService = new LogService();
    
    /**
     * 同步记录日志（会阻塞当前线程直到日志记录完成）
     */
    public static void logSync(String operatorId, String operationType, String operationContent) {
        try {
            logService.logOperation(operatorId, operationType, operationContent);
        } catch (Exception e) {
            // 日志记录失败不影响主业务流程
            System.err.println("【日志记录失败】" + new Timestamp(System.currentTimeMillis()) + 
                             " - " + operatorId + " - " + operationType + " - " + operationContent);
        }
    }
    
    /**
     * 异步记录日志（推荐使用，不阻塞主线程）
     */
    public static void logAsync(String operatorId, String operationType, String operationContent) {
        new Thread(() -> {
            logSync(operatorId, operationType, operationContent);
        }).start();
    }
    
    /**
     * 简化的日志方法（默认异步）
     */
    public static void log(String operatorId, String operationType, String operationContent) {
        logAsync(operatorId, operationType, operationContent);
    }
    
    /**
     * 记录登录日志的便捷方法
     */
    public static void logLogin(String userId, String userName, boolean success) {
        String type = success ? "用户登录" : "登录失败";
        String content = success ? 
            userName + "(" + userId + ") 登录系统" : 
            "用户 " + userId + " 登录失败";
        
        log(userId, type, content);
    }
    
    /**
     * 记录登出日志的便捷方法
     */
    public static void logLogout(String userId, String userName) {
        log(userId, "用户登出", userName + "(" + userId + ") 退出系统");
    }
}