package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.service.UserService;
import com.yangjiayu.exam_system_server_online.vo.LoginRequestVo;
import com.yangjiayu.exam_system_server_online.vo.LoginResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    /**
     * 用户登录接口 - 测试版本，直接放行
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponseVo> login(@RequestBody LoginRequestVo loginRequest) {
        log.info("登录请求，用户名: {}", loginRequest.getUsername());

        // 直接返回mock数据，不做任何验证
        LoginResponseVo response = new LoginResponseVo();
        response.setUserId(1L);
        response.setUsername(loginRequest.getUsername());
        response.setRealName("测试用户");
        response.setRole("ADMIN");
        response.setToken("mock-token-123456");

        return Result.success(response, "登录成功");
    }
}