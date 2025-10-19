package com.yangjiayu.exam_system_server_online.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangjiayu.exam_system_server_online.entity.Category;
import com.yangjiayu.exam_system_server_online.entity.Question;
import com.yangjiayu.exam_system_server_online.mapper.CategoryMapper;
import com.yangjiayu.exam_system_server_online.mapper.QuestionMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yangjiayu.exam_system_server_online.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务接口实现
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
@Slf4j
@Service
@AllArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private CategoryMapper categoryMapper;
    private QuestionMapper questionMapper;

    @Override
    public List<Category> getAllCategories() {

        //1.查询所有分类信息集合
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);
        List<Category> categoryList = categoryMapper.selectList(queryWrapper);

        //2.查询每个分类的题目数量
        List<Map<String,Long>> mapList = questionMapper.getCategoryQuestionCount();

        //3.转换为Map便于查找
        Map<Long,Long>countMap = mapList.stream()
            .collect(Collectors.toMap(
               m->m.get("category_id"),
               m->m.get("count")
            ));

        //4.先给所有分类设置直接关联的题目数量
        for (Category category : categoryList) {
            Long id = category.getId();
            category.setCount(countMap.getOrDefault(id,0L));
        }

        //5.累加子分类的题目到父分类
        for(Category category : categoryList) {
            //如果是子分类（parentId不为0或者null）
            if(category.getParentId() != null && category.getParentId()!=0){
                Long parentId = category.getParentId();
                Long childCount = category.getCount();

                //找到父分类，累加子分类的count
                for(Category parent: categoryList){
                    if(parentId.equals(parent.getId())){
                        parent.setCount(parent.getCount()+childCount);
                        break;
                    }
                }
            }
        }
        return categoryList;
    }


    @Override
    public List<Category> findCategoryTreeList() {
        //1.查询所有分类信息集合
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);
        List<Category> categoryList = categoryMapper.selectList(queryWrapper);

        //2.查询每个分类的题目数量
        List<Map<String,Long>> mapList = questionMapper.getCategoryQuestionCount();

        //3.转换为Map便于查找
        Map<Long,Long>countMap = mapList.stream()
            .collect(Collectors.toMap(
                m->m.get("category_id"),
                m->m.get("count")
            ));

        for (Category category : categoryList) {
            Long id = category.getId();
            category.setCount(countMap.getOrDefault(id,0L));
        }

        // 4.分类信息进行分组 （parent_id）stream分组
        // key - parent_id
        // value - List<子分类>
        Map<Long, List<Category>> longListMap = categoryList.stream().collect(Collectors.groupingBy(Category::getParentId));

        // 5.筛选分类信息(获取一级分类)
        List<Category> parentCategoryList = categoryList.stream().filter(c -> c.getParentId() == 0).collect(Collectors.toList());

        // 6.给一级分类循环，获取子分类，并且计算count(父分类的count+所有子分类的count)
        for (Category parentCategory : parentCategoryList) {
            List<Category> sonCategoryList = longListMap.getOrDefault(parentCategory.getId(),new ArrayList<>());
            parentCategory.setChildren(sonCategoryList);
            // count
            Long sonCount = sonCategoryList.stream().collect(Collectors.summingLong(Category::getCount));
            parentCategory.setCount(parentCategory.getCount()+sonCount);
        }
        return parentCategoryList;
    }

    /**
     * 保存分类信息
     *   需要检查名称是否重复！
     * @param category
     */
    @Override
    public void saveCategory(Category category) {
        //1.判断同一个父类分类下不允许重名
        // parent_id = 传入 and name = 传入
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Category::getParentId, category.getParentId());
        lambdaQueryWrapper.eq(Category::getName,category.getName());
        long count = count(lambdaQueryWrapper);// count 查询存在的数量
        //知识点： 我们可以在自己的service获取自己的mapper -> CategoryMapper baseMapper = getBaseMapper();
        if (count > 0) {
            Category parent = getById(category.getParentId());
            //不能添加，同一个父类下名称重复了
            throw new RuntimeException("在%s父分类下，已经存在名为：%s的子分类，本次添加失败！".formatted(parent.getName(),category.getName()));
        }
        //2.保存
        save(category);
    }

    @Override
    public void updateCategory(Category category) {
        //1.先校验  同一父分类下！ 可以跟自己的name重复，不能跟其他的子分类name重复！
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Category::getParentId, category.getParentId()); // 同一父分类下！
        lambdaQueryWrapper.ne(Category::getId, category.getId());
        lambdaQueryWrapper.eq(Category::getName, category.getName());
        CategoryMapper categoryMapper = getBaseMapper();
        boolean exists = categoryMapper.exists(lambdaQueryWrapper);
        if (exists) {
            Category parent = getById(category.getParentId());
            //不能添加，同一个父类下名称重复了
            throw new RuntimeException("在%s父分类下，已经存在名为：%s的子分类，本次更新失败！".formatted(parent.getName(),category.getName()));
        }
        //2.再更新
        updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        //1.检查是否一级标题
        Category category = getById(id);
        if (category.getParentId() == 0){
            throw new RuntimeException("不能删除一级标题！");
        }
        //2.检查是否存在关联的题目
        LambdaQueryWrapper<Question> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Question::getCategoryId,id);
        long count = questionMapper.selectCount(lambdaQueryWrapper);
        if (count>0){
            throw new RuntimeException("当前的:%s分类，关联了%s道题目,无法删除！".formatted(category.getName(),count));
        }
        //3.以上不都不满足，删除即可【子关联数据，一并删除】
        removeById(id);
    }
}















//    @Override
//    public List<Category> getAllCategories() {
//        // 1.查询所有分类信息集合（单表操作） => mybatis-plus提供的list方法即可
//        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.orderByAsc(Category::getSort);
//        List<Category> categoryList = list(queryWrapper);// 少了count
//        // select category_id,count(*) from questions where is_delete = 0 GROUP BY category_id;
//
//        // 2,QuestionMapper定义查询方法，category_id 进行分组，并且统计每个分类下的count
//        // [map{categroy_id:14,count:1},map{categroy_id:14,count:1},map{categroy_id:14,count:1}]
//        List<Map<String, Long>> mapList = questionMapper.getCategoryQuestionCount();
//        /*
//        [
//  {
//    "category_id": 14,  // Key是列名(String)，Value是数值(Long)
//    "count": 2          // Key是列名(String)，Value是数值(Long)
//  },
//  {
//    "category_id": 15,
//    "count": 2
//  },
//  {
//    "category_id": 16,
//    "count": 1
//  }
//]
//注意看：
//
//✅ Key: "category_id" 是字符串（列名）
//✅ Value: 14 是Long类型（列的值）
//         */
//
//        // 3.题目查询的分类题目数量赋值给分类集合
//
//        // mapList -> map -> 14:1,15:2
//        Map<Long, Long> countMap = mapList.stream()
//            .collect(Collectors.toMap(m -> m.get("category_id"), m -> m.get("count")));
//
//        for(Category category:categoryList){
//            Long id = category.getId();
//            category.setCount(countMap.getOrDefault(id,0L));
//
//
//
//
////            // O（n方）
////            for(Map<String, Long> map:mapList){
////                Long categoryId = map.get("category_id");
////                if(id.equals(categoryId)){
////                    category.setCount(map.get("count"));
////                }
////            }
//        }
//        // 4. 返回对应的查询集合即可
//        return categoryList;
//    }
