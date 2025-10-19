package com.yangjiayu.exam_system_server_online.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 * 处理前后端分离项目的跨域请求问题
 *
 * @author yangjiayu
 * @since 2025-10-17
 * @description 允许所有域名、方法和请求头进行跨域访问
 */
@Configuration
public class WebCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 创建 CorsConfiguration 对象
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();

        // 允许所有源进行跨域请求（生产环境建议配置具体的前端域名）
        config.addAllowedOriginPattern("*");

        // 允许携带Cookie等凭证信息
        config.setAllowCredentials(true);

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许所有HTTP方法（GET, POST, PUT, DELETE等）
        config.addAllowedMethod("*");

        // 暴露的响应头，前端可以访问
        config.addExposedHeader("*");

        // 预检请求的有效期，单位为秒（这里设置为1小时）
        config.setMaxAge(3600L);

        // 添加映射路径，拦截所有请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 返回 CorsFilter 实例
        return new CorsFilter(source);
    }
}
