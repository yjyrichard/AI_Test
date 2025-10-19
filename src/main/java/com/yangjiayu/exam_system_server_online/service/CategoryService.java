package com.yangjiayu.exam_system_server_online.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangjiayu.exam_system_server_online.entity.Category;

import java.util.List;

/**
 * 服务接口
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
public interface CategoryService extends IService<Category> {

    /**
     * 获取分类列表
     * @return
     */
    List<Category> getAllCategories();

    /**
     * 查询分类树状接口
     * @return
     */
    List<Category> findCategoryTreeList();

    void saveCategory(Category category);

    void updateCategory(Category category);

    void deleteCategory(Long id);
}
