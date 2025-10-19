package com.yangjiayu.exam_system_server_online.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangjiayu.exam_system_server_online.common.CacheConstants;
import com.yangjiayu.exam_system_server_online.entity.*;
import com.yangjiayu.exam_system_server_online.entity.VO.QuestionQueryVo;
import com.yangjiayu.exam_system_server_online.mapper.PaperQuestionMapper;
import com.yangjiayu.exam_system_server_online.mapper.QuestionAnswerMapper;
import com.yangjiayu.exam_system_server_online.mapper.QuestionChoiceMapper;
import com.yangjiayu.exam_system_server_online.mapper.QuestionMapper;
import com.yangjiayu.exam_system_server_online.utils.ExcelUtil;
import com.yangjiayu.exam_system_server_online.utils.RedisUtils;
import com.yangjiayu.exam_system_server_online.vo.QuestionImportVo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yangjiayu.exam_system_server_online.service.QuestionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.beans.Transient;
import java.io.IOException;
import java.util.*;
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
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    private QuestionMapper questionMapper;

    private QuestionChoiceMapper questionChoiceMapper;

    private QuestionAnswerMapper questionAnswerMapper;

    private PaperQuestionMapper paperQuestionMapper;

    private RedisUtils redisUtils;

    /**
     * 分页查询题目信息：方案2 进行分步查询
     * @param pageBean
     * @param questionQueryVo
     */
    @Override
    public void customPageService(Page<Question> pageBean, QuestionQueryVo questionQueryVo) {
        questionMapper.selectQuestionPage(pageBean,questionQueryVo);
    }

    /**
     * 分页查询题目信息：方案3 java代码进行处理
     * @param questionPage
     * @param questionQueryVo
     */
    @Override
    public void queryQuestionListByStream(Page<Question> questionPage, QuestionQueryVo questionQueryVo) {
        //1.题目单表的分页+动态条件查询（mybatisplus）
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        // 单个包装类型直接 判断不为null即可
        queryWrapper.eq(questionQueryVo.getCategoryId()!=null,Question::getCategoryId,questionQueryVo.getCategoryId());
        // 字符串要用ObjectUtil
        queryWrapper.eq(!ObjectUtils.isEmpty(questionQueryVo.getDifficulty()),Question::getDifficulty,questionQueryVo.getDifficulty());
        queryWrapper.eq(!ObjectUtils.isEmpty(questionQueryVo.getType()),Question::getType,questionQueryVo.getType());
        queryWrapper.like(!ObjectUtils.isEmpty(questionQueryVo.getKeyword()),Question::getTitle,questionQueryVo.getKeyword());
        queryWrapper.orderByDesc(Question::getCreateTime);
        page(questionPage,queryWrapper);

        //简单判断！ 没有满足条件的题目信息，后续没必要进行了！
        if(ObjectUtils.isEmpty(questionPage.getRecords())){
            log.debug("没有符合条件的题目信息：后面可以终止直接返回结果即可！");
            return ;
        }
        fillQuestionChoicesAndAnswer(questionPage.getRecords());

    }

    private void fillQuestionChoicesAndAnswer(List<Question>questionList) {
        //2.查询题目对应的所有的选项和所有答案（mybatisplus）
        // 我们不循环题目集合 questionPage.getRecords() 我们一次查询所有的答案和选项，进行java代码处理！
        // todo:避免1+n问题
        // 获取所有的题目ID
        List<Long> questionIds = questionList.stream().map(Question::getId).collect(Collectors.toList());
        // 查询所有选项
        LambdaQueryWrapper<QuestionChoice> questionChoiceQueryWrapper = new LambdaQueryWrapper<>();
        questionChoiceQueryWrapper.in(QuestionChoice::getQuestionId,questionIds);
        List<QuestionChoice> questionChoices = questionChoiceMapper.selectList(questionChoiceQueryWrapper);
        // 查询所有答案
        LambdaQueryWrapper<QuestionAnswer> questionAnswerQueryWrapper = new LambdaQueryWrapper<>();
        questionAnswerQueryWrapper.in(QuestionAnswer::getQuestionId,questionIds);
        List<QuestionAnswer> questionAnswers = questionAnswerMapper.selectList(questionAnswerQueryWrapper);

        //3.题目的选项和答案集合转为map格式（key=>题目id,题目对应的选项集合 | 题目对应的答案对象）
        Map<Long, QuestionAnswer> questionAnswerMap = questionAnswers.stream().collect(Collectors.toMap(
            // key                                       value
            QuestionAnswer::getQuestionId, questionAnswer -> questionAnswer
        ));

        Map<Long, List<QuestionChoice>> questionChoiceMap = questionChoices.stream()
            .collect(Collectors.groupingBy(QuestionChoice::getQuestionId));

        //4.循环题目列表，进行题目的选项和方案赋值工作

        questionList.forEach(question -> {
           // 给题目的答案赋值 [题目一定有答案]
           question.setAnswer(questionAnswerMap.get(question.getId()));
           // 给题目的选项赋值 [只有选择题才有选项！选择题 type = CHOICE]
            if("CHOICE".equals(question.getType())){
                // 只要是选项的操作，一定要考虑排序的问题！sort
                List<QuestionChoice> qc = questionChoiceMap.get(question.getId());
                // 字段进行排序 从小到大 正序 .reversed() 倒序
                qc.sort(Comparator.comparing(QuestionChoice::getSort));
                question.setChoices(qc);
            }
        });
    }

    /**
     * 查询题目的详情
     * 题目 + 答案 + 选项
     * 1：嵌套查询 连表查询 + result [可以使用 没有分页]
     * 2：嵌套查询 分步查询 【可以使用，没有必要 1 + n】
     * 3: 查询+java代码赋值即可
     * @param id
     * @return
     */
    @Override
    public Question queryQuestionById(Long id) {
//        Question question = new Question();
        // 1.查询题目详情对象
//        question =  questionMapper.selectById(id);
        Question question = getById(id);
        if(question == null){
            log.debug("查询id={}的题目不存在！",id);
            throw new RuntimeException("查询id为%s的题目已经不存在！".formatted(id));

        }

        // 2.查询题目对应的答案
        LambdaQueryWrapper<QuestionAnswer> queryWrapperAnswer = new LambdaQueryWrapper<>();
        queryWrapperAnswer.eq(QuestionAnswer::getQuestionId,id);
        QuestionAnswer questionAnswer = questionAnswerMapper.selectOne(
            queryWrapperAnswer
        );
        question.setAnswer(questionAnswer);

        // 3.查询题目对应的选项（选择题才有选项）
        if("CHOICE".equals(question.getType())){
            LambdaQueryWrapper<QuestionChoice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(QuestionChoice::getQuestionId,id);
            List<QuestionChoice> questionChoices = questionChoiceMapper.selectList(queryWrapper);
            question.setChoices(questionChoices);
        }

        // 预留：进行redis的数据缓存zset
        new Thread(()->{
           incrementQuestionScore(question.getId());
        }).start();

        return question;
    }


    /**
     * 方法进行题目加分，在排行榜中 被异步调用
     * @param questionId
     */
    private void incrementQuestionScore(Long questionId){
        Double score = redisUtils.zIncrementScore(CacheConstants.POPULAR_QUESTIONS_KEY, questionId, 1);
        log.debug("完成id:{}题目的热榜分数累计，累计后的分数为：{}",questionId,score);


    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveQuestion(Question question) {
        // 1.先判断不能重复 同一个type类型下（选择 简答 判断 ）title不能重复
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getType,question.getType());
        queryWrapper.eq(Question::getTitle,question.getTitle());
        long count = count(queryWrapper);
        if(count > 0){
            throw new RuntimeException("在%s类型下，已经存在名为%s的题目信息，保存失败！".formatted(question.getType(),question.getTitle()));
        }

        // 2.保存题目信息，先保存题目，你才有题目的id,才可以进行后续的答案和选项保存
        save(question); // mybatis-plus提供的方法 自动主键回显

        // 3.判断是不是选择题，是，根据选项的正确给答案赋值 同时将选项插入到选项表
        QuestionAnswer answer = question.getAnswer(); // 获取答案对象 如果是选择题 答案属性为""
        // 设置答案的题目ID（必须设置，否则数据库插入失败）
        answer.setQuestionId(question.getId());

        if("CHOICE".equals(question.getType())){
            List<QuestionChoice> choices = question.getChoices();
            StringBuilder sb = new StringBuilder();// 拼接正确答案 A,D
            for(int i = 0;i<choices.size();i++){
                QuestionChoice qc = choices.get(i);
                qc.setSort(i); // 0  1  2  3
                qc.setQuestionId(question.getId());
                // 保存选项 【循环+数据库保存】
                questionChoiceMapper.insert(qc);

                // A,D
                if(qc.getIsCorrect()){
                    if(sb.length()>0){
                        sb.append(",");
                    }
                    sb.append((char)('A'+i));
                }
            }
            // 通过选项给答案赋值
            answer.setAnswer(sb.toString());
        }

        // 4.完成答案数据的插入
        questionAnswerMapper.insert(answer);
    }

    @Override
    public void updateQuestion(Question question) {
        //1.进行判断 不同的题目id的title不能相同
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Question::getId,question.getId());
        queryWrapper.eq(Question::getTitle,question.getTitle());
        long count = count(queryWrapper);
        if(count > 0){
            throw new RuntimeException("修改题目的新的title:%s 已经被其他题目使用了！更新失败！".formatted(question.getTitle()));
        }
        updateById(question);

        // 答案处理
        QuestionAnswer answer = question.getAnswer();
        if("CHOICE".equals(question.getType())){
            List<QuestionChoice> questionChoices = question.getChoices();
            //删除原有的选项
            questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId,question.getId()));
            //接收新的答案
            StringBuilder sb = new StringBuilder();
            for(int i = 0;i<questionChoices.size();i++){
                QuestionChoice qc = questionChoices.get(i);
                qc.setId(null);
                qc.setCreateTime(null);
                qc.setUpdateTime(null);
                qc.setSort(i);
                qc.setQuestionId(question.getId());
                questionChoiceMapper.insert(qc);
                if(qc.getIsCorrect()){
                    if(sb.length()>0){
                        sb.append(",");

                    }
                    sb.append((char)('A'+i));
                }
            }
            answer.setAnswer(sb.toString());
        }
        // 进行答案对的更新
        questionAnswerMapper.updateById(answer);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeQuestion(Long id) {
        //1.检查是否有关联的试卷题目，有删除失败
        LambdaQueryWrapper<PaperQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaperQuestion::getQuestionId,id);
        Long l = paperQuestionMapper.selectCount(queryWrapper);
        if(l>0){
            throw new RuntimeException("id为%s的题目，被试卷正在引用，引用次数为：%s,所以删除失败！".formatted(id,l));
        }


        //2.删除题目本身
        removeById(id);

        //3.删除关联的子数据：选项和答案
        questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId,id));
        questionAnswerMapper.delete(new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId,id));
        //4.添加事务注解
    }

    @Override
    public List<Question> FindPopularQuestions(Integer size) {

        //定义一个集合存储热门题目
        List<Question>popularQuestions = new ArrayList<>();

        //1.查询redis中缓存的题目id!(按照顺序访问倒序查询)
        //有序 按照题目的访问次数倒序
        Set<Object> popularIds = redisUtils.zReverseRange(CacheConstants.POPULAR_QUESTIONS_KEY, 0, size - 1);
        //2.根据查询的id查询对应的热门题目集合
        if(!ObjectUtils.isEmpty(popularIds)) {
            List<Long> longlist = popularIds.stream().map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());
            //处理热门题目【可能热门题目对，但是热门题目的顺序会有问题】
//            List<Question> questionList = listByIds(longlist);

            for (Long id : longlist) {
                Question question = getById(id);
                // 校验：id有，但是题目已经被删除！redis的数据和数据库数据不同步问题！
                if (question != null) {
                    popularQuestions.add(question);
                }
            }
        }

        //检查热门题目集合数量是否满足size条
        int diff = size - popularQuestions.size();
        // 不满足 我们需要自己补充（提目标查询 -》 查询最新的题目）
        if(diff > 0){
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(Question::getCreateTime);//根据题目倒序

            // 已经有的id进行过滤和排除
            List<Long> existQuestionId = popularQuestions.stream().map(Question::getId).collect(Collectors.toList());
            queryWrapper.notIn(ObjectUtils.isEmpty(existQuestionId),Question::getId, existQuestionId);
            // 切割指定的diff条
            queryWrapper.last("limit " + diff);//在我们的sql语句最后加一段sql!
            List<Question> newQuestions = list(queryWrapper);
            popularQuestions.addAll(newQuestions);
        }
        //4.给题目进行选项和答案赋值即可
        fillQuestionChoicesAndAnswer(popularQuestions);
        return popularQuestions;
    }


    @Override
    public List<QuestionImportVo> preViewExcel(MultipartFile file) {
        //1.文件校验（非空 | 格式问题 xls xlsx格式结尾）
        if(file.isEmpty()){
            throw new RuntimeException("生成预览数据的表格文件为空");
        }
        String filename = file.getOriginalFilename();
        // 修复逻辑: 使用 && 表示"既不是xls也不是xlsx才报错"
        if(!filename.endsWith(".xls") && !filename.endsWith(".xlsx")){
            throw new RuntimeException("上传的文件格式错误，必须是xls或者xlsx");
        }

        //2.ExcelUtils工具类解析file输入流=> List<QuestionImportVo>
        //解析数据
        List<QuestionImportVo> questionImportVoList = null;
        try {
            questionImportVoList = ExcelUtil.parseExcel(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //返回结果
        return questionImportVoList;



        //3.返回结果

    }

    /**
     * 批量题目导入 [execl和ai生成批量导入]
     * 批量数据库添加
     * @param questions
     * @return Excel导入完成！成功导入 %d [工程导入] / %d [题目总数] 道题目
     */
    @Override
    public int importBatchQuestions(List<QuestionImportVo> questions) {
        //1. 进行数据校验
        if (questions == null || questions.isEmpty()){
            throw new RuntimeException("导入的题目集合为空！");
        }

        //3. 循环 + try 调用保存的方法 [部分成功]  //服务降级业务代码
        int successCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            try {
                //循环中vo -> question 以及进行题目保存业务的调用
                //questionImportVo -> question -> 属性 -》 question对象
                //questionImportVo -> List<ChoiceImportDto> choices -> question 对象 -》 List<QuestionChoice>choices（选择题）
                //questionImportVo -> answer 和 keyword -> Answer对象 -》 question对象 -》 answer

                //2. 进行vo - question [提取一个方法]
                Question question =  convertQuestionImportVoToQuestion(questions.get(i));
                //数据单体保存
                saveQuestion(question);
                //正确技术统计
                successCount++;
            }catch (Exception e){
                //导入失败的提示
                log.debug("{}题目导入失败！",questions.get(i).getTitle());
            }
        }
        return successCount;
    }


    private Question convertQuestionImportVoToQuestion(QuestionImportVo questionImportVo) {
        //1. 给question本体属性赋值
        Question question = new Question();
        //question.setTitle(questionImportVo.getTitle());
        /**
         * 作用：给对象的属性进行赋值！根据另一个对象的相同属性值！
         * 参数1：source 源对象 【提供值】
         * 参数2：target 目标对象 【接收值】
         */
        BeanUtils.copyProperties(questionImportVo,question);

        //2. 判断是选择，给选项集合进行赋值
        if ("CHOICE".equals(questionImportVo.getType())){
            if (questionImportVo.getChoices().size() > 0) {
                List<QuestionChoice> questionChoices = new ArrayList<>(questionImportVo.getChoices().size());
                for (QuestionImportVo.ChoiceImportDto importVoChoice : questionImportVo.getChoices()) {
                    QuestionChoice questionChoice = new QuestionChoice();
                    questionChoice.setContent(importVoChoice.getContent());
                    questionChoice.setIsCorrect(importVoChoice.getIsCorrect());
                    questionChoice.setSort(importVoChoice.getSort());
                    questionChoices.add(questionChoice);
                }
                question.setChoices(questionChoices);
            }
        }
        //3. 不管是不是选择题创建答案对象并赋值 【保存的时候，获取答案对象，选择题可以没有答案值，保存会判断答案值】
        QuestionAnswer questionAnswer = new QuestionAnswer();
        //判断题，需要将true和false转成大写！ 否则无法识别！！
        if ("JUDGE".equals(questionImportVo.getType())){
            questionAnswer.setAnswer(questionImportVo.getAnswer().toUpperCase());
        }else{
            questionAnswer.setAnswer(questionImportVo.getAnswer());
        }
        questionAnswer.setKeywords(questionImportVo.getKeywords());
        question.setAnswer(questionAnswer);

        return question;
    }


}






