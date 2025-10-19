package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.entity.ExamRecord;
import com.yangjiayu.exam_system_server_online.service.ExamService;
import com.yangjiayu.exam_system_server_online.vo.StartExamVo;
import com.yangjiayu.exam_system_server_online.vo.SubmitAnswerVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Classname ExamController
 * @Description 考试控制器 - 处理考试流程相关的HTTP请求 包括开始考试 提交答案 AI批阅，成绩查询等功能
 * @Date 2025/10/20 20:24
 * @Created by YangJiaYu
 */
@Slf4j
@RestController
@RequestMapping("/exams")
@AllArgsConstructor
@Tag(name="考试管理",description = "考试流程相关操作，包括开始考试，答题提交，AI批阅，成绩查询等功能")
public class ExamController {

    private ExamService examService;

    /**
     * 开始考试 - 创建新的考试记录
     * @param startExamVo 开始考试请求DTO
     * @return 考试记录
     */
    @PostMapping("/start")  // 处理POST请求
    @Operation(summary = "开始考试", description = "学生开始考试，创建考试记录并返回试卷内容")  // API描述
    public Result<ExamRecord> startExam(@RequestBody StartExamVo startExamVo) {
        ExamRecord examRecord =  examService.startExam(startExamVo);
        log.info("开始考试，考试对象创建成功！{}",examRecord);
        return Result.success(examRecord, "考试开始成功");
    }

    /**
     * 根据ID获取考试记录详情 - 查询具体考试结果
     */
    @GetMapping("/{id}")  // 处理GET请求
    @Operation(summary = "查询考试记录详情", description = "获取指定考试记录的详细信息，包括答题情况和得分")  // API描述
    public Result<ExamRecord> getExamRecordById(
        @Parameter(description = "考试记录ID") @PathVariable Integer id) {
        ExamRecord examRecord = examService.customGetExamRecordById(id);
        log.info("获取试卷详情信息接口调用成功！数据为：{}",examRecord);
        return Result.success(examRecord);
    }

    /**
     * 提交答案 - 学生提交考试答案
     * @param examRecordId 考试记录ID
     * @param answers      答案列表
     */
    @PostMapping("/{examRecordId}/submit")  // 处理POST请求
    @Operation(summary = "提交考试答案", description = "学生提交考试答案，系统记录答题情况")  // API描述
    public Result<Void> submitAnswers(
        @Parameter(description = "考试记录ID") @PathVariable Integer examRecordId,
        @RequestBody List<SubmitAnswerVo> answers) throws InterruptedException {
        examService.customSubmitAnswer(examRecordId,answers);
        log.info("提交答案接口调用成功！");
        return Result.success("答案提交成功");
    }


    /**
     * 删除考试记录
     */
    @DeleteMapping("/{id}")  // 处理DELETE请求
    @Operation(summary = "删除考试记录", description = "根据ID删除指定的考试记录")  // API描述
    public Result<Void> deleteExamRecord(
        @Parameter(description = "考试记录ID") @PathVariable Integer id) {
        examService.customRemoveById(id);
        log.info("删除考试记录成功！id:{}",id);
        return Result.success("删除考试记录成功！id:{}");
    }
}
