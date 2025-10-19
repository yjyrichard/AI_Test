package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.entity.Question;
import com.yangjiayu.exam_system_server_online.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 文件下载示例 Controller
 *
 * 演示三种文件下载方式:
 * 1. HttpServletResponse - 传统方式
 * 2. ResponseEntity<byte[]> - 小文件方式
 * 3. ResponseEntity<Resource> - 推荐方式
 *
 * 访问地址:
 * - http://localhost:8080/api/file-download/xxx
 *
 * @author yangjiayu
 * @date 2025-10-19
 */
@Slf4j
@RestController
@RequestMapping("/file-download")
@RequiredArgsConstructor
@Tag(name = "文件下载示例", description = "演示文件下载的三种实现方式")
public class FileDownloadController {

    private final QuestionService questionService;

    // 临时文件存放目录
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    /**
     * 方式1: 使用 HttpServletResponse 下载文件
     *
     * 优点: 灵活,可精细控制
     * 缺点: 代码冗长,不符合 RESTful 风格
     * 适用: 需要精细控制响应流程的场景
     *
     * 访问: GET /api/file-download/response?fileName=学生成绩.xlsx
     */
    @GetMapping("/response")
    @Operation(summary = "方式1: HttpServletResponse 下载",
               description = "传统 Servlet 方式,手动操作输出流")
    public void downloadWithResponse(
        @Parameter(description = "文件名", example = "学生成绩.xlsx")
        @RequestParam(defaultValue = "示例文件.xlsx") String fileName,
        HttpServletResponse response) throws IOException {

        log.info("【方式1】HttpServletResponse 下载文件: {}", fileName);

        // 1. 生成测试用的 Excel 文件
        File file = generateSampleExcel(fileName);

        // 2. 设置响应头
        response.setContentType("application/vnd.ms-excel");  // Excel 的 MIME 类型
        response.setHeader("Content-Disposition",
                          "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLengthLong(file.length());  // 设置文件大小

        log.info("设置响应头: Content-Type=application/vnd.ms-excel, 文件大小={} 字节", file.length());

        // 3. 读取文件并写入响应流
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            byte[] buffer = new byte[1024];  // 1KB 缓冲区
            int len;
            int totalBytes = 0;

            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
                totalBytes += len;
            }

            os.flush();
            log.info("文件传输完成: {} 字节", totalBytes);

        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw e;
        } finally {
            // 删除临时文件
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * 方式2: 使用 ResponseEntity<byte[]> 下载文件
     *
     * 优点: 代码简洁,易于测试
     * 缺点: 文件全部加载到内存,不适合大文件
     * 适用: 小文件下载 (< 10MB),如 Excel 导出、PDF 报表
     *
     * 访问: GET /api/file-download/bytes?fileName=学生成绩.xlsx
     */
    @GetMapping("/bytes")
    @Operation(summary = "方式2: ResponseEntity<byte[]> 下载",
               description = "适合小文件,代码简洁,但会将文件全部加载到内存")
    public ResponseEntity<byte[]> downloadWithBytes(
        @Parameter(description = "文件名", example = "学生成绩.xlsx")
        @RequestParam(defaultValue = "示例文件.xlsx") String fileName) throws IOException {

        log.info("【方式2】ResponseEntity<byte[]> 下载文件: {}", fileName);

        // 1. 生成测试用的 Excel 文件
        File file = generateSampleExcel(fileName);

        try {
            // 2. 读取文件内容到字节数组
            byte[] fileContent = Files.readAllBytes(file.toPath());
            log.info("文件读取到内存: {} 字节", fileContent.length);

            // 3. 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                                 .filename(fileName, StandardCharsets.UTF_8)
                                 .build()
            );

            log.info("构建 ResponseEntity 并返回");

            // 4. 返回 ResponseEntity
            return ResponseEntity.ok()
                                .headers(headers)
                                .body(fileContent);

        } finally {
            // 删除临时文件
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * 方式3: 使用 ResponseEntity<Resource> 下载文件 (推荐)
     *
     * 优点: 流式传输,内存友好,支持大文件
     * 缺点: 需要理解 Resource 概念
     * 适用: 大文件下载,生产环境推荐
     *
     * 访问: GET /api/file-download/resource?fileName=学生成绩.xlsx
     */
    @GetMapping("/resource")
    @Operation(summary = "方式3: ResponseEntity<Resource> 下载 (推荐)",
               description = "流式传输,内存友好,支持大文件,生产环境推荐")
    public ResponseEntity<Resource> downloadWithResource(
        @Parameter(description = "文件名", example = "学生成绩.xlsx")
        @RequestParam(defaultValue = "示例文件.xlsx") String fileName) throws IOException {

        log.info("【方式3】ResponseEntity<Resource> 下载文件: {}", fileName);

        // 1. 生成测试用的 Excel 文件
        File file = generateSampleExcel(fileName);

        // 2. 创建 Resource 对象
        Resource resource = new FileSystemResource(file);
        log.info("创建 Resource: {}, 文件大小: {} 字节", resource.getFilename(), resource.contentLength());

        // 3. 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
            ContentDisposition.builder("attachment")
                             .filename(fileName, StandardCharsets.UTF_8)
                             .build()
        );
        headers.setContentLength(file.length());  // 设置文件大小,浏览器可显示下载进度

        log.info("构建 ResponseEntity 并返回");

        // 4. 返回 ResponseEntity
        // 注意: 这里不删除临时文件,因为 Resource 需要在响应时读取文件
        //      实际项目中可以使用定时任务清理临时文件
        return ResponseEntity.ok()
                            .headers(headers)
                            .body(resource);
    }

    /**
     * 实战案例: 导出题目数据到 Excel
     *
     * 业务场景:
     * - 管理员需要导出题库数据
     * - 支持查看、备份、分析
     *
     * 技术选型:
     * - 使用 ResponseEntity<Resource> (推荐方式)
     * - 使用 Apache POI 生成 Excel
     *
     * 访问: GET /api/file-download/export-questions
     */
    @GetMapping("/export-questions")
    @Operation(summary = "导出题目数据到 Excel",
               description = "从数据库查询题目数据,生成 Excel 文件并下载")
    public ResponseEntity<Resource> exportQuestions() throws IOException {
        log.info("【实战案例】导出题目数据到 Excel");

        // 1. 查询题目数据
        List<Question> questions = questionService.list();
        log.info("查询到 {} 条题目数据", questions.size());

        // 2. 生成 Excel
        File excelFile = generateQuestionExcel(questions);
        log.info("Excel 生成完成: {}, 大小: {} 字节", excelFile.getName(), excelFile.length());

        // 3. 创建 Resource
        Resource resource = new FileSystemResource(excelFile);

        // 4. 设置响应头
        String fileName = "题目数据_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel"));
        headers.setContentDisposition(
            ContentDisposition.builder("attachment")
                             .filename(fileName, StandardCharsets.UTF_8)
                             .build()
        );
        headers.setContentLength(excelFile.length());

        log.info("返回文件: {}", fileName);

        // 5. 返回
        return ResponseEntity.ok()
                            .headers(headers)
                            .body(resource);
    }

    /**
     * 在线预览 vs 下载
     *
     * Content-Disposition 的两种模式:
     * - attachment: 下载文件
     * - inline: 在线预览 (如 PDF、图片)
     *
     * 访问: GET /api/file-download/preview?fileName=示例.xlsx
     */
    @GetMapping("/preview")
    @Operation(summary = "在线预览 vs 下载",
               description = "演示 Content-Disposition 的 inline 和 attachment 区别")
    public ResponseEntity<Resource> previewFile(
        @Parameter(description = "文件名") @RequestParam(defaultValue = "示例文件.xlsx") String fileName,
        @Parameter(description = "是否在线预览 (true=预览, false=下载)") @RequestParam(defaultValue = "false") boolean inline) throws IOException {

        log.info("文件{}: {}", inline ? "在线预览" : "下载", fileName);

        File file = generateSampleExcel(fileName);
        Resource resource = new FileSystemResource(file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        // 关键区别在这里: inline vs attachment
        String dispositionType = inline ? "inline" : "attachment";
        headers.setContentDisposition(
            ContentDisposition.builder(dispositionType)
                             .filename(fileName, StandardCharsets.UTF_8)
                             .build()
        );
        headers.setContentLength(file.length());

        return ResponseEntity.ok()
                            .headers(headers)
                            .body(resource);
    }

    /**
     * 错误处理示例: 文件不存在
     *
     * 访问: GET /api/file-download/not-found
     */
    @GetMapping("/not-found")
    @Operation(summary = "错误处理示例", description = "演示文件不存在时的处理")
    public ResponseEntity<Result<String>> handleFileNotFound() {
        log.warn("文件不存在示例");

        // 实际项目中,可以抛出自定义异常,由全局异常处理器处理
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Result.error("文件不存在"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成示例 Excel 文件
     */
    private File generateSampleExcel(String fileName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("数据");

        // 表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("学号");
        headerRow.createCell(1).setCellValue("姓名");
        headerRow.createCell(2).setCellValue("成绩");

        // 数据
        String[][] data = {
            {"001", "张三", "95"},
            {"002", "李四", "88"},
            {"003", "王五", "92"}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < data[i].length; j++) {
                row.createCell(j).setCellValue(data[i][j]);
            }
        }

        // 写入临时文件
        File tempFile = File.createTempFile("sample_", "_" + fileName);
        FileOutputStream fos = new FileOutputStream(tempFile);
        workbook.write(fos);
        fos.close();
        workbook.close();

        return tempFile;
    }

    /**
     * 生成题目数据 Excel
     */
    private File generateQuestionExcel(List<Question> questions) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("题目列表");

        // 创建样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"题目ID", "题目标题", "题目类型", "难度", "分值", "分类ID"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
        int rowIndex = 1;
        for (Question question : questions) {
            Row row = sheet.createRow(rowIndex++);

            row.createCell(0).setCellValue(question.getId());
            row.createCell(1).setCellValue(question.getTitle());
            row.createCell(2).setCellValue(getQuestionTypeName(question.getType()));
            row.createCell(3).setCellValue(getDifficultyName(question.getDifficulty()));
            row.createCell(4).setCellValue(question.getScore());
            row.createCell(5).setCellValue(question.getCategoryId());

            // 应用样式
            for (int i = 0; i < 6; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }

        // 写入临时文件
        File tempFile = File.createTempFile("questions_", ".xlsx");
        FileOutputStream fos = new FileOutputStream(tempFile);
        workbook.write(fos);
        fos.close();
        workbook.close();

        return tempFile;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private String getQuestionTypeName(String type) {
        if (type == null) return "";
        switch (type) {
            case "CHOICE": return "选择题";
            case "JUDGE": return "判断题";
            case "TEXT": return "简答题";
            default: return type;
        }
    }

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
