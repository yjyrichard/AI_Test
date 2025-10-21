package com.yangjiayu.exam_system_server_online.entity.VO;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

@Data
public class QuestionQueryVo {

//    @Parameter(description = "分类ID筛选条件") @RequestParam(required = false)
    private Long categoryId;
    //@Parameter(description = "难度筛选条件，可选值：EASY/MEDIUM/HARD") @RequestParam(required = false)
    private String difficulty;
    //@Parameter(description = "题型筛选条件，可选值：CHOICE/JUDGE/TEXT") @RequestParam(required = false)
    private String type;
    //@Parameter(description = "关键词搜索，对题目标题进行模糊查询") @RequestParam(required = false)
    private String keyword;
}