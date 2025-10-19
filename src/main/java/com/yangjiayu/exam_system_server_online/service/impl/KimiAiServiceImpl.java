package com.yangjiayu.exam_system_server_online.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yangjiayu.exam_system_server_online.config.properties.KimiProperties;
import com.yangjiayu.exam_system_server_online.entity.Question;
import com.yangjiayu.exam_system_server_online.service.KimiAiService;
import com.yangjiayu.exam_system_server_online.vo.AiGenerateRequestVo;
import com.yangjiayu.exam_system_server_online.vo.QuestionImportVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Kimi AI生成服务实现类
 */
@Slf4j
@Service
@AllArgsConstructor
public class KimiAiServiceImpl implements KimiAiService {

    private final WebClient webClient;
    private final KimiProperties kimiProperties;


    /**
     * 构建发送给AI的提示词
     */
    @Override
    public String buildPrompt(AiGenerateRequestVo request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请为我生成").append(request.getCount()).append("道关于【")
              .append(request.getTopic()).append("】的题目。\n\n");
        
        prompt.append("要求：\n");
        
        // 题目类型要求
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            List<String> typeList = Arrays.asList(request.getTypes().split(","));
            prompt.append("- 题目类型：");
            for (String type : typeList) {
                switch (type.trim()) {
                    case "CHOICE":
                        prompt.append("选择题");
                        if (request.getIncludeMultiple() != null && request.getIncludeMultiple()) {
                            prompt.append("(包含单选和多选)");
                        }
                        prompt.append(" ");
                        break;
                    case "JUDGE":
                        prompt.append("判断题（**重要：确保正确答案和错误答案的数量大致平衡，不要全部都是正确或错误**） ");
                        break;
                    case "TEXT":
                        prompt.append("简答题 ");
                        break;
                }
            }
            prompt.append("\n");
        }
        
        // 难度要求
        if (request.getDifficulty() != null) {
            String difficultyText = switch (request.getDifficulty()) {
                case "EASY" -> "简单";
                case "MEDIUM" -> "中等";
                case "HARD" -> "困难";
                default -> "中等";
            };
            prompt.append("- 难度等级：").append(difficultyText).append("\n");
        }
        
        // 额外要求
        if (request.getRequirements() != null && !request.getRequirements().isEmpty()) {
            prompt.append("- 特殊要求：").append(request.getRequirements()).append("\n");
        }
        
        // 判断题特别要求
        if (request.getTypes() != null && request.getTypes().contains("JUDGE")) {
            prompt.append("- **判断题特别要求**：\n");
            prompt.append("  * 确保生成的判断题中，正确答案(TRUE)和错误答案(FALSE)的数量尽量平衡\n");
            prompt.append("  * 不要所有判断题都是正确的或都是错误的\n");
            prompt.append("  * 错误的陈述应该是常见的误解或容易混淆的概念\n");
            prompt.append("  * 正确的陈述应该是重要的基础知识点\n");
        }
        
        prompt.append("\n请严格按照以下JSON格式返回，不要包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"题目内容\",\n");
        prompt.append("      \"type\": \"CHOICE|JUDGE|TEXT\",\n");
        prompt.append("      \"multi\": true/false,\n");
        prompt.append("      \"difficulty\": \"EASY|MEDIUM|HARD\",\n");
        prompt.append("      \"score\": 5,\n");
        prompt.append("      \"choices\": [\n");
        prompt.append("        {\"content\": \"选项内容\", \"isCorrect\": true/false, \"sort\": 1}\n");
        prompt.append("      ],\n");
        prompt.append("      \"answer\": \"TRUE或FALSE(判断题专用)|文本答案(简答题专用)\",\n");
        prompt.append("      \"analysis\": \"题目解析\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("注意：\n");
        prompt.append("1. 选择题必须有choices数组，判断题和简答题设置answer字段\n");
        prompt.append("2. 多选题的multi字段设为true，单选题设为false\n");
        prompt.append("3. **判断题的answer字段只能是\"TRUE\"或\"FALSE\"，请确保答案分布合理**\n");
        prompt.append("4. 每道题都要有详细的解析\n");
        prompt.append("5. 题目要有实际价值，贴近实际应用场景\n");
        prompt.append("6. 严格按照JSON格式返回，确保可以正确解析\n");
        
        // 如果只生成判断题，额外强调答案平衡
        if (request.getTypes() != null && request.getTypes().equals("JUDGE") && request.getCount() > 1) {
            prompt.append("7. **判断题答案分布要求**：在").append(request.getCount()).append("道判断题中，");
            int halfCount = request.getCount() / 2;
            if (request.getCount() % 2 == 0) {
                prompt.append("请生成").append(halfCount).append("道正确(TRUE)和").append(halfCount).append("道错误(FALSE)的题目");
            } else {
                prompt.append("请生成约").append(halfCount).append("-").append(halfCount + 1).append("道正确(TRUE)和约").append(halfCount).append("-").append(halfCount + 1).append("道错误(FALSE)的题目");
            }
        }
        
        return prompt.toString();
    }

    // 进行kimi调用的时候，失败了有3次机会，三次机会都失败了就彻底失败
    @Override
    public String callKimiAI(String prompt) throws InterruptedException {
        // 1.构建重试代码结构
        int maxTry = 3;//最多三次

        for(int i = 1; i <= maxTry; i++) {

            try{
               //2.webClient进行kimi调用
                Map<String,Object> requestBody = new HashMap<>();
                requestBody.put("model",kimiProperties.getModel());
                requestBody.put("temperature",kimiProperties.getTemperature());
                requestBody.put("max_tokens",4096);//设置相应数据tokens最大值

                List<Map<String,Object>> messages = new ArrayList<>();

                Map<String,Object>userMessage = new HashMap<>();
                userMessage.put("role","user");
                userMessage.put("content",prompt);
                messages.add(userMessage);

                requestBody.put("messages",messages);


                //2.利用webClient发起网络请求并且获取结果即可
                String response = webClient.post() //确定网络请求方式
                    .bodyValue(requestBody) //请求体的内容 会转化为json
                    .retrieve()//请求准备好了可以发送了！
                    .bodyToMono(String.class)//返回结果的类型为String
//            .subscribe(p-> System.out.println()) //异步 继续向下执行
                    .block();//同步获取结果

               //3.结果进行解析和处理（成功|失败）
                //成功还是失败，返回的都是jsonObject对象！
                //1.转成JSONObject 2.判断里面有没有error的key 3.有=》失败 4.没有 =》成功
                //jackson => objectMapper
                //fastJson => json工具包 => jsonObject => 字符串
                JSONObject resultObject = JSONObject.parseObject(response);

                if(resultObject.containsKey("error")){
                    //证明这一次请求就失败了
                    // 抛出异常=》catch=>统一进行失败次数检查和是否尝试的处理
                    String errorMessage = resultObject.getJSONObject("error").getString("message");
                    throw new RuntimeException(errorMessage);
                }
                //失败【限速 参数 key没有钱了】
                /*
                {
                    "error": {
                        "type": "content_filter",
                        "message": "The request was rejected because it was considered high risk"
                    }
                }
                 */

                //成功
                /*
                {
                    "id": "cmpl-04ea926191a14749b7f2c7a48a68abc6",
                    "object": "chat.completion",
                    "created": 1698999496,
                    "model": "kimi-k2-0905-preview",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": " 你好，李雷！1+1等于2。如果你有其他问题，请随时提问！"
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 19,
                        "completion_tokens": 21,
                        "total_tokens": 40
                    }
                }
                 */
                String content = resultObject.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
                if(ObjectUtils.isEmpty(content)){
                    throw new RuntimeException("返回结果结构正确，但是返回数据为空！再次尝试！");
                }
                return content;
            }catch (Exception e){
                // 失败
                log.error("第{}次尝试调用kimi模型失败！失败的错误信息为：{}",i,e.getMessage());
                //0.i==maxTry 最后一次重试机会
                if(i==maxTry){
                    throw new RuntimeException("已经重试了%s次调用kimi的模型，但是依然没有正确的返回结果！".formatted(maxTry));
                }
                //1.第几次重试失败了
                //2.建议线程休眠1s(限速)
                Thread.sleep(1000);
            }
        }
        // 循环中 没有正确的结果返回 都是失败！
        throw new RuntimeException("已经重试了%s次调用kimi的模型，但是依然没有正确的返回结果！".formatted(maxTry));

        //2.webClient进行kimi调用
    }


    @Override
    public List<QuestionImportVo> aiGenerateQuestions(AiGenerateRequestVo request) throws InterruptedException {

        //1.生成对应的提示词
        String prompt = buildPrompt(request);
        log.debug("ai出题额条件是：{}，生成对应的提示词为：{}",request,prompt);

        //2.调用ai模型获取结果
        String response = callKimiAI(prompt);

        //3.继续结果的解析即可
        /*
        ```json
            {
                question:[
                    {


                    }
                ]

            }


         */
        //3.1 判定开始(```json)和结束字符的位置(```)
        int startIndex = response.indexOf("```json");
        int endIndex = response.lastIndexOf("```");  // 使用 lastIndexOf 找到最后一个 ```

        if(startIndex != -1 && endIndex != -1 && startIndex < endIndex){
            //数据结构是正确的
            //+7 排除```json
            String resultJson = response.substring(startIndex + 7, endIndex);
            //fastjson
            JSONObject resultObject = JSONObject.parseObject(resultJson);
            JSONArray questions = resultObject.getJSONArray("questions");
            List<QuestionImportVo> questionImportVoList = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                //循环解析内容{}=》QuestionImportVo
                JSONObject itemObject = questions.getJSONObject(i);
                QuestionImportVo questionImportVo = new QuestionImportVo();
                questionImportVo.setTitle(itemObject.getString("title"));
                questionImportVo.setType(itemObject.getString("type"));
                questionImportVo.setMulti(itemObject.getBoolean("multi"));
                questionImportVo.setCategoryId(request.getCategoryId());
                questionImportVo.setDifficulty(itemObject.getString("difficulty"));
                questionImportVo.setScore(itemObject.getInteger("score"));
                questionImportVo.setAnalysis(itemObject.getString("analysis"));
                questionImportVo.setAnswer(itemObject.getString("answer"));
                // 选择题 选项
                if("CHOICE".equals(questionImportVo.getType())){
                    //获取选项的JSONArray
                    JSONArray choices = itemObject.getJSONArray("choices");
                    List<QuestionImportVo.ChoiceImportDto>choiceImportDtoList = new ArrayList<>();
                    for (int j = 0; j < choices.size(); j++) {
                        JSONObject choiceObject = choices.getJSONObject(j);
                        QuestionImportVo.ChoiceImportDto choiceImportDto = new QuestionImportVo.ChoiceImportDto();
                        choiceImportDto.setContent(choiceObject.getString("content"));
                        choiceImportDto.setIsCorrect(choiceObject.getBoolean("isCorrect"));  // 从 choiceObject 获取，不是 itemObject
                        choiceImportDto.setSort(choiceObject.getInteger("sort"));
                        choiceImportDtoList.add(choiceImportDto);  // 添加到列表中！
                    }
                    questionImportVo.setChoices(choiceImportDtoList);

                }
                questionImportVoList.add(questionImportVo);
            }
            return questionImportVoList;
        }
        throw new RuntimeException("ai生成题目的结果结构错误，无法进行解析！具体数据为：%s".formatted(response));
    }



    /**
     * 构建判卷提示词
     */
    @Override
     public String buildGradingPrompt(Question question, String userAnswer, Integer maxScore) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名专业的考试阅卷老师，请对以下题目进行判卷：\n\n");

        prompt.append("【题目信息】\n");
        prompt.append("题型：").append(getQuestionTypeText(question.getType())).append("\n");
        prompt.append("题目：").append(question.getTitle()).append("\n");
        prompt.append("标准答案：").append(question.getAnswer().getAnswer()).append("\n");
        prompt.append("满分：").append(maxScore).append("分\n\n");

        prompt.append("【学生答案】\n");
        prompt.append(userAnswer.trim().isEmpty() ? "（未作答）" : userAnswer).append("\n\n");

        prompt.append("【判卷要求】\n");
        if ("CHOICE".equals(question.getType()) || "JUDGE".equals(question.getType())) {
            prompt.append("- 客观题：答案完全正确得满分，答案错误得0分\n");
        } else if ("TEXT".equals(question.getType())) {
            prompt.append("- 主观题：根据答案的准确性、完整性、逻辑性进行评分\n");
            prompt.append("- 答案要点正确且完整：80-100%分数\n");
            prompt.append("- 答案基本正确但不够完整：60-80%分数\n");
            prompt.append("- 答案部分正确：30-60%分数\n");
            prompt.append("- 答案完全错误或未作答：0分\n");
        }

        prompt.append("\n请按以下JSON格式返回判卷结果：\n");
        prompt.append("{\n");
        prompt.append("  \"score\": 实际得分(整数),\n");
        prompt.append("  \"feedback\": \"具体的评价反馈(50字以内)\",\n");
        prompt.append("  \"reason\": \"扣分原因或得分依据(30字以内)\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 获取题目类型文本
     */
    private String getQuestionTypeText(String type) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("CHOICE", "选择题");
        typeMap.put("JUDGE", "判断题");
        typeMap.put("TEXT", "简答题");
        return typeMap.getOrDefault(type, "未知题型");
    }

    @Override
    public String buildSummaryPrompt(Integer totalScore, Integer maxScore, Integer questionCount,
        Integer correctCount) {
        double percentage = (double) totalScore / maxScore * 100;

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深的教育专家，请为学生的考试表现提供专业的总评和学习建议：\n\n");

        prompt.append("【考试成绩】\n");
        prompt.append("总得分：").append(totalScore).append("/").append(maxScore).append("分\n");
        prompt.append("得分率：").append(String.format("%.1f", percentage)).append("%\n");
        prompt.append("题目总数：").append(questionCount).append("道\n");
        prompt.append("答对题数：").append(correctCount).append("道\n\n");

        prompt.append("【要求】\n");
        prompt.append("请提供一份150字左右的考试总评，包括：\n");
        prompt.append("1. 对本次考试表现的客观评价\n");
        prompt.append("2. 指出优势和不足之处\n");
        prompt.append("3. 提供具体的学习建议和改进方向\n");
        prompt.append("4. 给予鼓励和激励\n\n");

        prompt.append("请直接返回总评内容，无需特殊格式：");

        return prompt.toString();
    }
}