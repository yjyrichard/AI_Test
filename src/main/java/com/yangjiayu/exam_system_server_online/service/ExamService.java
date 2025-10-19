package com.yangjiayu.exam_system_server_online.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yangjiayu.exam_system_server_online.entity.Exam;
import com.yangjiayu.exam_system_server_online.entity.ExamRecord;
import com.yangjiayu.exam_system_server_online.entity.QuestionAnswer;
import com.yangjiayu.exam_system_server_online.vo.ExamRankingVO;
import com.yangjiayu.exam_system_server_online.vo.StartExamVo;
import com.yangjiayu.exam_system_server_online.vo.SubmitAnswerVo;

import java.util.List;

/**
 * @Classname ExamService
 * @Description 考试接口层
 * @Date 2025/10/20 20:29
 * @Created by YangJiaYu
 */
public interface ExamService  extends IService<ExamRecord> {

    ExamRecord startExam(StartExamVo startExamVo);

    ExamRecord customGetExamRecordById(Integer id);

    void customSubmitAnswer(Integer examRecordId, List<SubmitAnswerVo> answers);

    /**
     * ai智能判卷
     * @param examRecordId
     * @return
     */
    ExamRecord gradeExam1(Integer examRecordId);

    void customRemoveById(Integer id);

    List<ExamRankingVO> customGetRanking(Integer paperId, Integer limit);
}
