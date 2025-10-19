package com.yangjiayu.exam_system_server_online.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 * @author: Yangjiayu
 * description: 接收kimi调用的五个参数
 */
@ConfigurationProperties(prefix = "kimi.api")
@Data
public class KimiProperties {

    private String model;
    private String uri;
    private String apiKey;
//    private Integer maxTokens;
    private Double temperature;
}
