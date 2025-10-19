package com.yangjiayu.exam_system_server_online.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.entity.Question;
import com.yangjiayu.exam_system_server_online.entity.VO.QuestionQueryVo;
import com.yangjiayu.exam_system_server_online.service.QuestionService;
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
@RequestMapping("/questions")
public class QuestionController{
    private final QuestionService questionService;

    /**
     * 分页查询题目列表（支持多条件筛选）
     * @param page 当前页码，从1开始，默认第1页
     * @param size 每页显示数量，默认10条
     * @return 封装的分页查询结果，包含题目列表和分页信息
     */
    @GetMapping("/list")  // 映射GET请求到/api/questions/list
    @Operation(summary = "分页查询题目列表", description = "支持按分类、难度、题型、关键词进行多条件筛选的分页查询")  // Swagger接口描述
    public Result<Page<Question>> getQuestionList(
        @Parameter(description = "当前页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer page,  // 参数描述
        @Parameter(description = "每页显示数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
        QuestionQueryVo questionQueryVo) {
        // 返回统一格式的成功响应
        Page<Question> questionPage = new Page<>(page, size);
        // 方案二：嵌套查询，分布查询实现 自定义多表查询语句
        //        questionService.customPageService(pageBean,questionQueryVo);
        // 方案三：使用mybatis-plus的方法全部查询，然后代码进行处理
        questionService.queryQuestionListByStream(questionPage,questionQueryVo);
        //questionService.customPageJavaService(pageBean,questionPageVo);
        log.info("分页查询数据成功！total为：{},数据为：{}" ,questionPage.getTotal(),questionPage.getRecords());
        return Result.success(questionPage);
    }

    /**
     * 根据ID查询单个题目详情
     * RESTful API教学：
     * - URL模式：GET /api/questions/{id}
     * - 路径参数：通过@PathVariable获取URL中的参数
     * - 语义化：URL直观表达资源和操作
     * @param id 题目主键ID，通过URL路径传递
     * @return 题目详细信息，包含选项和答案
     */
    @GetMapping("/{id}")  // {id}是路径变量，会映射到方法参数
    @Operation(summary = "根据ID查询题目详情", description = "获取指定ID的题目完整信息，包括题目内容、选项、答案等详细数据")  // API描述
    public Result<Question> getQuestionById(
        @Parameter(description = "题目ID", example = "1") @PathVariable Long id) {
        Question question =  questionService.queryQuestionById(id);
        log.info("查询题目详情调用成功！ 返回数据为：{}",question);
        return Result.success(question);
    }

    /**
     * 创建新题目
     * 事务处理：
     * - 题目创建涉及多张表（题目、选项、答案）
     * - Service层方法应该使用@Transactional保证数据一致性
     * @param question 前端提交的题目数据（JSON格式）
     * @return 创建成功后的题目信息
     */
    @PostMapping  // 映射POST请求到/api/questions
    @Operation(summary = "创建新题目", description = "添加新的考试题目，支持选择题、判断题、简答题等多种题型")  // API描述
    public Result<Question> createQuestion(@RequestBody Question question) {
        questionService.saveQuestion(question);
        log.info("保存题目信息接口调用成功！最终结果为：{}",question);
        return Result.success(question);
    }

    /**
     * 更新题目信息
     *
     * RESTful API教学：
     * - HTTP方法：PUT表示更新操作
     * - URL设计：PUT /api/questions/{id} 语义明确
     * - 参数组合：路径参数(ID) + 请求体(数据)
     *
     * @param id 要更新的题目ID
     * @param question 更新的题目数据
     * @return 更新后的题目信息
     */
    @PutMapping("/{id}")  // 处理PUT请求
    @Operation(summary = "更新题目信息", description = "修改指定题目的内容、选项、答案等信息")  // API描述
    public Result<Question> updateQuestion(
        @Parameter(description = "题目ID") @PathVariable Long id,
        @RequestBody Question question) {
        questionService.updateQuestion(question);
        log.info("修改题目信息成功！！");
        return Result.success(question);
    }

    /**
     * 删除题目
     *
     * RESTful API教学：
     * - HTTP方法：DELETE表示删除操作
     * - 响应设计：删除成功返回确认消息，失败返回错误信息
     *
     * 注意事项：
     * - 删除前应检查题目是否被试卷引用
     * - 考虑使用逻辑删除而非物理删除，保留数据完整性
     *
     * @param id 要删除的题目ID
     * @return 删除操作结果
     */
    @DeleteMapping("/{id}")  // 处理DELETE请求
    @Operation(summary = "删除题目", description = "根据ID删除指定的题目，包括关联的选项和答案数据")  // API描述
    public Result<String> deleteQuestion(
        @Parameter(description = "题目ID") @PathVariable Long id) {
        questionService.removeQuestion(id);
        log.info("删除指定id:{} 题目信息成功！！",id);
        // 根据操作结果返回不同的响应
        return Result.success("题目删除成功");
    }


    /**
     * 获取热门题目 - 首页展示推荐题目
     *
     * 业务逻辑：
     * - 热门度定义：按创建时间倒序，展示最新题目
     * - 可扩展：未来可按答题次数、正确率等指标排序
     *
     * SQL优化教学：
     * - 使用LIMIT限制结果集大小，提高查询性能
     * - 建议在create_time字段上建立索引
     *
     * @param size 返回题目数量，默认6条（适合首页展示）
     * @return 热门题目列表
     */
    @GetMapping("/popular")  // 处理GET请求
    @Operation(summary = "获取热门题目", description = "获取访问次数最多的热门题目，用于首页推荐展示")  // API描述
    public Result<List<Question>> getPopularQuestions(
        @Parameter(description = "返回题目数量", example = "10") @RequestParam(defaultValue = "6") Integer size) {
        List<Question> questionList =  questionService.FindPopularQuestions(size);
        log.info("查询热门题目接口调用成功！热门题目数量：{},具体数据集合为：{}",questionList.size(),questionList);
        // 异常处理：记录日志并返回友好的错误信息
        return Result.success(questionList);
    }
}