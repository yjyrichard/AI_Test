package com.yangjiayu.exam_system_server_online.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yangjiayu.exam_system_server_online.entity.Question;
import com.yangjiayu.exam_system_server_online.entity.VO.QuestionQueryVo;

import java.util.List;

/**
 * 服务接口
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
public interface QuestionService extends IService<Question> {

    /**  * 分页查询题目数据  * @param pageBean  * @param questionPageDto  */
    void customPageService(Page<Question> pageBean, QuestionQueryVo questionPageVo);

    void queryQuestionListByStream(Page<Question> questionPage, QuestionQueryVo questionQueryVo);

    Question queryQuestionById(Long id);

    void saveQuestion(Question question);

    void updateQuestion(Question question);

    void removeQuestion(Long id);

    List<Question> FindPopularQuestions(Integer size);
}
