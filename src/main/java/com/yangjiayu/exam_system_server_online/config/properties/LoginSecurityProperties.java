package com.yangjiayu.exam_system_server_online.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录安全配置属性类
 *
 * 用于从 application.yml 中读取登录安全相关配置
 * 配置前缀: security.login
 *
 * @author Yangjiayu
 * @description 登录安全防护配置 - 防止暴力破解攻击
 */
@ConfigurationProperties(prefix = "security.login")
@Component
@Data
public class LoginSecurityProperties {

    /**
     * 时间窗口（秒）
     * 用于统计登录失败次数的时间范围
     * 例如：10秒表示在过去10秒内统计失败次数
     * 默认值：10秒
     */
    private Integer timeWindow = 10;

    /**
     * 最大失败次数
     * 在时间窗口内允许的最大登录失败次数
     * 超过此次数将触发账户锁定
     * 默认值：3次
     */
    private Integer maxFailCount = 3;

    /**
     * 账户锁定时长（分钟）
     * 当失败次数达到阈值后，账户被锁定的时长
     * 默认值：15分钟
     */
    private Integer lockDuration = 15;

    /**
     * 是否启用IP封禁功能
     * true: 启用IP封禁
     * false: 不启用IP封禁
     * 默认值：false（暂不启用）
     */
    private Boolean enableIpBan = false;

    /**
     * IP封禁阈值
     * 同一IP在时间窗口内的最大失败次数
     * 超过此次数将封禁该IP
     * 默认值：10次
     */
    private Integer ipBanThreshold = 10;

    /**
     * IP封禁时长（分钟）
     * IP被封禁后的持续时间
     * 默认值：30分钟
     */
    private Integer ipBanDuration = 30;
}
