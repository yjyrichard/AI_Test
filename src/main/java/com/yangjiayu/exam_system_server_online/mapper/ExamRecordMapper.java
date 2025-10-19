package com.yangjiayu.exam_system_server_online.mapper;

import com.yangjiayu.exam_system_server_online.entity.ExamRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangjiayu.exam_system_server_online.vo.ExamRankingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (exam_records)数据Mapper
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
*/
@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {


    List<ExamRankingVO> customQueryRanking(@Param("paperId") Integer paperId, @Param("limit") Integer limit);;
}
