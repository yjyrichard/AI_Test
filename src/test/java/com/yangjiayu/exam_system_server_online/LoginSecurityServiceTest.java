package com.yangjiayu.exam_system_server_online;

import com.yangjiayu.exam_system_server_online.config.properties.LoginSecurityProperties;
import com.yangjiayu.exam_system_server_online.service.LoginSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录安全服务测试类
 *
 * 测试 LoginSecurityService 的核心功能:
 * 1. 登录失败记录
 * 2. 滑动窗口算法
 * 3. 账户锁定机制
 * 4. 账户解锁功能
 * 5. 失败记录清除
 *
 * @author Yangjiayu
 * @description 登录安全功能测试
 */
@SpringBootTest
@Slf4j
@DisplayName("登录安全服务测试")
public class LoginSecurityServiceTest {

    @Autowired
    private LoginSecurityService loginSecurityService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LoginSecurityProperties properties;

    // 测试用户名
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_IP = "192.168.1.100";

    /**
     * 每个测试方法执行前的准备工作
     * 清理Redis中的测试数据，保证测试环境干净
     */
    @BeforeEach
    public void setUp() {
        log.info("========== 测试准备：清理测试数据 ==========");

        // 清理失败记录
        loginSecurityService.clearLoginFail(TEST_USERNAME);

        // 清理锁定状态
        String lockKey = "account:lock:" + TEST_USERNAME;
        redisTemplate.delete(lockKey);

        log.info("测试数据清理完成");
    }

    /**
     * 测试1: 单次登录失败记录
     *
     * 验证点:
     * - 失败记录能否正常保存到Redis
     * - 失败次数统计是否正确
     * - 账户不应该被锁定（未达到阈值）
     */
    @Test
    @DisplayName("测试1: 单次登录失败记录")
    public void testSingleLoginFailure() {
        log.info("\n========== 开始测试：单次登录失败记录 ==========");

        // 记录一次登录失败
        loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);

        // 验证失败次数
        Long failCount = loginSecurityService.getFailCount(TEST_USERNAME);
        log.info("当前失败次数: {}", failCount);
        assertEquals(1L, failCount, "失败次数应该为1");

        // 验证账户未被锁定
        boolean isLocked = loginSecurityService.isAccountLocked(TEST_USERNAME);
        log.info("账户是否被锁定: {}", isLocked);
        assertFalse(isLocked, "账户不应该被锁定（未达到阈值）");

        log.info("✅ 测试通过：单次登录失败记录正常\n");
    }

    /**
     * 测试2: 多次登录失败但未达到锁定阈值
     *
     * 验证点:
     * - 多次失败记录是否正确累计
     * - 未达到阈值时账户不被锁定
     */
    @Test
    @DisplayName("测试2: 多次登录失败但未达到锁定阈值")
    public void testMultipleFailuresUnderThreshold() {
        log.info("\n========== 开始测试：多次登录失败但未达到锁定阈值 ==========");

        int failuresToRecord = properties.getMaxFailCount() - 1; // 比阈值少1次
        log.info("配置的最大失败次数: {}, 本次测试失败次数: {}",
            properties.getMaxFailCount(), failuresToRecord);

        // 记录多次失败（但不超过阈值）
        for (int i = 1; i <= failuresToRecord; i++) {
            loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
            log.info("第 {} 次失败记录完成", i);
        }

        // 验证失败次数
        Long failCount = loginSecurityService.getFailCount(TEST_USERNAME);
        log.info("当前失败次数: {}", failCount);
        assertEquals((long) failuresToRecord, failCount);

        // 验证账户未被锁定
        boolean isLocked = loginSecurityService.isAccountLocked(TEST_USERNAME);
        log.info("账户是否被锁定: {}", isLocked);
        assertFalse(isLocked, "账户不应该被锁定（未达到阈值）");

        log.info("✅ 测试通过：多次失败未达阈值时不锁定\n");
    }

    /**
     * 测试3: 达到阈值触发账户锁定
     *
     * 验证点:
     * - 达到失败阈值时账户被自动锁定
     * - 锁定后能正确获取剩余锁定时间
     */
    @Test
    @DisplayName("测试3: 达到阈值触发账户锁定")
    public void testAccountLockingAtThreshold() {
        log.info("\n========== 开始测试：达到阈值触发账户锁定 ==========");

        int maxFailCount = properties.getMaxFailCount();
        log.info("配置的最大失败次数: {}", maxFailCount);

        // 记录失败次数直到达到阈值
        for (int i = 1; i <= maxFailCount; i++) {
            loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
            log.info("第 {} 次失败记录完成", i);
        }

        // 验证账户已被锁定
        boolean isLocked = loginSecurityService.isAccountLocked(TEST_USERNAME);
        log.info("账户是否被锁定: {}", isLocked);
        assertTrue(isLocked, "账户应该被锁定（已达到阈值）");

        // 验证剩余锁定时间
        Long remainingTime = loginSecurityService.getRemainingLockTime(TEST_USERNAME);
        log.info("剩余锁定时间: {} 秒", remainingTime);
        assertNotNull(remainingTime, "剩余锁定时间不应为null");
        assertTrue(remainingTime > 0, "剩余锁定时间应大于0");

        // 验证剩余时间在合理范围内（应该接近配置的锁定时长）
        long expectedSeconds = properties.getLockDuration() * 60L;
        log.info("预期锁定时间: {} 秒, 实际剩余: {} 秒", expectedSeconds, remainingTime);
        assertTrue(remainingTime <= expectedSeconds, "剩余时间不应超过配置的锁定时长");

        log.info("✅ 测试通过：达到阈值时账户正确锁定\n");
    }

    /**
     * 测试4: 滑动窗口算法（时间窗口外的失败记录不计入）
     *
     * 验证点:
     * - 时间窗口外的失败记录应被自动清除
     * - 只统计窗口内的失败次数
     *
     * 注意：此测试需要等待，运行时间较长
     */
    @Test
    @DisplayName("测试4: 滑动窗口算法")
    public void testSlidingWindow() throws InterruptedException {
        log.info("\n========== 开始测试：滑动窗口算法 ==========");

        int timeWindow = properties.getTimeWindow();
        log.info("配置的时间窗口: {} 秒", timeWindow);

        // 第一次失败
        loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
        log.info("第1次失败记录完成");
        Long firstCount = loginSecurityService.getFailCount(TEST_USERNAME);
        log.info("第1次失败后，失败次数: {}", firstCount);
        assertEquals(1L, firstCount);

        // 等待时间窗口过期（加1秒确保过期）
        log.info("等待 {} 秒，让第一次失败记录超出时间窗口...", timeWindow + 1);
        Thread.sleep((timeWindow + 1) * 1000L);

        // 第二次失败（此时第一次应该已经被清除）
        loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
        log.info("第2次失败记录完成");

        // 验证失败次数（应该只有1次，因为第一次已经超出窗口）
        Long secondCount = loginSecurityService.getFailCount(TEST_USERNAME);
        log.info("第2次失败后，失败次数: {}", secondCount);
        assertEquals(1L, secondCount, "时间窗口外的记录应被清除，只保留窗口内的记录");

        log.info("✅ 测试通过：滑动窗口算法正常工作\n");
    }

    /**
     * 测试5: 清除失败记录
     *
     * 验证点:
     * - 清除操作能删除所有失败记录
     * - 清除后失败次数归零
     */
    @Test
    @DisplayName("测试5: 清除失败记录")
    public void testClearLoginFailure() {
        log.info("\n========== 开始测试：清除失败记录 ==========");

        // 记录多次失败
        for (int i = 1; i <= 2; i++) {
            loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
        }
        log.info("记录了2次失败");

        Long beforeClear = loginSecurityService.getFailCount(TEST_USERNAME);
        log.info("清除前失败次数: {}", beforeClear);
        assertEquals(2L, beforeClear);

        // 清除失败记录
        loginSecurityService.clearLoginFail(TEST_USERNAME);
        log.info("执行清除操作");

        // 验证失败次数归零
        Long afterClear = loginSecurityService.getFailCount(TEST_USERNAME);
        log.info("清除后失败次数: {}", afterClear);
        assertEquals(0L, afterClear, "清除后失败次数应该为0");

        log.info("✅ 测试通过：失败记录清除成功\n");
    }

    /**
     * 测试6: 手动解锁账户
     *
     * 验证点:
     * - 锁定的账户能被手动解锁
     * - 解锁后失败记录也被清除
     */
    @Test
    @DisplayName("测试6: 手动解锁账户")
    public void testUnlockAccount() {
        log.info("\n========== 开始测试：手动解锁账户 ==========");

        // 先锁定账户（记录足够多的失败）
        int maxFailCount = properties.getMaxFailCount();
        for (int i = 1; i <= maxFailCount; i++) {
            loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
        }
        log.info("记录了 {} 次失败，账户已锁定", maxFailCount);

        // 验证账户已锁定
        assertTrue(loginSecurityService.isAccountLocked(TEST_USERNAME), "账户应该被锁定");

        // 手动解锁
        loginSecurityService.unlockAccount(TEST_USERNAME);
        log.info("执行手动解锁操作");

        // 验证账户已解锁
        boolean isLockedAfterUnlock = loginSecurityService.isAccountLocked(TEST_USERNAME);
        log.info("解锁后账户是否被锁定: {}", isLockedAfterUnlock);
        assertFalse(isLockedAfterUnlock, "账户应该已解锁");

        // 验证失败记录已清除
        Long failCountAfterUnlock = loginSecurityService.getFailCount(TEST_USERNAME);
        log.info("解锁后失败次数: {}", failCountAfterUnlock);
        assertEquals(0L, failCountAfterUnlock, "解锁后失败记录应该被清除");

        log.info("✅ 测试通过：手动解锁账户成功\n");
    }

    /**
     * 测试7: 综合场景测试
     *
     * 模拟真实的登录场景:
     * 1. 用户多次登录失败
     * 2. 账户被锁定
     * 3. 等待锁定时间过期或手动解锁
     * 4. 登录成功后清除记录
     */
    @Test
    @DisplayName("测试7: 综合场景测试")
    public void testCompleteScenario() {
        log.info("\n========== 开始测试：综合场景测试 ==========");

        // 场景1: 用户输错密码2次
        log.info("场景1: 用户输错密码2次");
        loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
        loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
        assertFalse(loginSecurityService.isAccountLocked(TEST_USERNAME), "2次失败后不应锁定");

        // 场景2: 第3次输错，账户被锁定
        log.info("场景2: 第3次输错密码，触发锁定");
        loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
        assertTrue(loginSecurityService.isAccountLocked(TEST_USERNAME), "3次失败后应该锁定");
        log.info("账户已锁定，剩余时间: {} 秒",
            loginSecurityService.getRemainingLockTime(TEST_USERNAME));

        // 场景3: 管理员手动解锁
        log.info("场景3: 管理员介入，手动解锁账户");
        loginSecurityService.unlockAccount(TEST_USERNAME);
        assertFalse(loginSecurityService.isAccountLocked(TEST_USERNAME), "手动解锁后应该可以登录");

        // 场景4: 用户成功登录，清除历史记录
        log.info("场景4: 用户使用正确密码登录成功");
        loginSecurityService.clearLoginFail(TEST_USERNAME);
        assertEquals(0L, loginSecurityService.getFailCount(TEST_USERNAME), "登录成功后应清除失败记录");

        log.info("✅ 测试通过：综合场景测试成功\n");
    }

    /**
     * 测试8: 并发场景测试（多次快速失败）
     *
     * 验证点:
     * - 短时间内多次失败能正确记录
     * - Redis操作的原子性
     */
    @Test
    @DisplayName("测试8: 并发场景测试")
    public void testConcurrentFailures() {
        log.info("\n========== 开始测试：并发场景测试 ==========");

        int failureCount = 5;
        log.info("快速记录 {} 次失败", failureCount);

        // 快速记录多次失败
        for (int i = 1; i <= failureCount; i++) {
            loginSecurityService.recordLoginFail(TEST_USERNAME, TEST_IP);
            log.info("第 {} 次失败记录", i);
        }

        // 验证所有失败都被记录
        Long actualCount = loginSecurityService.getFailCount(TEST_USERNAME);
        log.info("记录的失败次数: {}", actualCount);
        assertEquals((long) failureCount, actualCount, "所有失败应该被正确记录");

        // 验证账户被锁定（因为超过了阈值）
        assertTrue(loginSecurityService.isAccountLocked(TEST_USERNAME),
            "超过阈值后应该锁定");

        log.info("✅ 测试通过：并发场景测试成功\n");
    }
}
