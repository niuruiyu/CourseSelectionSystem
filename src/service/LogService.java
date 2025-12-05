package service;

import model.OperationLog;
import util.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志相关数据库操作
 */
public class LogService {

    /**
     * 获取所有操作日志（按时间倒序）
     */
    public List<OperationLog> getAllOperationLogs() {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM operation_log ORDER BY operation_time DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                OperationLog log = new OperationLog(
                        rs.getInt("log_id"),
                        rs.getString("operator_id"),
                        rs.getString("operation_type"),
                        rs.getString("operation_content"),
                        rs.getTimestamp("operation_time")
                );
                logs.add(log);
            }
        } catch (SQLException e) {
            System.err.println("查询操作日志失败：" + e.getMessage());
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return logs;
    }
}