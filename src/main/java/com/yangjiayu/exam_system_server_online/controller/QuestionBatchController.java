package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.service.KimiAiService;
import com.yangjiayu.exam_system_server_online.service.QuestionService;
import com.yangjiayu.exam_system_server_online.utils.ExcelUtil;
import com.yangjiayu.exam_system_server_online.vo.AiGenerateRequestVo;
import com.yangjiayu.exam_system_server_online.vo.QuestionImportVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @Classname QuestionBatchController
 * @Description 题目批量处理
 * @Date 2025/10/19 21:00
 * @Created by YangJiaYu
 */
@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("questions/batch")
@Tag(name = "题目批量操作",description = "题目批量操作管理相关操作，包括Excel,AI生成题目,批量验证等功能")
public class QuestionBatchController {

    private QuestionService questionService;
    private KimiAiService kimiAiService;

    /**
     * 下载Excel导入模板
     * @return Excel模板文件
     */
    @GetMapping("/template")  // 处理GET请求
    @Operation(summary = "下载Excel导入模板", description = "下载题目批量导入的Excel模板文件")  // API描述
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        //1.获取下载模板字节数组
        byte[] template = ExcelUtil.generateTemplate();
        //2.封装ResponseEntity
        ResponseEntity<byte[]> responseEntity = ResponseEntity.ok()
            .header("Content-Disposition","attachment;filename=question_import_template.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM) //二进制文件，不确定类型！！
            .body(template);
        return responseEntity;
    }

    /**
     * 预览Excel文件内容（不入库）
     * @param file Excel文件
     * @return 解析出的题目列表
     */
    @PostMapping("/preview-excel")  // 处理POST请求
    @Operation(summary = "预览Excel文件内容", description = "解析并预览Excel文件中的题目内容，不会导入到数据库")  // API描述
    public Result<List<QuestionImportVo>> previewExcel(
        @Parameter(description = "Excel文件，支持.xls和.xlsx格式") @RequestParam("file") MultipartFile file) throws IOException {
        List<QuestionImportVo> questionImportVoList = questionService.preViewExcel(file);
        log.info("预览解析execl接口调用成功！题目数量：{},数据为：{}",questionImportVoList.size(),questionImportVoList);
        return Result.success(questionImportVoList);
    }

    /**
     * 批量导入题目（通用接口，支持Excel导入或AI生成后的确认导入）
     * @param questions 题目导入DTO列表
     * @return 导入结果
     */
    @PostMapping("/import-questions")  // 处理POST请求
    @Operation(summary = "批量导入题目", description = "将题目列表批量导入到数据库，支持Excel解析后的导入或AI生成后的确认导入")  // API描述
    public Result<String> importQuestions(@RequestBody List<QuestionImportVo> questions) {
        int successCount =  questionService.importBatchQuestions(questions);
        log.info("批量导入题目接口调用成功！ 一共：{}题目需要导入，成功导入了：{}道题！" ,questions.size(),successCount);
        return Result.success("批量导入题目接口调用成功！ 一共：%s 题目需要导入，成功导入了：%s 道题！".formatted(questions.size(),successCount));
    }

    /**
     * 使用AI生成题目（预览，不入库）
     * @param request AI生成请求参数
     * @return 生成的题目列表
     */
    @PostMapping("/ai-generate")  // 处理POST请求
    @Operation(summary = "AI智能生成题目", description = "使用AI技术根据指定主题和要求智能生成题目，支持预览后再决定是否导入")  // API描述
    public Result<List<QuestionImportVo>> generateQuestionsByAi(
        @RequestBody @Validated AiGenerateRequestVo request) throws InterruptedException {
        List<QuestionImportVo> questionImportVoList = kimiAiService.aiGenerateQuestions(request);
        log.info("使用ai生成：{} 为标题的题目成功！ 计划生成：{}道题，实际生成：{}道题！",
            request.getTopic(),request.getCount(),questionImportVoList.size());
        return Result.success(questionImportVoList);
    }



}
