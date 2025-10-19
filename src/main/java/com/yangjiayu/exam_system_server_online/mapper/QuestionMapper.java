package com.yangjiayu.exam_system_server_online.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yangjiayu.exam_system_server_online.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangjiayu.exam_system_server_online.entity.VO.QuestionQueryVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * (questions)数据Mapper
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
*/
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 获取每个分类的题目数量
     * @return 包含分类ID和题目数量的结果列表
     *             category_id     count
     *             List<定义一个实体类 categoryId count || Map category_id  , count>
     */
    @Select("SELECT category_id, COUNT(*) as count FROM questions where is_deleted = 0  GROUP BY category_id ; ")
    List<Map<String, Long>> getCategoryQuestionCount();

    // 定一个查询方法，还想使用mybatis-plus分页插件
    // 方法规则：返回值必须是IPage 方法名（第一个必须是IPage【分页数据第几页，每页显示条件】，其他参数）
    /**
     * 分页查询题目信息，第一步！一会要触发，根据题目id查询选项！！
     * @param page 分页对象
     * @param questionQueryVo 自己实体类,封装的查询对象！
     * @return
     */
    IPage<Question> selectQuestionPage(IPage page, @Param("queryVo") QuestionQueryVo questionQueryVo);

}










