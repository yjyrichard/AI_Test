package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.service.LoginSecurityService;
import com.yangjiayu.exam_system_server_online.service.UserService;
import com.yangjiayu.exam_system_server_online.utils.IpUtils;
import com.yangjiayu.exam_system_server_online.vo.LoginRequestVo;
import com.yangjiayu.exam_system_server_online.vo.LoginResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

/**
 * 用户服务控制器
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理")
public class UserController  {
    private final UserService userService;
    private final LoginSecurityService loginSecurityService;

    /**
     * 用户登录接口
     *
     * 集成了登录安全防护功能:
     * 1. 登录前检查账户是否被锁定
     * 2. 登录失败时记录失败信息
     * 3. 登录成功时清除失败记录
     * 4. 使用滑动窗口算法统计失败次数
     *
     * @param loginRequest 登录请求对象（包含用户名和密码）
     * @param request      HTTP请求对象，用于获取客户端IP
     * @return 登录结果（成功返回用户信息和token，失败返回错误信息）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponseVo> login(@RequestBody LoginRequestVo loginRequest,
                                          HttpServletRequest request) {
        // 提取登录参数
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        // 获取客户端真实IP地址
        // 支持通过代理、负载均衡器等场景
        String clientIp = IpUtils.getClientIp(request);

        log.info("【登录请求】用户名: {}, IP: {}", username, clientIp);

        // ============== 步骤1：检查账户是否被锁定 ==============
        // 在验证用户名密码之前先检查锁定状态
        // 避免被锁定的账户继续尝试登录
        if (loginSecurityService.isAccountLocked(username)) {
            // 获取剩余锁定时间（秒）
            Long remainingTime = loginSecurityService.getRemainingLockTime(username);

            // 转换为分钟显示（更友好）
            long remainingMinutes = remainingTime / 60;
            long remainingSeconds = remainingTime % 60;

            String lockMessage = String.format(
                "账户已被锁定，请 %d分%d秒 后再试。如非本人操作请联系管理员。",
                remainingMinutes,
                remainingSeconds
            );

            log.warn("【登录失败】账户已锁定 - 用户名: {}, IP: {}, 剩余时间: {}秒",
                username, clientIp, remainingTime);

            return Result.error(lockMessage);
        }

        // ============== 步骤2：验证用户名和密码 ==============
        // TODO: 这里需要实现真实的用户验证逻辑
        // 目前使用mock数据进行演示

        // 模拟验证逻辑：密码错误的情况
        // 在实际项目中，这里应该调用 userService.authenticate(username, password)
        boolean isPasswordCorrect = "123456".equals(password); // 仅用于演示

        if (!isPasswordCorrect) {
            // 密码错误，记录登录失败
            loginSecurityService.recordLoginFail(username, clientIp);

            log.warn("【登录失败】密码错误 - 用户名: {}, IP: {}", username, clientIp);

            return Result.error("用户名或密码错误");
        }

        // ============== 步骤3：登录成功处理 ==============
        // 清除该用户的所有失败记录
        loginSecurityService.clearLoginFail(username);

        // 构建登录响应数据
        LoginResponseVo response = new LoginResponseVo();
        response.setUserId(1L);
        response.setUsername(username);
        response.setRealName("测试用户");
        response.setRole("ADMIN");
        response.setToken("mock-token-" + System.currentTimeMillis());

        log.info("【登录成功】用户名: {}, IP: {}", username, clientIp);

        return Result.success(response, "登录成功");
    }
}