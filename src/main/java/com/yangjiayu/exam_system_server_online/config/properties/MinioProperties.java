package com.yangjiayu.exam_system_server_online.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * description: 读取Minio相关的参数
 */
@ConfigurationProperties(prefix = "minio")
@Data
//@Component // <--- 确保这个注解存在且生效  【饿汉】 @EnableConfigurationProperties(MinioProperties.class)[懒汉]
public class MinioProperties {

//    端点 minio.endpoint  账号 minio.username 密码 minio.password  桶名 mimio.bucket-name
    private String endpoint;
    private String username;
    private String password;
    // @Value() //依赖注入！ 非引用类型！
    private String bucketName;

    /*
         yaml              java         数据库
       bucket-name       bucketName    bucket_name
     */
}
