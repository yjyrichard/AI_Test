package com.yangjiayu.exam_system_server_online.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @Classname MybatisPlusConfiguration
 * @Description MP配置类
 * @Date 2025/10/15 20:46
 * @Created by YangJiaYu
 */
@Configuration
@MapperScan(basePackages = "com.yangjiayu.exam_system_server_online.mapper")
public class MybatisPlusConfiguration {


}
