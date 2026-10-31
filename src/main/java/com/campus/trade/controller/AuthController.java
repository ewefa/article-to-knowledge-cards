package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.dto.LoginRequest;
import com.campus.trade.dto.RegisterRequest;
import com.campus.trade.dto.ResetPasswordRequest;
import com.campus.trade.dto.SmsCodeRequest;
import com.campus.trade.ratelimit.RateLimit;
import com.campus.trade.service.AuthService;
import com.campus.trade.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 注册 / 登录 / 退出 / 找回密码
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 发送短信验证码（演示模式下验证码直接在返回结果中给出） */
    @PostMapping("/sms-code")
    @RateLimit(key = "sms-code", count = 5, seconds = 300, message = "验证码请求过于频繁，请稍后再试")
    public Result<Map<String, Object>> sendCode(@Valid @RequestBody SmsCodeRequest request) {
        return Result.ok("验证码已发送", authService.sendCode(request.getPhone()));
    }

    /** 注册（仅限本校师生学号 / 工号） */
    @PostMapping("/register")
    @RateLimit(key = "register", count = 10, seconds = 600, message = "注册过于频繁，请稍后再试")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok("注册成功", authService.register(request));
    }

    /** 账号密码登录 */
    @PostMapping("/login")
    @RateLimit(key = "login", count = 20, seconds = 300, message = "登录尝试过于频繁，请稍后再试")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok("登录成功", authService.login(request));
    }

    /** 退出登录：删除 Redis 中的令牌 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.ok("已退出登录", null);
    }

    /** 忘记密码：手机号 + 验证码重置 */
    @PostMapping("/reset-password")
    @RateLimit(key = "reset-password", count = 10, seconds = 600, message = "操作过于频繁，请稍后再试")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.ok("密码重置成功，请重新登录", null);
    }
}
