package view;

import model.OperationLog;
import service.LogService;
import util.DBUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 系统操作日志查看窗口
 */
public class OperationLogFrame extends JFrame {
    private final LogService logService = new LogService();
    private DefaultTableModel tableModel;

    public OperationLogFrame() {
        setTitle("系统操作日志");
        setSize(900, 600);
        setLocationRelativeTo(null); // 居中显示
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initUI();
        loadLogData();
        setVisible(true);
    }

    // 初始化UI
    private void initUI() {
        // 表格列：日志ID、操作人ID、操作类型、操作内容、操作时间
        String[] columnNames = {"日志ID", "操作人ID", "操作类型", "操作内容", "操作时间"};
        tableModel = new DefaultTableModel(null, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 日志只读，不可编辑
            }
        };
        JTable logTable = new JTable(tableModel);
        // 设置列宽自适应
        logTable.getColumnModel().getColumn(3).setPreferredWidth(300); // 操作内容列加宽

        // 刷新按钮
        JButton refreshBtn = new JButton("刷新日志");
        refreshBtn.addActionListener(e -> loadLogData());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(refreshBtn);

        // 布局
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(logTable), BorderLayout.CENTER);
    }

    // 加载日志数据
    private void loadLogData() {
        tableModel.setRowCount(0); // 清空表格
        List<OperationLog> logs = logService.getAllOperationLogs();
        for (OperationLog log : logs) {
            tableModel.addRow(new Object[]{
                    log.getLogId(),
                    log.getOperatorId(),
                    log.getOperationType(),
                    log.getOperationContent(),
                    log.getOperationTime()
            });
        }
    }
}