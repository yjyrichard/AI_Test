package com.yangjiayu.exam_system_server_online.config;

import com.yangjiayu.exam_system_server_online.config.properties.KimiProperties;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @Classname WebClientConfiguration
 * @Description webClient加入核心容器
 * @Date 2025/10/20 10:27
 * @Created by YangJiaYu
 */
@Configuration
@EnableConfigurationProperties(KimiProperties.class)
@AllArgsConstructor
public class WebClientConfiguration {

    private KimiProperties kimiProperties;

    @Bean
    public WebClient webClient() {
        return  WebClient.builder()
            .baseUrl(kimiProperties.getUri())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Authorization","Bearer "+kimiProperties.getApiKey())
            .build();
    }

}
