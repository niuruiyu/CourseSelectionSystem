package model;

import java.sql.Timestamp;

/**
 * 操作日志模型（对应operation_log表）
 */
public class OperationLog {
    private int logId;
    private String operatorId;
    private String operationType;
    private String operationContent;
    private Timestamp operationTime;

    // 构造方法
    public OperationLog(int logId, String operatorId, String operationType, String operationContent, Timestamp operationTime) {
        this.logId = logId;
        this.operatorId = operatorId;
        this.operationType = operationType;
        this.operationContent = operationContent;
        this.operationTime = operationTime;
    }

    // Getter方法（无需Setter，日志只读）
    public int getLogId() { return logId; }
    public String getOperatorId() { return operatorId; }
    public String getOperationType() { return operationType; }
    public String getOperationContent() { return operationContent; }
    public Timestamp getOperationTime() { return operationTime; }
}