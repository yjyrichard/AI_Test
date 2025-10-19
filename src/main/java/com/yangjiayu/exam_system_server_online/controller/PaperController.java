package com.yangjiayu.exam_system_server_online.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.entity.Paper;
import com.yangjiayu.exam_system_server_online.service.PaperService;
import com.yangjiayu.exam_system_server_online.vo.AiPaperVo;
import com.yangjiayu.exam_system_server_online.vo.PaperVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务控制器
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/papers")
public class PaperController  {
    private final PaperService paperService;

    /**
     * 获取所有试卷列表（支持模糊搜索和状态筛选）
     */
    @GetMapping("/list")  // 处理GET请求
    @Operation(summary = "获取试卷列表", description = "支持按名称模糊搜索和状态筛选的试卷列表查询")  // API描述
    public Result<java.util.List<Paper>> listPapers(
        @Parameter(description = "试卷名称，支持模糊查询") @RequestParam(required = false) String name,
        @Parameter(description = "试卷状态，可选值：DRAFT/PUBLISHED/STOPPED") @RequestParam(required = false) String status) {

        LambdaQueryWrapper<Paper> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!ObjectUtils.isEmpty(name), Paper::getName, name);
        queryWrapper.eq(!ObjectUtils.isEmpty(status), Paper::getStatus, status);
        List<Paper> paperList = paperService.list(queryWrapper);
        log.info("试卷列表接口调用成功！本次条件：name = {} , status = {} , 查询列表为：{}",
            name, status, paperList);
        return Result.success(paperList);
    }

    /**
     * 获取试卷详情（包含题目）
     */
    @GetMapping("/{id}")  // 处理GET请求
    @Operation(summary = "获取试卷详情", description = "获取试卷的详细信息，包括试卷基本信息和包含的所有题目")  // API描述
    public Result<Paper> getPaperById(@Parameter(description = "试卷ID") @PathVariable Long id) {
        Paper paper =  paperService.customPaperDetailById(id);
        log.info("查询试卷详情接口成功！试卷信息为:{}",paper);
        return Result.success(paper);
    }


    /**
     * 手动创建试卷
     */
    @PostMapping  // 处理POST请求
    @Operation(summary = "手动创建试卷", description = "通过手动选择题目的方式创建试卷")  // API描述
    public Result<Paper> createPaper(@RequestBody PaperVo paperVo) {
        Paper paper = paperService.customCreatePaper(paperVo);
        log.info("手动组卷成功！试卷信息为：{}",paper);
        return Result.success(paper, "试卷创建成功");
    }

    /**
     * AI智能组卷（新版）
     * @param aiPaperVo 包含试卷信息和组卷规则的数据
     * @return 创建好的试卷
     */
    @PostMapping("/ai")  // 处理POST请求
    @Operation(summary = "AI智能组卷", description = "基于设定的规则（题型分布、难度配比等）使用AI自动生成试卷")  // API描述
    public Result<Paper> createPaperWithAI(@RequestBody AiPaperVo aiPaperVo) {
        Paper paper = paperService.customAiCreatePaper(aiPaperVo);
        log.info("智能组卷成功！试卷信息为：{}",paper);
        return Result.success(paper, "AI智能组卷成功");
    }


    /**
     * 更新试卷
     * @param id 试卷ID
     * @param paperVo 试卷更新数据
     * @return 操作结果
     */
    @PutMapping("/{id}")  // 处理PUT请求
    @Operation(summary = "更新试卷信息", description = "更新试卷的基本信息和题目配置")  // API描述
    public Result<Paper> updatePaper(
        @Parameter(description = "试卷ID") @PathVariable Integer id,
        @RequestBody PaperVo paperVo) {
        Paper paper = paperService.customUpdatePaper(id,paperVo);
        log.info("试卷{}信息更新成功！！",paper);
        return Result.success(paper, "试卷更新成功");
    }

    /**
     * 更新试卷状态（发布/停止）
     * @param id 试卷ID
     * @param status 新的状态
     * @return 操作结果
     */
    @PostMapping("/{id}/status")  // 处理POST请求
    @Operation(summary = "更新试卷状态", description = "修改试卷状态：发布试卷供学生考试或停止试卷禁止考试")  // API描述
    public Result<Void> updatePaperStatus(
        @Parameter(description = "试卷ID") @PathVariable Integer id,
        @Parameter(description = "新的状态，可选值：PUBLISHED/STOPPED") @RequestParam String status) {
        paperService.customUpdatePaperStatus(id,status);
        return Result.success(null, "状态更新成功");
    }

    /**
     * 删除试卷
     * @param id 试卷ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")  // 处理DELETE请求
    @Operation(summary = "删除试卷", description = "删除指定的试卷，注意：已发布的试卷不能删除")  // API描述
    public Result<Void> deletePaper(@Parameter(description = "试卷ID") @PathVariable Integer id) {
        // 检查试卷是否存在  // 验证试卷存在性
        paperService.customRemoveId(id);
        log.info("id:{}的试卷删除成功！",id);
        return Result.success("试卷删除成功！！");
    }

}