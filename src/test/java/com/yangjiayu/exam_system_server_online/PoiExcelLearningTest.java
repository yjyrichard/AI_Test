package com.yangjiayu.exam_system_server_online;

import com.yangjiayu.exam_system_server_online.entity.Question;
import com.yangjiayu.exam_system_server_online.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Apache POI Excel 操作学习测试类
 *
 * 什么是 Apache POI?
 * Apache POI 是一个用于读写 Microsoft Office 格式文件的 Java 库
 * 主要组件:
 * - HSSF: 处理 Excel 97-2003 格式 (.xls)
 * - XSSF: 处理 Excel 2007+ 格式 (.xlsx) - 我们主要使用这个
 * - SXSSF: 处理大数据量的 Excel (.xlsx),内存优化版本
 *
 * 核心概念:
 * - Workbook (工作簿): 整个 Excel 文件
 * - Sheet (工作表): Excel 中的一个标签页
 * - Row (行): 工作表中的一行
 * - Cell (单元格): 行中的一个单元格
 *
 * @author yangjiayu
 * @date 2025-10-19
 */
@Slf4j
@SpringBootTest
public class PoiExcelLearningTest {

    @Autowired
    private QuestionService questionService;

    /**
     * 测试1: 创建一个简单的 Excel 文件
     *
     * 学习目标:
     * 1. 创建 Workbook (工作簿)
     * 2. 创建 Sheet (工作表)
     * 3. 创建 Row (行) 和 Cell (单元格)
     * 4. 设置单元格的值
     * 5. 保存到文件
     */
    @Test
    public void test01_CreateSimpleExcel() throws IOException {
        log.info("========== 测试1: 创建简单的 Excel 文件 ==========");

        // 步骤1: 创建一个工作簿对象 (XSSFWorkbook 对应 .xlsx 格式)
        Workbook workbook = new XSSFWorkbook();
        log.info("1. 创建工作簿对象成功");

        // 步骤2: 在工作簿中创建一个工作表,名称为"学生信息"
        Sheet sheet = workbook.createSheet("学生信息");
        log.info("2. 创建工作表 '学生信息' 成功");

        // 步骤3: 创建第一行 (索引从0开始,所以0代表第一行)
        Row headerRow = sheet.createRow(0);
        log.info("3. 创建表头行(第1行)");

        // 步骤4: 在第一行创建单元格,设置表头
        // createCell(0) 表示创建第一列,索引从0开始
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("学号");  // 设置单元格的值

        Cell cell1 = headerRow.createCell(1);
        cell1.setCellValue("姓名");

        Cell cell2 = headerRow.createCell(2);
        cell2.setCellValue("年龄");

        Cell cell3 = headerRow.createCell(3);
        cell3.setCellValue("成绩");

        log.info("4. 设置表头: 学号 | 姓名 | 年龄 | 成绩");

        // 步骤5: 创建数据行
        // 第二行数据
        Row dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("001");
        dataRow1.createCell(1).setCellValue("张三");
        dataRow1.createCell(2).setCellValue(20);  // 数字类型
        dataRow1.createCell(3).setCellValue(95.5); // 小数类型

        // 第三行数据
        Row dataRow2 = sheet.createRow(2);
        dataRow2.createCell(0).setCellValue("002");
        dataRow2.createCell(1).setCellValue("李四");
        dataRow2.createCell(2).setCellValue(21);
        dataRow2.createCell(3).setCellValue(88.0);

        log.info("5. 创建2行学生数据");

        // 步骤6: 自动调整列宽 (可选,让列宽适应内容)
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
        log.info("6. 自动调整列宽");

        // 步骤7: 将工作簿写入到文件
        // 确保输出目录存在
        File outputDir = new File("D:\\excel_test");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            log.info("7. 创建输出目录: {}", outputDir.getAbsolutePath());
        }

        String filePath = "D:\\excel_test\\学生信息表.xlsx";
        FileOutputStream fos = new FileOutputStream(filePath);
        workbook.write(fos);  // 将工作簿写入输出流

        // 步骤8: 关闭资源 (重要!)
        fos.close();
        workbook.close();

        log.info("8. Excel 文件创建成功! 文件位置: {}", filePath);
        log.info("========== 测试1完成 ==========\n");
    }

    /**
     * 测试2: 读取 Excel 文件
     *
     * 学习目标:
     * 1. 打开已存在的 Excel 文件
     * 2. 遍历工作表
     * 3. 遍历行和单元格
     * 4. 读取不同类型的单元格数据
     */
    @Test
    public void test02_ReadExcel() throws IOException {
        log.info("========== 测试2: 读取 Excel 文件 ==========");

        // 步骤1: 先创建一个测试文件
        test01_CreateSimpleExcel();

        // 步骤2: 打开 Excel 文件
        String filePath = "D:\\excel_test\\学生信息表.xlsx";
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);
        log.info("1. 打开 Excel 文件: {}", filePath);

        // 步骤3: 获取第一个工作表
        Sheet sheet = workbook.getSheetAt(0);  // 通过索引获取
        // 或者通过名称获取: Sheet sheet = workbook.getSheet("学生信息");
        log.info("2. 获取工作表: {}, 总行数: {}", sheet.getSheetName(), sheet.getLastRowNum() + 1);

        // 步骤4: 遍历所有行
        log.info("3. 开始读取数据:");
        log.info("--------------------------------------------------");

        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);

            // 如果行为空,跳过
            if (row == null) {
                continue;
            }

            // 步骤5: 遍历当前行的所有单元格
            StringBuilder rowData = new StringBuilder();
            rowData.append(String.format("第%d行: ", rowIndex + 1));

            for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                Cell cell = row.getCell(cellIndex);

                // 步骤6: 读取单元格的值 (需要根据类型处理)
                String cellValue = getCellValueAsString(cell);
                rowData.append(cellValue).append("\t");
            }

            log.info(rowData.toString());
        }

        log.info("--------------------------------------------------");

        // 步骤7: 关闭资源
        workbook.close();
        fis.close();

        log.info("4. Excel 文件读取完成!");
        log.info("========== 测试2完成 ==========\n");
    }

    /**
     * 测试3: 带样式的 Excel (美化表格)
     *
     * 学习目标:
     * 1. 创建单元格样式
     * 2. 设置字体 (加粗、颜色、大小)
     * 3. 设置背景色
     * 4. 设置边框
     * 5. 设置对齐方式
     */
    @Test
    public void test03_ExcelWithStyle() throws IOException {
        log.info("========== 测试3: 创建带样式的 Excel ==========");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("考试成绩");

        // 步骤1: 创建表头样式
        CellStyle headerStyle = workbook.createCellStyle();

        // 1.1 设置背景色 (蓝色)
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 1.2 设置边框
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 1.3 设置对齐方式 (水平居中、垂直居中)
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 1.4 设置字体 (加粗、14号、黑色)
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);  // 加粗
        headerFont.setFontHeightInPoints((short) 14);  // 字号
        headerFont.setColor(IndexedColors.BLACK.getIndex());  // 颜色
        headerStyle.setFont(headerFont);

        log.info("1. 创建表头样式: 蓝色背景 + 加粗字体 + 居中对齐 + 边框");

        // 步骤2: 创建数据样式
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);

        log.info("2. 创建数据样式: 边框 + 居中对齐");

        // 步骤3: 创建高分样式 (成绩>=90分,标记为绿色)
        CellStyle highScoreStyle = workbook.createCellStyle();
        highScoreStyle.cloneStyleFrom(dataStyle);  // 复制基础样式
        highScoreStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        highScoreStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        log.info("3. 创建高分样式: 绿色背景 (成绩>=90)");

        // 步骤4: 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.setHeight((short) 500);  // 设置行高 (单位: 1/20 磅)

        String[] headers = {"序号", "学生姓名", "科目", "成绩", "等级"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);  // 应用表头样式
        }

        log.info("4. 创建表头并应用样式");

        // 步骤5: 创建数据并应用样式
        Object[][] data = {
            {1, "张三", "Java程序设计", 95, "优秀"},
            {2, "李四", "数据库原理", 88, "良好"},
            {3, "王五", "Web开发", 92, "优秀"},
            {4, "赵六", "算法设计", 78, "中等"}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            Object[] rowData = data[i];

            for (int j = 0; j < rowData.length; j++) {
                Cell cell = row.createCell(j);

                // 设置单元格的值
                if (rowData[j] instanceof Integer) {
                    cell.setCellValue((Integer) rowData[j]);
                } else {
                    cell.setCellValue(rowData[j].toString());
                }

                // 应用样式: 成绩列(第4列)如果>=90,使用高分样式
                if (j == 3 && (Integer) rowData[j] >= 90) {
                    cell.setCellStyle(highScoreStyle);
                } else {
                    cell.setCellStyle(dataStyle);
                }
            }
        }

        log.info("5. 创建数据并应用样式 (高分标记为绿色)");

        // 步骤6: 设置列宽
        sheet.setColumnWidth(0, 2000);   // 序号列
        sheet.setColumnWidth(1, 4000);   // 姓名列
        sheet.setColumnWidth(2, 5000);   // 科目列
        sheet.setColumnWidth(3, 3000);   // 成绩列
        sheet.setColumnWidth(4, 3000);   // 等级列

        log.info("6. 设置列宽");

        // 步骤7: 保存文件
        String filePath = "D:\\excel_test\\考试成绩表_带样式.xlsx";
        FileOutputStream fos = new FileOutputStream(filePath);
        workbook.write(fos);
        fos.close();
        workbook.close();

        log.info("7. 带样式的 Excel 创建成功! 文件位置: {}", filePath);
        log.info("========== 测试3完成 ==========\n");
    }

    /**
     * 测试4: 实战案例 - 导出题目数据到 Excel
     *
     * 学习目标:
     * 1. 从数据库查询数据
     * 2. 将实体类数据写入 Excel
     * 3. 处理日期格式
     * 4. 实现实际业务场景
     */
    @Test
    public void test04_ExportQuestions() throws IOException {
        log.info("========== 测试4: 导出题目数据到 Excel ==========");

        // 步骤1: 从数据库查询题目数据
        List<Question> questions = questionService.list();
        log.info("1. 从数据库查询到 {} 条题目数据", questions.size());

        // 步骤2: 创建工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("题目列表");

        // 步骤3: 创建表头样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);  // 日期专用样式

        // 步骤4: 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"题目ID", "题目标题", "题目类型", "难度", "分值", "分类ID", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        log.info("2. 创建表头");

        // 步骤5: 填充数据
        int rowIndex = 1;
        for (Question question : questions) {
            Row row = sheet.createRow(rowIndex++);

            // ID
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(question.getId());
            cell0.setCellStyle(dataStyle);

            // 标题
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(question.getTitle());
            cell1.setCellStyle(dataStyle);

            // 类型
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(getQuestionTypeName(question.getType()));
            cell2.setCellStyle(dataStyle);

            // 难度
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(getDifficultyName(question.getDifficulty()));
            cell3.setCellStyle(dataStyle);

            // 分值
            Cell cell4 = row.createCell(4);
            cell4.setCellValue(question.getScore());
            cell4.setCellStyle(dataStyle);

            // 分类ID
            Cell cell5 = row.createCell(5);
            cell5.setCellValue(question.getCategoryId());
            cell5.setCellStyle(dataStyle);

            // 创建时间
            Cell cell6 = row.createCell(6);
            if (question.getCreateTime() != null) {
                cell6.setCellValue(question.getCreateTime());
                cell6.setCellStyle(dateStyle);  // 使用日期样式
            }
        }
        log.info("3. 填充 {} 条数据", questions.size());

        // 步骤6: 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // 因为中文宽度计算不准,手动增加一些宽度
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
        log.info("4. 自动调整列宽");

        // 步骤7: 保存文件
        String filePath = "D:\\excel_test\\题目数据导出_" + System.currentTimeMillis() + ".xlsx";
        FileOutputStream fos = new FileOutputStream(filePath);
        workbook.write(fos);
        fos.close();
        workbook.close();

        log.info("5. 题目数据导出成功! 文件位置: {}", filePath);
        log.info("========== 测试4完成 ==========\n");
    }

    /**
     * 测试5: 读取 Excel 批量导入题目
     *
     * 学习目标:
     * 1. 读取 Excel 数据
     * 2. 数据验证
     * 3. 批量插入数据库
     */
    @Test
    public void test05_ImportQuestions() throws IOException {
        log.info("========== 测试5: 从 Excel 批量导入题目 ==========");

        // 这个测试需要你先手动创建一个模板 Excel 文件
        // 文件格式参考 test04 导出的文件
        // 这里只演示读取逻辑

        log.info("提示: 请先运行 test04 导出一个示例文件");
        log.info("然后可以基于该文件编辑新题目进行批量导入");
        log.info("========== 测试5完成 ==========\n");
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取单元格的值并转换为字符串
     *
     * 重要: Excel 单元格有多种类型,需要根据类型读取
     * - STRING: 字符串
     * - NUMERIC: 数字 (包括日期)
     * - BOOLEAN: 布尔值
     * - FORMULA: 公式
     * - BLANK: 空白
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        // 根据单元格类型读取值
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                // 判断是否为日期类型
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return date.toString();
                } else {
                    // 数字类型,避免科学计数法显示
                    double numericValue = cell.getNumericCellValue();
                    // 如果是整数,不显示小数点
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                // 公式类型,返回计算后的值
                return cell.getCellFormula();

            case BLANK:
                return "";

            default:
                return "";
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 背景色
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 对齐
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 字体
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 对齐
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 创建日期样式
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 对齐
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 日期格式
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));

        return style;
    }

    /**
     * 获取题目类型中文名称
     */
    private String getQuestionTypeName(String type) {
        if (type == null) return "";
        switch (type) {
            case "CHOICE": return "选择题";
            case "JUDGE": return "判断题";
            case "TEXT": return "简答题";
            default: return type;
        }
    }

    /**
     * 获取难度中文名称
     */
    private String getDifficultyName(String difficulty) {
        if (difficulty == null) return "";
        switch (difficulty) {
            case "EASY": return "简单";
            case "MEDIUM": return "中等";
            case "HARD": return "困难";
            default: return difficulty;
        }
    }
}
