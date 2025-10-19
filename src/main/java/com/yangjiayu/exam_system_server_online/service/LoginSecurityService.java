package com.yangjiayu.exam_system_server_online.service;

import com.yangjiayu.exam_system_server_online.config.properties.LoginSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 登录安全服务
 *
 * 基于 Redis + 滑动窗口算法实现登录失败检测与账户锁定功能
 * 用于防止暴力破解和恶意登录攻击
 *
 * 核心功能:
 * 1. 记录登录失败信息（使用Redis Sorted Set）
 * 2. 统计时间窗口内的失败次数（滑动窗口）
 * 3. 达到阈值时自动锁定账户
 * 4. 检查账户锁定状态
 * 5. 登录成功后清除失败记录
 *
 * @author Yangjiayu
 * @description 登录安全防护服务 - 防止暴力破解
 */
@Service
@Slf4j
@RequiredArgsConstructor  // Lombok注解，自动生成包含final字段的构造函数
public class LoginSecurityService {

    // 使用构造函数注入依赖，保证依赖的不可变性和线程安全
    private final StringRedisTemplate redisTemplate;
    private final LoginSecurityProperties properties;

    // ============== Redis Key 前缀定义 ==============
    /**
     * 登录失败记录的Key前缀
     * 完整格式: login:fail:username
     * 例如: login:fail:admin
     */
    private static final String FAIL_KEY_PREFIX = "login:fail:";

    /**
     * 账户锁定的Key前缀
     * 完整格式: account:lock:username
     * 例如: account:lock:admin
     */
    private static final String LOCK_KEY_PREFIX = "account:lock:";

    /**
     * IP失败记录的Key前缀（预留，暂不使用）
     * 完整格式: login:fail:ip:192.168.1.1
     */
    private static final String IP_FAIL_KEY_PREFIX = "login:fail:ip:";

    // ============== 核心方法 ==============

    /**
     * 记录登录失败
     *
     * 使用Redis Sorted Set（有序集合）存储失败记录
     * - member（成员）: 当前时间戳的字符串形式
     * - score（分数）: 当前时间戳的数值，用于排序和范围查询
     *
     * 滑动窗口算法流程:
     * 1. 添加当前失败记录到Sorted Set
     * 2. 删除时间窗口之外的旧记录
     * 3. 统计窗口内的失败次数
     * 4. 判断是否需要锁定账户
     *
     * @param username 用户名
     * @param ip       客户端IP地址，用于日志记录
     */
    public void recordLoginFail(String username, String ip) {
        // 构建Redis Key: login:fail:username
        String key = FAIL_KEY_PREFIX + username;

        // 获取当前时间戳（毫秒）
        // 时间戳既作为成员值，也作为分数值
        Long currentTime = System.currentTimeMillis();

        /*
         * Redis命令: ZADD key score member
         * 功能: 将一个或多个成员及其分数加入到有序集合
         *
         * 这里的实现:
         * - key: login:fail:admin
         * - score: 1734672000000 (时间戳数值，用于排序)
         * - member: "1734672000000" (时间戳字符串，作为唯一标识)
         *
         * 为什么用时间戳作为score?
         * - 方便按时间范围删除旧记录 (ZREMRANGEBYSCORE)
         * - 自然按时间排序
         */
        redisTemplate.opsForZSet().add(key, currentTime.toString(), currentTime);

        /*
         * Redis命令: ZREMRANGEBYSCORE key min max
         * 功能: 删除分数在指定区间内的所有成员
         *
         * 实现滑动窗口的关键步骤:
         * - min: 0 (最小时间)
         * - max: currentTime - timeWindow * 1000 (窗口起始时间)
         *
         * 例如: 当前时间是 1734672000000，窗口是10秒
         * - 删除分数 < 1734671990000 的所有记录
         * - 保留最近10秒内的记录
         *
         * 这样就实现了"滑动窗口"效果:
         * 随着时间推移，旧记录自动被移出窗口
         */
        long windowStartTime = currentTime - properties.getTimeWindow() * 1000L;
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStartTime);

        /*
         * Redis命令: EXPIRE key seconds
         * 功能: 设置键的过期时间
         *
         * 为什么要设置过期时间?
         * - 防止Redis内存泄漏
         * - 即使用户长期不登录，失败记录也会自动清理
         * - 1小时后自动删除整个Key，释放内存
         */
        redisTemplate.expire(key, 1, TimeUnit.HOURS);

        /*
         * Redis命令: ZCARD key
         * 功能: 获取有序集合的成员数量
         *
         * 返回值就是当前时间窗口内的失败次数
         * 因为旧记录已经被删除了
         */
        Long failCount = redisTemplate.opsForZSet().zCard(key);

        // 记录详细的失败日志，便于后续审计和分析
        log.warn("【登录安全】用户登录失败 - 用户名: {}, IP: {}, {}秒内失败次数: {}, 时间: {}",
                username,
                ip,
                properties.getTimeWindow(),
                failCount,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        // 判断失败次数是否达到锁定阈值
        if (failCount != null && failCount >= properties.getMaxFailCount()) {
            // 达到阈值，锁定账户
            lockAccount(username, ip, failCount);
        }
    }

    /**
     * 检查账户是否被锁定
     *
     * @param username 用户名
     * @return true: 账户已锁定, false: 账户未锁定
     */
    public boolean isAccountLocked(String username) {
        // 构建锁定Key
        String lockKey = LOCK_KEY_PREFIX + username;

        /*
         * Redis命令: EXISTS key
         * 功能: 检查键是否存在
         *
         * 返回值:
         * - true: 键存在，表示账户被锁定
         * - false: 键不存在，表示账户未锁定
         *
         * 注意: Boolean.TRUE.equals() 用于安全地处理null值
         */
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * 获取账户剩余锁定时间
     *
     * @param username 用户名
     * @return 剩余锁定时间（秒），如果未锁定则返回-2
     */
    public Long getRemainingLockTime(String username) {
        String lockKey = LOCK_KEY_PREFIX + username;

        /*
         * Redis命令: TTL key
         * 功能: 获取键的剩余生存时间（秒）
         *
         * 返回值:
         * - 正数: 剩余秒数
         * - -1: 键存在但没有设置过期时间
         * - -2: 键不存在
         */
        return redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
    }

    /**
     * 锁定账户（私有方法）
     *
     * 当失败次数达到阈值时调用此方法
     *
     * @param username  用户名
     * @param ip        客户端IP
     * @param failCount 失败次数
     */
    private void lockAccount(String username, String ip, Long failCount) {
        String lockKey = LOCK_KEY_PREFIX + username;

        /*
         * Redis命令: SET key value EX seconds
         * 功能: 设置键值，并设置过期时间
         *
         * 参数说明:
         * - key: account:lock:admin
         * - value: "locked" (标记值)
         * - timeout: 15 (分钟)
         * - unit: TimeUnit.MINUTES
         *
         * 效果: 15分钟后Redis自动删除该key，账户自动解锁
         */
        redisTemplate.opsForValue().set(
                lockKey,
                "locked",
                properties.getLockDuration(),
                TimeUnit.MINUTES
        );

        // 记录账户锁定日志（警告级别）
        log.warn("【登录安全】账户已锁定 - 用户名: {}, IP: {}, 失败次数: {}, 锁定时长: {}分钟, 时间: {}",
                username,
                ip,
                failCount,
                properties.getLockDuration(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        // ============ 安全通知（预留接口）============
        // TODO: 这里可以发送邮件或短信通知用户
        // 当前仅打印日志，后续可扩展为真实的通知功能
        log.info("【安全通知】用户 {} 的账户因多次登录失败已被锁定，如非本人操作请及时联系管理员。", username);
    }

    /**
     * 清除登录失败记录
     *
     * 使用场景:
     * 1. 用户登录成功后调用，清除历史失败记录
     * 2. 管理员手动解锁账户时调用
     *
     * @param username 用户名
     */
    public void clearLoginFail(String username) {
        String key = FAIL_KEY_PREFIX + username;

        /*
         * Redis命令: DEL key
         * 功能: 删除指定的键
         *
         * 删除整个Sorted Set，清空所有失败记录
         */
        redisTemplate.delete(key);

        log.info("【登录安全】清除失败记录 - 用户名: {}, 时间: {}",
                username,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    /**
     * 手动解锁账户
     *
     * 使用场景:
     * 1. 管理员手动解锁被锁定的账户
     * 2. 用户申诉后解锁
     *
     * @param username 用户名
     */
    public void unlockAccount(String username) {
        String lockKey = LOCK_KEY_PREFIX + username;

        // 删除锁定标记
        redisTemplate.delete(lockKey);

        // 同时清除失败记录
        clearLoginFail(username);

        log.info("【登录安全】账户已解锁 - 用户名: {}, 操作: 管理员手动解锁, 时间: {}",
                username,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    /**
     * 获取当前失败次数（用于测试和监控）
     *
     * @param username 用户名
     * @return 时间窗口内的失败次数
     */
    public Long getFailCount(String username) {
        String key = FAIL_KEY_PREFIX + username;
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0L;
    }
}
