package com.yangjiayu.exam_system_server_online.service;

import com.yangjiayu.exam_system_server_online.entity.Question;
import com.yangjiayu.exam_system_server_online.vo.AiGenerateRequestVo;
import com.yangjiayu.exam_system_server_online.vo.QuestionImportVo;

import java.util.List;

/**
 * Kimi AI服务接口
 * 用于调用Kimi API生成题目
 */
public interface KimiAiService {

    String buildPrompt(AiGenerateRequestVo request);

    /**
     * 封装请求kimi模型的方法
     * @param prompt
     * @return 模型反馈的结果
     */
    String callKimiAI(String prompt) throws InterruptedException;

    List<QuestionImportVo> aiGenerateQuestions(AiGenerateRequestVo request) throws InterruptedException;

    String buildGradingPrompt(Question question, String userAnswer, Integer maxScore);

    String buildSummaryPrompt(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount);
}