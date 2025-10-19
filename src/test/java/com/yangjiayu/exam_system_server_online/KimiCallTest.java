package com.yangjiayu.exam_system_server_online;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ResponseBody;
import org.checkerframework.checker.units.qual.N;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @Classname KimiCallTest
 * @Description kimi调用
 * @Date 2025/10/20 09:09
 * @Created by YangJiaYu
 */
public class KimiCallTest {

    //1.创建一个webClient网络请求对象
    private String apiKey = "sk-6nn3lH2bodhb1vN04obIWhA6mPgjBokMCB4F8vnLnqrZqDKi";

    private String kimiBaseUrl = "https://api.moonshot.cn/v1";

    //初始化webClient
    private final WebClient webClient = WebClient.builder()
        .baseUrl(kimiBaseUrl+"/chat/completions")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("Authorization","Bearer "+apiKey)
        .build();

    //2.发起网络kimi模型网络请求方法
    @Test
    public void callKimi() throws JsonProcessingException {
        //1.封装请求的数据格式（moonshot文本模型的请求数据格式固定）
        /*
        方案一：定义一个请求的数据格式vo
        方案二：使用map和集合完成json处理
        {
            "model": "kimi-k2-0905-preview",
            "messages": [
                {
                    "role": "system",
                    "content": "你是 Kimi，由 Moonshot AI 提供的人工智能助手，你更擅长中文和英文的对话。你会为用户提供安全，有帮助，准确的回答。同时，你会拒绝一切涉及恐怖主义，种族歧视，黄色暴力等问题的回答。Moonshot AI 为专有名词，不可翻译成其他语言。"
                },
                { "role": "user", "content": "你好，我叫李雷，1+1等于多少？" }
            ],
            "temperature": 0.6
        }
         */
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("model","moonshot-v1-32k");
        requestBody.put("temperature",0.3);

        List<Map<String,Object>> messages = new ArrayList<>();
        Map<String,Object>systemMessage = new HashMap<>();
        systemMessage.put("role","system");
        systemMessage.put("content","我是可爱的YJY");
        messages.add(systemMessage);

        Map<String,Object>userMessage = new HashMap<>();
        userMessage.put("role","user");
        userMessage.put("content","你是谁呢？你能干嘛呢？能哄我开心么？");
        messages.add(userMessage);

        requestBody.put("messages",messages);


        //2.利用webClient发起网络请求并且获取结果即可
        String response = webClient.post() //确定网络请求方式
            .bodyValue(requestBody) //请求体的内容 会转化为json
            .retrieve()//请求准备好了可以发送了！
            .bodyToMono(String.class)//返回结果的类型为String
//            .subscribe(p-> System.out.println()) //异步 继续向下执行
            .block();//同步获取结果

        //3.对moonshot请求的请求的数据格式进行解析（结果的格式也是固定）
//        System.out.println(response);

        //解析的思路：string->json字符串->json处理工具（jackjson）->java的对象【JsonNode】 -> 一层层获取！
        // jackson的json处理工具
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response);
        // jsonNode get（对象  asText）
        //json 数据解析
        JsonNode choices = jsonNode.get("choices");
        JsonNode result = choices.get(0);  // 使用整数索引而不是字符串"0"
        JsonNode messageNode = result.get("message");
        String content = messageNode.get("content").asText();
        System.out.println("===============");
        System.out.println(content);

    }

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


}
