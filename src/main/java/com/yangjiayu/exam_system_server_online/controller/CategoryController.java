package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.entity.Category;
import com.yangjiayu.exam_system_server_online.service.CategoryService;
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
@RequestMapping("/categories")
public class CategoryController  {
    private final CategoryService categoryService;

    /**
     * 获取分类列表（包含题目数量）
     * @return 分类列表数据
     */
    @GetMapping
    @Operation(summary = "获取分类列表", description = "获取所有题目分类列表，包含每个分类下的题目数量统计")
    public Result<List<Category>> getCategories() {
        // 调用Service层获取处理后的数据
        List<Category> allCategories = categoryService.getAllCategories();
        // 封装成统一响应格式返回
        return Result.success(allCategories);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取树形结构",description = "获取题目分类的树形层级结构，用于前端树形组件展示")
    public Result<List<Category>> getAllCategoriesTree() {
        List<Category>categoryList = categoryService.findCategoryTreeList();
        log.info("查询树状分类列表接口调用成功！查询数量为：{}，具体为：{}",categoryList.size(),categoryList);
        return Result.success(categoryList);
    }

    @PostMapping
    @Operation(summary = "添加新分类", description = "创建新的题目分类，支持设置父分类实现层级结构")
    public Result<Void> addCategory(@RequestBody Category category) {
        categoryService.saveCategory(category);
        return Result.success(null); // 通常创建操作成功后无需返回特定数据
    }


    /**
     * 更新分类
     * @param category 分类对象
     * @return 操作结果
     */
    @PutMapping  // 处理PUT请求
    @Operation(summary = "更新分类信息", description = "修改分类的名称、描述、排序等信息")  // API描述
    public Result<Void> updateCategory(@RequestBody Category category) {
        categoryService.updateCategory(category);
        log.info("在{}父分类下，更新{}子分类成功！",category.getParentId(),category.getName());
        return Result.success("更新分类接口调用成功！");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除指定的题目分类，注意：删除前需确保分类下没有题目")
    public Result<Void> deleteCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success(null);
    }


}