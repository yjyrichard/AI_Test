package com.yangjiayu.exam_system_server_online;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 文件下载测试类 - 三种方式详解
 *
 * 本测试类演示了 Spring Boot 中文件下载的三种实现方式:
 * 1. HttpServletResponse - 传统 Servlet 方式
 * 2. ResponseEntity<byte[]> - 小文件下载方式
 * 3. ResponseEntity<Resource> - 大文件下载方式(推荐)
 *
 * 配合 FileDownloadLearning.md 文档学习
 *
 * @author yangjiayu
 * @date 2025-10-19
 */
@Slf4j
@SpringBootTest
public class FileDownloadTest {

    // 测试文件存放目录
    private static final String TEST_DIR = "D:\\file_download_test";

    /**
     * 准备测试: 创建测试用的 Excel 文件
     *
     * 在运行其他测试前,先运行这个方法生成测试文件
     */
    @Test
    public void test00_PrepareTestFile() throws IOException {
        log.info("========== 准备测试文件 ==========");

        // 1. 创建测试目录
        File dir = new File(TEST_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("创建测试目录: {}", TEST_DIR);
        }

        // 2. 创建一个 Excel 文件
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("学生成绩");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("学号");
        headerRow.createCell(1).setCellValue("姓名");
        headerRow.createCell(2).setCellValue("成绩");

        // 创建数据
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

        // 保存文件
        String filePath = TEST_DIR + "\\学生成绩表.xlsx";
        FileOutputStream fos = new FileOutputStream(filePath);
        workbook.write(fos);
        fos.close();
        workbook.close();

        log.info("测试文件创建成功: {}", filePath);
        log.info("文件大小: {} 字节", new File(filePath).length());
        log.info("========== 准备完成 ==========\n");
    }

    /**
     * 测试1: 模拟 HttpServletResponse 方式下载
     *
     * 学习要点:
     * 1. 如何设置响应头
     * 2. 如何使用输出流写入文件内容
     * 3. 如何处理中文文件名
     * 4. 缓冲区的作用
     *
     * 注意: 这是单元测试,无法真正测试 HttpServletResponse
     *       实际使用需要在 Controller 中实现
     */
    @Test
    public void test01_HttpServletResponseWay() throws IOException {
        log.info("========== 测试1: HttpServletResponse 方式 ==========");

        // 这里演示核心代码逻辑
        File file = new File(TEST_DIR + "\\学生成绩表.xlsx");
        log.info("1. 读取文件: {}", file.getName());
        log.info("   文件大小: {} 字节", file.length());

        // 模拟设置响应头 (实际在 Controller 中使用 response 对象)
        log.info("\n2. 设置响应头 (实际代码):");
        log.info("   response.setContentType(\"application/vnd.ms-excel\");");
        log.info("   response.setHeader(\"Content-Disposition\", \"attachment; filename=\" + URLEncoder.encode(\"{}\", \"UTF-8\"));", file.getName());
        log.info("   response.setContentLengthLong({});", file.length());

        // 模拟读取文件并写入输出流
        log.info("\n3. 读取文件内容并写入响应流:");
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];  // 1KB 缓冲区
        int len;
        int totalBytes = 0;
        int batchCount = 0;

        while ((len = fis.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
            totalBytes += len;
            batchCount++;
            log.info("   批次 {}: 读取 {} 字节, 累计 {} 字节", batchCount, len, totalBytes);
        }

        fis.close();
        log.info("\n4. 文件传输完成! 总共分 {} 批传输,总大小 {} 字节", batchCount, totalBytes);

        log.info("\n优点:");
        log.info("  ✅ 灵活性高,可精细控制");
        log.info("  ✅ 使用缓冲区,内存占用低");
        log.info("缺点:");
        log.info("  ❌ 代码冗长,需要手动处理流");
        log.info("  ❌ 返回 void,不符合 RESTful 风格");

        log.info("========== 测试1完成 ==========\n");
    }

    /**
     * 测试2: ResponseEntity<byte[]> 方式
     *
     * 学习要点:
     * 1. 如何构建 ResponseEntity
     * 2. 如何设置 HttpHeaders
     * 3. byte[] 的优缺点
     * 4. 适用场景
     */
    @Test
    public void test02_ResponseEntityBytesWay() throws IOException {
        log.info("========== 测试2: ResponseEntity<byte[]> 方式 ==========");

        // 1. 读取文件内容到字节数组
        File file = new File(TEST_DIR + "\\学生成绩表.xlsx");
        byte[] fileContent = Files.readAllBytes(file.toPath());
        log.info("1. 读取文件到内存: {} 字节", fileContent.length);
        log.info("   内存占用: {} KB", fileContent.length / 1024);

        // 2. 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
            ContentDisposition.builder("attachment")
                             .filename("学生成绩表.xlsx", StandardCharsets.UTF_8)
                             .build()
        );
        log.info("\n2. 设置响应头:");
        log.info("   Content-Type: {}", headers.getContentType());
        log.info("   Content-Disposition: {}", headers.getContentDisposition());

        // 3. 构建 ResponseEntity
        ResponseEntity<byte[]> responseEntity = ResponseEntity.ok()
                                                              .headers(headers)
                                                              .body(fileContent);

        log.info("\n3. 构建 ResponseEntity:");
        log.info("   状态码: {}", responseEntity.getStatusCode());
        log.info("   响应体大小: {} 字节", responseEntity.getBody().length);
        log.info("   是否成功: {}", responseEntity.getStatusCode() == HttpStatus.OK);

        log.info("\n核心代码解析:");
        log.info("  ResponseEntity.ok()              ← 设置 HTTP 200 状态码");
        log.info("               .headers(headers)   ← 设置响应头");
        log.info("               .body(fileContent)  ← 设置响应体(字节数组)");

        log.info("\n优点:");
        log.info("  ✅ 代码简洁优雅");
        log.info("  ✅ Spring 自动处理编码");
        log.info("  ✅ 易于单元测试");
        log.info("缺点:");
        log.info("  ❌ 整个文件加载到内存");
        log.info("  ❌ 不适合大文件 (> 10MB)");

        log.info("\n适用场景:");
        log.info("  • Excel 导出 (通常 < 5MB)");
        log.info("  • PDF 报表");
        log.info("  • 小图片下载");

        log.info("========== 测试2完成 ==========\n");
    }

    /**
     * 测试3: ResponseEntity<Resource> 方式 (推荐)
     *
     * 学习要点:
     * 1. 什么是 Resource
     * 2. Resource 的流式传输原理
     * 3. 为什么适合大文件
     * 4. 最佳实践
     */
    @Test
    public void test03_ResponseEntityResourceWay() throws IOException {
        log.info("========== 测试3: ResponseEntity<Resource> 方式 (推荐) ==========");

        // 1. 创建 Resource 对象
        File file = new File(TEST_DIR + "\\学生成绩表.xlsx");
        Resource resource = new FileSystemResource(file);
        log.info("1. 创建 Resource 对象:");
        log.info("   类型: {}", resource.getClass().getSimpleName());
        log.info("   文件名: {}", resource.getFilename());
        log.info("   文件大小: {} 字节", resource.contentLength());
        log.info("   是否存在: {}", resource.exists());
        log.info("   是否可读: {}", resource.isReadable());

        // 2. 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
            ContentDisposition.builder("attachment")
                             .filename("学生成绩表.xlsx", StandardCharsets.UTF_8)
                             .build()
        );
        headers.setContentLength(file.length());
        log.info("\n2. 设置响应头:");
        log.info("   Content-Type: {}", headers.getContentType());
        log.info("   Content-Length: {} 字节", headers.getContentLength());
        log.info("   Content-Disposition: {}", headers.getContentDisposition());

        // 3. 构建 ResponseEntity
        ResponseEntity<Resource> responseEntity = ResponseEntity.ok()
                                                                 .headers(headers)
                                                                 .body(resource);

        log.info("\n3. 构建 ResponseEntity:");
        log.info("   状态码: {}", responseEntity.getStatusCode());
        log.info("   响应体类型: {}", responseEntity.getBody().getClass().getSimpleName());

        // 4. 模拟流式传输
        log.info("\n4. Resource 流式传输原理:");
        log.info("   [原理] Resource 不会一次性加载文件到内存");
        log.info("   [原理] Spring 会自动使用输入流分批读取");
        log.info("   [原理] 边读边传,内存占用极低");

        // 演示读取流程
        InputStream is = resource.getInputStream();
        byte[] buffer = new byte[1024];
        int len;
        int totalBytes = 0;
        int batchCount = 0;

        log.info("\n   模拟分批读取:");
        while ((len = is.read(buffer)) != -1) {
            totalBytes += len;
            batchCount++;
            if (batchCount <= 3 || len < 1024) {  // 只显示前3批和最后一批
                log.info("     批次 {}: 读取 {} 字节", batchCount, len);
            }
        }
        is.close();
        log.info("     ... (共 {} 批)", batchCount);
        log.info("     总计: {} 字节", totalBytes);

        log.info("\n优点:");
        log.info("  ✅ 内存占用低 (流式传输)");
        log.info("  ✅ 支持大文件 (GB 级别)");
        log.info("  ✅ 代码简洁优雅");
        log.info("  ✅ Spring 推荐方式");

        log.info("\n缺点:");
        log.info("  ❌ 需要理解 Resource 概念");

        log.info("\n适用场景:");
        log.info("  • 大文件下载 (视频、压缩包)");
        log.info("  • 高并发场景");
        log.info("  • 生产环境推荐");

        log.info("========== 测试3完成 ==========\n");
    }

    /**
     * 测试4: 对比三种方式的内存占用
     *
     * 学习要点:
     * 1. 内存占用的差异
     * 2. 性能对比
     * 3. 如何选择合适的方式
     */
    @Test
    public void test04_MemoryComparison() throws IOException {
        log.info("========== 测试4: 三种方式内存占用对比 ==========");

        File file = new File(TEST_DIR + "\\学生成绩表.xlsx");
        long fileSize = file.length();

        log.info("测试文件: {}", file.getName());
        log.info("文件大小: {} 字节 ({} KB)\n", fileSize, fileSize / 1024);

        // 1. byte[] 方式
        log.info("方式1: ResponseEntity<byte[]>");
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        byte[] fileContent = Files.readAllBytes(file.toPath());

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long usedMemory = afterMemory - beforeMemory;

        log.info("  文件加载到内存: {} 字节", fileContent.length);
        log.info("  实际内存占用: {} 字节 ({} KB)", usedMemory, usedMemory / 1024);
        log.info("  内存占用率: {}%", (usedMemory * 100 / fileSize));

        // 2. Resource 方式
        log.info("\n方式2: ResponseEntity<Resource>");
        Resource resource = new FileSystemResource(file);
        log.info("  Resource 对象大小: 极小 (只是文件引用)");
        log.info("  实际传输时: 分批读取,每批 1-8KB");
        log.info("  内存占用: 恒定 (仅缓冲区大小)");

        log.info("\n结论:");
        log.info("  • byte[] 方式: 文件多大,内存占用就多大");
        log.info("  • Resource 方式: 内存占用固定,与文件大小无关");
        log.info("  • 对于 100MB 文件:");
        log.info("    - byte[] 需要 100MB 内存");
        log.info("    - Resource 只需要 几KB 内存");

        log.info("========== 测试4完成 ==========\n");
    }

    /**
     * 测试5: 常见 Content-Type 示例
     *
     * 学习要点:
     * 1. 不同文件类型的 MIME 类型
     * 2. 如何设置正确的 Content-Type
     * 3. Content-Type 对下载的影响
     */
    @Test
    public void test05_ContentTypes() {
        log.info("========== 测试5: 常见 Content-Type ==========\n");

        log.info("Office 文档:");
        log.info("  Excel 2007+ (.xlsx): application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        log.info("  Excel 97-2003 (.xls): application/vnd.ms-excel");
        log.info("  Word 2007+ (.docx):  application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        log.info("  Word 97-2003 (.doc): application/msword");
        log.info("  PowerPoint (.pptx):  application/vnd.openxmlformats-officedocument.presentationml.presentation");
        log.info("  PDF (.pdf):          application/pdf");

        log.info("\n图片:");
        log.info("  JPEG (.jpg):         image/jpeg");
        log.info("  PNG (.png):          image/png");
        log.info("  GIF (.gif):          image/gif");
        log.info("  SVG (.svg):          image/svg+xml");

        log.info("\n视频:");
        log.info("  MP4 (.mp4):          video/mp4");
        log.info("  AVI (.avi):          video/x-msvideo");
        log.info("  WebM (.webm):        video/webm");

        log.info("\n压缩文件:");
        log.info("  ZIP (.zip):          application/zip");
        log.info("  RAR (.rar):          application/x-rar-compressed");
        log.info("  7Z (.7z):            application/x-7z-compressed");

        log.info("\n文本:");
        log.info("  纯文本 (.txt):       text/plain");
        log.info("  HTML (.html):        text/html");
        log.info("  CSS (.css):          text/css");
        log.info("  JavaScript (.js):    application/javascript");
        log.info("  JSON (.json):        application/json");

        log.info("\n通用:");
        log.info("  二进制流:            application/octet-stream  ← 万能类型");

        log.info("\n小技巧:");
        log.info("  // 自动检测文件类型");
        log.info("  String contentType = Files.probeContentType(file.toPath());");
        log.info("  headers.setContentType(MediaType.parseMediaType(contentType));");

        log.info("========== 测试5完成 ==========\n");
    }

    /**
     * 测试6: Content-Disposition 详解
     *
     * 学习要点:
     * 1. attachment vs inline
     * 2. 文件名编码
     * 3. 浏览器兼容性
     */
    @Test
    public void test06_ContentDisposition() throws UnsupportedEncodingException {
        log.info("========== 测试6: Content-Disposition 详解 ==========\n");

        String fileName = "学生成绩表.xlsx";

        log.info("1. attachment - 下载文件");
        ContentDisposition attachment = ContentDisposition.builder("attachment")
                                                          .filename(fileName, StandardCharsets.UTF_8)
                                                          .build();
        log.info("   {}", attachment);
        log.info("   效果: 浏览器弹出下载框");

        log.info("\n2. inline - 在线预览");
        ContentDisposition inline = ContentDisposition.builder("inline")
                                                      .filename(fileName, StandardCharsets.UTF_8)
                                                      .build();
        log.info("   {}", inline);
        log.info("   效果: 浏览器直接打开 (如 PDF、图片)");

        log.info("\n3. 文件名编码问题");
        log.info("   问题: 中文文件名在不同浏览器可能乱码");
        log.info("   解决: Spring 的 ContentDisposition 自动处理");
        log.info("   原理: 使用 RFC 5987 编码标准");
        log.info("         filename*=UTF-8''%E5%AD%A6%E7%94%9F...");

        log.info("\n4. 传统编码方式 (兼容老项目)");
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        log.info("   URLEncoder: {}", encodedFileName);
        log.info("   使用: \"attachment; filename=\" + encodedFileName");

        log.info("========== 测试6完成 ==========\n");
    }

    /**
     * 测试7: 模拟实际业务场景
     *
     * 学习要点:
     * 1. 动态生成 Excel
     * 2. 设置合适的文件名
     * 3. 完整的下载流程
     */
    @Test
    public void test07_RealWorldScenario() throws IOException {
        log.info("========== 测试7: 实际业务场景 - 导出学生成绩 ==========\n");

        // 场景: 导出学生成绩报表
        log.info("场景: 用户点击 [导出成绩] 按钮");

        // 1. 生成 Excel
        log.info("\n步骤1: 生成 Excel 文件");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("成绩单");

        // 表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("学号");
        headerRow.createCell(1).setCellValue("姓名");
        headerRow.createCell(2).setCellValue("Java");
        headerRow.createCell(3).setCellValue("数据库");
        headerRow.createCell(4).setCellValue("总分");

        // 数据 (实际项目中从数据库查询)
        Object[][] data = {
            {"2021001", "张三", 95, 88, 183},
            {"2021002", "李四", 88, 92, 180},
            {"2021003", "王五", 92, 85, 177}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                if (data[i][j] instanceof String) {
                    cell.setCellValue((String) data[i][j]);
                } else {
                    cell.setCellValue((Integer) data[i][j]);
                }
            }
        }

        log.info("  ✅ Excel 生成完成,共 {} 行数据", data.length);

        // 2. 写入临时文件
        log.info("\n步骤2: 写入临时文件");
        File tempFile = new File(TEST_DIR + "\\成绩单_" + System.currentTimeMillis() + ".xlsx");
        FileOutputStream fos = new FileOutputStream(tempFile);
        workbook.write(fos);
        fos.close();
        workbook.close();
        log.info("  ✅ 临时文件: {}", tempFile.getAbsolutePath());

        // 3. 构建响应
        log.info("\n步骤3: 构建下载响应");
        Resource resource = new FileSystemResource(tempFile);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel"));
        headers.setContentDisposition(
            ContentDisposition.builder("attachment")
                             .filename("学生成绩单.xlsx", StandardCharsets.UTF_8)
                             .build()
        );
        headers.setContentLength(tempFile.length());

        ResponseEntity<Resource> response = ResponseEntity.ok()
                                                         .headers(headers)
                                                         .body(resource);

        log.info("  ✅ ResponseEntity 构建完成");
        log.info("     状态码: {}", response.getStatusCode());
        log.info("     文件大小: {} 字节", tempFile.length());

        // 4. 模拟返回给前端
        log.info("\n步骤4: 返回给前端");
        log.info("  前端接收到文件后,浏览器自动弹出下载框");
        log.info("  文件名: 学生成绩单.xlsx");

        log.info("\n完整的 Controller 代码:");
        log.info("  @GetMapping(\"/export-scores\")");
        log.info("  public ResponseEntity<Resource> exportScores() {{");
        log.info("      // 1. 查询数据");
        log.info("      List<Student> students = studentService.list();");
        log.info("      ");
        log.info("      // 2. 生成 Excel");
        log.info("      File excelFile = excelService.generateScoreReport(students);");
        log.info("      ");
        log.info("      // 3. 返回下载");
        log.info("      Resource resource = new FileSystemResource(excelFile);");
        log.info("      return ResponseEntity.ok()");
        log.info("                           .headers(createDownloadHeaders(\"成绩单.xlsx\"))");
        log.info("                           .body(resource);");
        log.info("  }}");

        log.info("========== 测试7完成 ==========\n");
    }

    /**
     * 测试8: 错误处理和边界情况
     *
     * 学习要点:
     * 1. 文件不存在的处理
     * 2. 文件名为空的处理
     * 3. 异常捕获
     */
    @Test
    public void test08_ErrorHandling() {
        log.info("========== 测试8: 错误处理和边界情况 ==========\n");

        log.info("常见错误场景:");

        // 1. 文件不存在
        log.info("\n1. 文件不存在");
        File nonExistFile = new File("D:\\不存在的文件.xlsx");
        log.info("   文件存在: {}", nonExistFile.exists());
        log.info("   处理方式:");
        log.info("     if (!file.exists()) {{");
        log.info("         throw new FileNotFoundException(\"文件不存在\");");
        log.info("     }}");

        // 2. 文件名为空
        log.info("\n2. 文件名为空或非法");
        log.info("   处理方式:");
        log.info("     String fileName = StringUtils.hasText(name) ? name : \"download.xlsx\";");

        // 3. 文件正在被其他进程占用
        log.info("\n3. 文件被占用");
        log.info("   处理方式:");
        log.info("     try (FileInputStream fis = new FileInputStream(file)) {{");
        log.info("         // 读取文件");
        log.info("     }} catch (IOException e) {{");
        log.info("         throw new BusinessException(\"文件无法访问\");");
        log.info("     }}");

        // 4. 磁盘空间不足
        log.info("\n4. 磁盘空间不足 (生成临时文件时)");
        log.info("   处理方式:");
        log.info("     File dir = new File(tempDir);");
        log.info("     long freeSpace = dir.getFreeSpace();");
        log.info("     if (freeSpace < requiredSize) {{");
        log.info("         throw new BusinessException(\"磁盘空间不足\");");
        log.info("     }}");

        log.info("\n最佳实践:");
        log.info("  • 使用 try-with-resources 自动关闭流");
        log.info("  • 捕获并处理 IOException");
        log.info("  • 返回友好的错误信息给前端");
        log.info("  • 记录错误日志,方便排查");

        log.info("========== 测试8完成 ==========\n");
    }
}
