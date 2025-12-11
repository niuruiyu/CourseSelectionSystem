package util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CSVExporter {
    
    /**
     * 导出表格数据到CSV文件
     * @param table 要导出的JTable
     * @param defaultFileName 默认文件名（不带扩展名）
     * @return 是否导出成功
     */
    public static boolean exportToCSV(JTable table, String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出CSV文件");
        fileChooser.setSelectedFile(new File(defaultFileName + ".csv"));
        
        // 设置文件过滤器
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".csv") || f.isDirectory();
            }
            
            @Override
            public String getDescription() {
                return "CSV文件 (*.csv)";
            }
        });
        
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // 确保文件扩展名正确
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            // 如果文件已存在，询问是否覆盖
            if (file.exists()) {
                int response = JOptionPane.showConfirmDialog(null, 
                    "文件已存在，是否覆盖？", "确认", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
            
            return exportTableToCSV(table, file);
        }
        return false;
    }
    
    /**
     * 实际导出表格数据到CSV文件
     */
    private static boolean exportTableToCSV(JTable table, File file) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file),
                StandardCharsets.UTF_8
        ))  {
            // 1. 写入UTF-8 BOM（确保Excel正确识别中文）
            writer.write("\uFEFF");
            
            // 2. 写入表头
            for (int i = 0; i < model.getColumnCount(); i++) {
                writer.write(escapeCSV(model.getColumnName(i)));
                if (i < model.getColumnCount() - 1) {
                    writer.write(",");
                }
            }
            writer.write("\n");
            
            // 3. 写入数据行
            int rowCount = 0;
            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    writer.write(escapeCSV(value != null ? value.toString() : ""));
                    if (col < model.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
                rowCount++;
            }
            
            // 4. 写入统计信息（作为注释）
            writer.write("\n# === 导出信息 ===\n");
            writer.write("# 导出时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("# 数据行数: " + rowCount + "\n");
            writer.write("# 数据列数: " + model.getColumnCount() + "\n");
            writer.write("# 导出工具: 选课系统 v1.0\n");
            
            return true;
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, 
                "导出失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 转义CSV字段（处理逗号、引号等特殊字符）
     */
    private static String escapeCSV(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        
        boolean needsQuotes = false;
        
        // 检查是否需要引号包围
        if (value.contains(",") || 
            value.contains("\"") || 
            value.contains("\n") || 
            value.contains("\r") ||
            value.startsWith(" ") || 
            value.endsWith(" ") ||
            value.contains("\t")) {
            needsQuotes = true;
        }
        
        // 转义双引号（" -> ""）
        if (value.contains("\"")) {
            value = value.replace("\"", "\"\"");
        }
        
        // 添加引号
        if (needsQuotes) {
            return "\"" + value + "\"";
        }
        
        return value;
    }
    
    /**
     * 导出课程统计数据（直接使用Course对象列表）
     * @param courses 课程列表
     * @param defaultFileName 默认文件名
     * @return 是否导出成功
     */
// 在CSVExporter.java中修改exportCoursesToCSV方法：
    public static boolean exportCoursesToCSV(java.util.List<model.Course> courses, String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出课程统计CSV");
        fileChooser.setSelectedFile(new File(defaultFileName + ".csv"));
        
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".csv") || f.isDirectory();
            }
            
            @Override
            public String getDescription() {
                return "CSV文件 (*.csv)";
            }
        });
        
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(file),  // 基于File对象创建文件输出流
                    java.nio.charset.StandardCharsets.UTF_8  // 显式指定UTF-8字符集
            )) {
                // UTF-8 BOM
                writer.write("\uFEFF");
                
                // 表头 - 确保包含所有字段
                String[] headers = {"课程代码", "课程名称", "授课教师", "学分", "上课时间", "容量", "已选人数", "饱和度%"};
                for (int i = 0; i < headers.length; i++) {
                    writer.write(escapeCSV(headers[i]));
                    if (i < headers.length - 1) writer.write(",");
                }
                writer.write("\n");
                
                // 数据行
                for (model.Course course : courses) {
                    // 计算饱和度百分比
                    double saturation = 0.0;
                    if (course.getCapacityLimit() > 0) {
                        saturation = (course.getCurrentSelected() * 100.0) / course.getCapacityLimit();
                    }
                    
                    // 构建行数据 - 确保获取所有字段
                    String[] row = {
                        course.getCourseCode() != null ? course.getCourseCode() : "",
                        course.getCourseName() != null ? course.getCourseName() : "",
                        course.getTeacherName() != null ? course.getTeacherName() : "",
                        String.valueOf(course.getCredit()),  // 学分
                        course.getScheduleTime() != null ? course.getScheduleTime() : "",  // 上课时间
                        String.valueOf(course.getCapacityLimit()),
                        String.valueOf(course.getCurrentSelected()),
                        String.format("%.2f%%", saturation)
                    };
                    
                    for (int i = 0; i < row.length; i++) {
                        writer.write(escapeCSV(row[i]));
                        if (i < row.length - 1) writer.write(",");
                    }
                    writer.write("\n");
                }
                
                // 统计信息
                writer.write("\n# === 课程统计 ===\n");
                writer.write("# 导出时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                writer.write("# 课程总数: " + courses.size() + "\n");
                
                return true;
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, 
                    "导出失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return false;
            }
        }
        return false;
 }
}
// 如果需要使用StandardCharsets而报错，添加这个内部类
class StandardCharsets {
                public static final java.nio.charset.Charset UTF_8 = java.nio.charset.Charset.forName("UTF-8");
}