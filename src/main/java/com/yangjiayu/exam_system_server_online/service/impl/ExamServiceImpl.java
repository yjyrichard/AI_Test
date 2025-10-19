package com.yangjiayu.exam_system_server_online.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangjiayu.exam_system_server_online.entity.AnswerRecord;
import com.yangjiayu.exam_system_server_online.entity.ExamRecord;
import com.yangjiayu.exam_system_server_online.entity.Paper;
import com.yangjiayu.exam_system_server_online.entity.Question;
import com.yangjiayu.exam_system_server_online.mapper.ExamRecordMapper;
import com.yangjiayu.exam_system_server_online.mapper.PaperMapper;
import com.yangjiayu.exam_system_server_online.service.AnswerRecordService;
import com.yangjiayu.exam_system_server_online.service.ExamService;
import com.yangjiayu.exam_system_server_online.service.KimiAiService;
import com.yangjiayu.exam_system_server_online.service.PaperService;
import com.yangjiayu.exam_system_server_online.vo.ExamRankingVO;
import com.yangjiayu.exam_system_server_online.vo.StartExamVo;
import com.yangjiayu.exam_system_server_online.vo.SubmitAnswerVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Classname ExamServiceImpl
 * @Description 考试接口实现类
 * @Date 2025/10/20 20:34
 * @Created by YangJiaYu
 */
@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper,ExamRecord> implements ExamService {

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private PaperService paperService;

    @Autowired
    private AnswerRecordService answerRecordService;

    @Autowired
    private KimiAiService kimiAiService;

    /**
     * 创建和保存考试记录业务（开始考试）
     * @param startExamVo
     * @return
     */
    @Override
    public ExamRecord startExam(StartExamVo startExamVo) {
        //1.检验考生在当前选择的试卷是否存在正在进行中的考试，存在就返回这个对象
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getStudentName,startExamVo.getStudentName());
        // 试卷id
        queryWrapper.eq(ExamRecord::getExamId,startExamVo.getPaperId());
        queryWrapper.eq(ExamRecord::getStatus,"进行中");
        ExamRecord one = getOne(queryWrapper);
        if(one!=null){
            log.debug("考生，{}在paperId={}的试卷中有正在进行的考试记录，直接返回对应的考试记录：{}",startExamVo.getStudentName(),startExamVo.getPaperId(),one.getExamId());
            return one;
        }

        //2.补全考试记录对象的属性（进行中 已完成 已批阅）
        ExamRecord examRecord = new ExamRecord();
        examRecord.setExamId(startExamVo.getPaperId()); //试卷id
        examRecord.setStudentName(startExamVo.getStudentName()); //学生姓名
        examRecord.setStatus("进行中");
        examRecord.setStartTime(LocalDateTime.now());
        examRecord.setWindowSwitches(0);//没有开发切屏


        //3.进行考试记录对象保存
        save(examRecord);

        //4.返回对应的考试记录

        return examRecord;
    }

    @Override
    public ExamRecord customGetExamRecordById(Integer id) {
        //宏观：获取考试记录，考试记录对应的试卷对象，获取考试记录对应的答题记录集合
        //注意： 答题记录和顺序和考试记录的顺序相同！
        //1. 获取考试记录详情
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new RuntimeException("开始考试的记录已经被删除！");
        }
        //2. 获取考试记录对应试卷对象详情 【试卷 题目 选项 和 答案】
        Paper paper = paperService.customPaperDetailById(examRecord.getExamId().longValue());
        if (paper == null) {
            throw new RuntimeException("当前考试记录的试卷被删除！获取考试记录详情失败！");
        }

        //3. 获取考试记录对应的答题记录集合
        LambdaQueryWrapper<AnswerRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AnswerRecord::getExamRecordId,id);

        List<AnswerRecord> answerRecords = answerRecordService.list(lambdaQueryWrapper);
        if (!ObjectUtils.isEmpty(answerRecords)){
            //[8,2,1,3,7,4] -> 题目id
            // 按照试卷中的题目顺序给答题记录排序
            // 先获取试卷中题目的id集合！【5，2，1，4，3】
            List<Long> questionIdList = paper.getQuestions().stream().map(Question::getId).collect(Collectors.toList());
            //[{questionId:1} -> 2 ,{questionId:2} -> 1 ,{questionId:3} -> 3,{questionId:4} ->5,{questionId:7} -> 4,{questionId:8} -> 0]

            // 根据考试记录对应题目在 题目集合中的顺序进行排序  《========这里值得学习一下
            //【1，2，3，4，5】 => 【2，1，4，3，0】

            answerRecords.sort((o1, o2) -> {
                // 获取 o1 的题目ID在试卷顺序列表中的索引
                int x = questionIdList.indexOf(o1.getQuestionId());

                // 获取 o2 的题目ID在试卷顺序列表中的索引
                int y = questionIdList.indexOf(o2.getQuestionId());

                // 【核心修正】比较的是在试卷中的位置（索引），而不是题目ID本身
                return Integer.compare(x, y);
            });
        }
        //4. 数据组装即可
        examRecord.setPaper(paper);
        examRecord.setAnswerRecords(answerRecords);
        return examRecord;
    }
    /*
    场景：按座位表给学生排队
假设你是一个老师，现在有一群学生刚下课，乱糟糟地站在一起（这就是 answerRecords，顺序是乱的）。

你的任务是让他们按照教室座位表的顺序排好队。

1. 你的“座位表”

你的座位表（这就是 questionIdList）是这样的：
[小张, 小李, 小王, 小赵]

2. 乱糟糟的学生

现在学生的顺序是：
[小王, 小赵, 小张, 小李]

3. 排队开始（这就是 sort 方法做的事情）

计算机的排序算法很“笨”，它一次只拉出两个学生，问：“你们俩谁应该站前面？”

假设它先拉出了 小王 和 小赵。

第一步：查座位表
它问：“小王，你在座位表第几位？” 小王说：“我第3位。” (索引是2)
它问：“小赵，你在座位表第几位？” 小赵说：“我第4位。” (索引是3)
第二步：比较位置
计算机比较这两个数字：2 和 3。
规则是：数字小的站前面。
因为 2 < 3，所以它得出结论：小王应该站在小赵前面。
于是，它把小王和小赵的顺序换了一下，现在队伍暂时变成了 [小赵, 小王, 小张, 小李]（这只是其中一步，排序算法会继续比较和交换）。

4. 关键点

你发现了吗？在整个过程中，计算机根本不关心小王和小赵谁高谁矮，谁胖谁瘦。它唯一的标准就是：查座位表，看谁的索引数字更小。

回到代码
现在我们把刚才的例子翻译成代码：



// 优化后的正确代码
answerRecords.sort((o1, o2) -> {
    // o1 就是第一个被拉出来的学生（比如小王）
    // o2 就是第二个被拉出来的学生（比如小赵）

    // 查座位表，找到小王的“位置”
    int x = questionOrderMap.get(o1.getQuestionId()); // x = 2

    // 查座位表，找到小赵的“位置”
    int y = questionOrderMap.get(o2.getQuestionId()); // y = 3

    // 比较位置，数字小的排前面
    return Integer.compare(x, y); // 2 < 3，所以 o1 (小王) 排在 o2 (小赵) 前面
});
Integer.compare(x, y) 这句话，就是计算机在说：“来，看看你们俩在座位表上的位置号，谁的位置号小，谁就站前面！”
     */

    @Override
    public void customSubmitAnswer(Integer examRecordId, List<SubmitAnswerVo> answers) {
        //宏观： 提交答案中间表保存  修改考试记录数据（已完成 ，结束时间）  触发开始判卷（examRecordId）
        //1.中间表保存问题 将集合转成AnswerRecorder集合
        if (!ObjectUtils.isEmpty(answers)) {
            List<AnswerRecord> answerRecordList = answers.stream().map(vo -> new AnswerRecord(examRecordId, vo.getQuestionId(), vo.getUserAnswer()))
                .collect(Collectors.toList());
            answerRecordService.saveBatch(answerRecordList);
        }
        //2. 暂时修改下考试记录状态（状态 -》 已完成 || 结束时间 - 设置）
        ExamRecord examRecord = getById(examRecordId);
        examRecord.setEndTime(LocalDateTime.now());
        examRecord.setStatus("已完成");
        updateById(examRecord);

        //3.调用判卷的接口
        gradeExam1(examRecordId);
    }

//    /**
//     * AI判卷方法
//     * @param examRecordId
//     * @return
//     */
////    @Override
//    public ExamRecord gradeExam(Integer examRecordId) throws InterruptedException {
//        //宏观：  获取考试记录相关的信息（考试记录对象 考试记录答题记录 考试对应试卷）
//        //  进行循环判断（1.答题记录进行修改 2.总体提到总分数 总正确数量）  修改考试记录（状态 -》 已批阅  修改 -》 总分数）   进行ai评语生成（总正确的题目数量）
//        //  修改考试记录表  返回考试记录对象
//        //1.获取考试记录和相关的信息（试卷和答题记录）
//        ExamRecord examRecord = customGetExamRecordById(examRecordId);
//        Paper paper = examRecord.getPaper();
//        if (paper == null){
//            examRecord.setStatus("已批阅");
//            examRecord.setAnswers("考试对应的试卷被删除！无法进行成绩判定！");
//            updateById(examRecord);
//            throw new RuntimeException("考试对应的试卷被删除！无法进行成绩判定！");
//        }
//        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
//        if (ObjectUtils.isEmpty(answerRecords)){
//            //没有提交
//            examRecord.setStatus("已批阅");
//            examRecord.setScore(0);
//            examRecord.setAnswers("没有提交记录！成绩为零！继续加油！");
//            updateById(examRecord);
//            return examRecord;
//        }
//
//        //2.进行循环的判卷（1.记录总分数 2.记录正确题目数量 3. 修改每个答题记录的状态（得分，是否正确 0 1 2 ，text-》ai评语））
//        int correctNumber = 0 ; //正确题目数量
//        int totalScore = 0; //总得分
//
//        //报错继续！ 某个记录错了，后续还需要继续判卷
//        //将正确题目转成map,方便每次判断获取正确答案
//        Map<Long, Question> questionMap = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, q -> q));
//
//        for (AnswerRecord answerRecord : answerRecords) {
//            try {
//                //1.先获取 答题记录对应的题目对象
//                Question question = questionMap.get(answerRecord.getQuestionId().longValue());
//                String systemAnswer = question.getAnswer().getAnswer();
//                String userAnswer = answerRecord.getUserAnswer();
//                if ("JUDGE".equalsIgnoreCase(question.getType())){
//                    //true false
//                    userAnswer = normalizeJudgeAnswer(userAnswer);
//                }
//                if (!"TEXT".equals(question.getType())) {
//                    //2.判断题目类型(选择和判断直接判卷)
//                    if (systemAnswer.equalsIgnoreCase(userAnswer)){
//                        answerRecord.setIsCorrect(1); //正确
//                        answerRecord.setScore(question.getPaperScore().intValue());
//                    }else{
//                        answerRecord.setIsCorrect(0); //正确
//                        answerRecord.setScore(0);
//                    }
//                }else{
//                    //3.简答题进行ai判断
//                    //简答题
//                    GradingResult result =
//                        kimiAiService.gradingTextQuestion(question,userAnswer,question.getPaperScore().intValue());
//                    //分
//                    answerRecord.setScore(result.getScore());
//                    //ai评价 正确  feedback  非正确 reason
//                    //是否正确 （满分 1 0分 0 其余就是2）
//                    if (result.getScore() == 0){
//                        answerRecord.setIsCorrect(0);
//                        answerRecord.setAiCorrection(result.getReason());
//                    }else if (result.getScore() == question.getPaperScore().intValue()){
//                        answerRecord.setIsCorrect(1);
//                        answerRecord.setAiCorrection(result.getFeedback());
//                    }else{
//                        answerRecord.setIsCorrect(2);
//                        answerRecord.setAiCorrection(result.getReason());
//                    }
//                }
//            } catch (Exception e) {
//                answerRecord.setScore(0);
//                answerRecord.setIsCorrect(0);
//                answerRecord.setAiCorrection("判题过程出错！");
//            }
//            //进行记录修改
//            //进行总分数赋值
//            totalScore += answerRecord.getScore();
//            //正确题目数量累加
//            if (answerRecord.getIsCorrect() == 1){
//                correctNumber++;
//            }
//        }
//        answerRecordService.updateBatchById(answerRecords);
//
//        //进行ai生成评价，进行考试记录修改和完善
//        String summary = kimiAiService.
//            buildSummary(totalScore, paper.getTotalScore().intValue(), paper.getQuestionCount(), correctNumber);
//
//        examRecord.setScore(totalScore);
//        examRecord.setAnswers(summary);
//        examRecord.setStatus("已批阅");
//        updateById(examRecord);
//
//        return examRecord;
//    }

    /**
     * 标准化判断题答案，将T/F转换为TRUE/FALSE
     * @param answer 原始答案
     * @return 标准化后的答案
     */
    private String normalizeJudgeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "";
        }

        String normalized = answer.trim().toUpperCase();
        switch (normalized) {
            case "T":
            case "TRUE":
            case "正确":
                return "TRUE";
            case "F":
            case "FALSE":
            case "错":
                return "FALSE";
            default:
                return normalized;
        }
    }

    /**
     * 获取考试排行榜 - 优化版本
     * 使用SQL关联查询，一次性获取所有需要的数据，避免N+1查询问题
     * @param paperId 试卷ID，可选参数，不传则查询所有试卷
     * @param limit 显示数量限制，可选参数，不传则返回所有记录
     * @return 排行榜列表
     */
    @Override
    public List<ExamRankingVO> customGetRanking(Integer paperId, Integer limit) {
        return examRecordMapper.customQueryRanking(paperId,limit);
    }

    @Override
    public void customRemoveById(Integer id) {
        //重要的关联数据校验，有删除失败！
        //判断自身状态，进行中不能删除
        ExamRecord examRecord = getById(id);
        if ("进行中".equals(examRecord.getStatus())){
            throw new RuntimeException("正在考试中，无法直接删除！");
        }
        //删除自身数据，同时删除答题记录
        removeById(id);
        answerRecordService.remove(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId,id));
    }

    @Override
    public ExamRecord gradeExam1(Integer examRecordId) {

        //1.获取考生的考试信息（考试记录对象，对应考试试卷（正确答案），答题记录集合（学生答案））
        ExamRecord examRecord = customGetExamRecordById(examRecordId);
        //2.校验考试记录对应的试卷是否被删除（正确答案）【已经被删除，抛出异常！已批阅 点评 对应试卷被删除无法判卷】
        Paper paper = examRecord.getPaper();
        if(paper == null){
            //已经被删除了
            examRecord.setStatus("已批阅");
            examRecord.setScore(0);
            examRecord.setAnswers("考试对应试卷已经被删除，无法判卷！");//ai 评价
            updateById(examRecord);
            log.warn("考试没有正常判定，原因id={}的考试记录对应的试卷已经被删除！！",examRecord);
            return examRecord;
        }
        //3.校验考生提交的考试试卷是否为空，为空，直接零分已批阅！
        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if (ObjectUtils.isEmpty(answerRecords)) {
            examRecord.setStatus("已批阅");
            examRecord.setScore(0);
            examRecord.setAnswers("学生没有提交考试记录，直接判0！");//ai 评价
            updateById(examRecord);
            log.warn("id={}的考试记录学生没有提交考试记录直接判0！！",examRecord);
            return examRecord;
        }

        //4.声明两个变量。记录正确题目数量 以及 总分数
        int correctCount = 0;//正确的数量
        int totalScore = 0;//总分

        //5.将试卷中question题目集合 转成 map(qiestionId,question)为了方便根据答题记录中的questionId快速获取题目对象
        Map<Long, Question> questionMap = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, q -> q));
        
        //6.循环学生的答题记录，在内部进行逐一判题，同时进行正确数量和题目分数的累加
        // 建议：容错处理！单个题错了，咱们就是这个题0分 不耽误其他题目判断
        for (AnswerRecord answerRecord : answerRecords) {
            //6.1获取答题记录对应的正确题目
            Question question = questionMap.get(answerRecord.getQuestionId().longValue());
            // 答题记录对应的题目被删除了 ，判断下一题
            if(question == null){
                continue;
            }
            //6.2 获取正确的答案和学生的答案
            String systemAnswer = question.getAnswer().getAnswer();//正确答案
            String userAnswer = answerRecord.getUserAnswer();
            // 如果是判断题，用户提交的答案T F -》TRUE 和 FALSE
            if("JUDGE".equalsIgnoreCase(question.getType())){
                userAnswer = normalizeJudgeAnswer(userAnswer);
            }

            try{
                //1.非简答题
                if(!"TEXT".equalsIgnoreCase(question.getType())){
                    //判断题 用户答案TRUE FALSE 【字符串比较】

                    //选择题：用户答案 A A,B  正确答案A A,B
                    if(userAnswer.equalsIgnoreCase(systemAnswer)){
                        //正确
                        answerRecord.setIsCorrect(1);
                        answerRecord.setScore(question.getPaperScore().intValue());
                    }else{
                        answerRecord.setIsCorrect(0);
                        answerRecord.setScore(0);
                    }
                }else{
                    //2.简答题（ai）
                    String prompt = kimiAiService.buildGradingPrompt(question, userAnswer,
                        question.getPaperScore().intValue());
                    String result = kimiAiService.callKimiAI(prompt);
                    JSONObject jsonObject = JSONObject.parseObject(result);
                    //ai给的分数
                    Integer aiScore = jsonObject.getInteger("score");
                    if(aiScore >= question.getPaperScore().intValue()){
                        //题完全正确
                        answerRecord.setScore(question.getPaperScore().intValue());
                        answerRecord.setIsCorrect(1);//完全正确
                        answerRecord.setAiCorrection(jsonObject.getString("feedback"));

                    }else if(aiScore <= 0){
                        //证明完全错误
                        answerRecord.setIsCorrect(0);
                        answerRecord.setScore(0);
                        answerRecord.setAiCorrection(jsonObject.getString("feedback"));

                    }else{
                        //部分正确
                        answerRecord.setIsCorrect(2);
                        answerRecord.setScore(aiScore);
                        answerRecord.setAiCorrection(jsonObject.getString("feedback"));

                    }

                }
            }catch (Exception e){
                //判断题目错了 给0分
                answerRecord.setScore(0);
                answerRecord.setIsCorrect(0);
                answerRecord.setAiCorrection("判断过程中报错，直接0分");
            }

            //进行题目数量的累加和得分累加
            totalScore += answerRecord.getScore();
            if (answerRecord.getIsCorrect() == 1){
                correctCount++;
            }
        }
        
        //7.修改每一条学生答题记录（分数，是否正确，简答题的ai评价）
        answerRecordService.updateBatchById(answerRecords);//答题记录的批量更新

        //8.调用kimi的模型，生成对应的ai调用设置给考试记录对象
//        String summary = "暂时不调用ai进行考试记录评价";
        String summaryPrompt = kimiAiService.buildSummaryPrompt(totalScore, paper.getTotalScore().intValue(),
            paper.getQuestionCount(), correctCount);
        String summary = null;
        try {
            summary = kimiAiService.callKimiAI(summaryPrompt);
        } catch (InterruptedException e) {
            throw new RuntimeException("调用失败"+e);
        }

        //9.更新考试记录对象即可
        examRecord.setScore(totalScore);
        examRecord.setAnswers(summary);
        examRecord.setStatus("已批阅");
        updateById(examRecord);

        //10.返回考试记录对象即可
        return examRecord;
    }

}
