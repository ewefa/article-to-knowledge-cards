package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.config.CampusProperties;
import com.campus.trade.dto.ChangePasswordRequest;
import com.campus.trade.dto.LoginRequest;
import com.campus.trade.dto.RegisterRequest;
import com.campus.trade.dto.ResetPasswordRequest;
import com.campus.trade.dto.UpdateProfileRequest;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.security.JwtUtil;
import com.campus.trade.vo.LoginVO;
import com.campus.trade.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 注册登录业务：验证码、注册、登录（Redis 单点登录）、找回密码、修改密码、资料维护
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private static final Duration CODE_INTERVAL = Duration.ofSeconds(60);

    private static final String DEFAULT_AVATAR = "/images/avatar/default.png";

    private final UserMapper userMapper;

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final StringRedisTemplate redisTemplate;

    private final CampusProperties properties;

    private final ConfigService configService;

    /** 发送短信验证码：验证码存 Redis 5 分钟，同一手机号 60 秒内只能发一次 */
    public Map<String, Object> sendCode(String phone) {
        String limitKey = String.format(RedisKeys.SMS_LIMIT, phone);
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(limitKey, "1", CODE_INTERVAL);
        if (!Boolean.TRUE.equals(absent)) {
            throw new BizException("验证码发送过于频繁，请 60 秒后再试");
        }
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        redisTemplate.opsForValue().set(RedisKeys.smsCode(phone), code, CODE_TTL);
        log.info("向 {} 发送验证码：{}（演示模式，未调用真实短信网关）", phone, code);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phone", phone);
        data.put("expireSeconds", CODE_TTL.toSeconds());
        if (properties.getSms().isMock()) {
            data.put("code", code);
            data.put("mock", true);
        }
        return data;
    }

    private void verifyCode(String phone, String code) {
        String cached = redisTemplate.opsForValue().get(RedisKeys.smsCode(phone));
        if (cached == null) {
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!cached.equals(code)) {
            throw new BizException("验证码不正确");
        }
        redisTemplate.delete(RedisKeys.smsCode(phone));
    }

    /** 注册：学号/工号 + 姓名 + 手机号 + 验证码 + 密码 */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterRequest request) {
        verifyCode(request.getPhone(), request.getCode());
        if (userService.getByAccount(request.getAccount()) != null) {
            throw new BizException("该学号/工号已注册，请直接登录");
        }
        if (userService.getByPhone(request.getPhone()) != null) {
            throw new BizException("该手机号已被使用");
        }
        User user = new User();
        user.setAccount(request.getAccount());
        user.setName(request.getName());
        user.setNickname(request.getName());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatar(DEFAULT_AVATAR);
        user.setDepartment(request.getDepartment());
        user.setIntro("这个同学很懒，还没有填写简介");
        user.setRole("USER");
        user.setStatus(0);
        // 开启身份审核时，新用户需管理员人工审核（identity_status=0 待审核）
        boolean needCheck = configService.getBool(ConfigService.IDENTITY_CHECK, false);
        user.setIdentityStatus(needCheck ? 0 : 1);
        user.setContactPublic(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return buildLogin(user);
    }

    /** 登录：校验密码、生成 JWT 并写入 Redis（同一账号后登录的令牌会顶掉先登录的） */
    public LoginVO login(LoginRequest request) {
        User user = userService.getByAccount(request.getAccount().trim());
        if (user == null) {
            throw new BizException("账号或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BizException("账号已被禁用，请联系管理员");
        }
        return buildLogin(user);
    }

    private LoginVO buildLogin(User user) {
        String token = jwtUtil.createToken(user.getId(), user.getAccount(), user.getName(), user.getRole());
        redisTemplate.opsForValue().set(RedisKeys.loginToken(user.getId()), token,
                Duration.ofSeconds(jwtUtil.getExpireSeconds()));
        return new LoginVO(token, jwtUtil.getExpireSeconds(), userService.toVO(user));
    }

    /** 退出登录：删除 Redis 中的令牌，令牌立即失效 */
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId != null) {
            redisTemplate.delete(RedisKeys.loginToken(userId));
        }
    }

    /** 忘记密码：手机号 + 验证码重置 */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        verifyCode(request.getPhone(), request.getCode());
        User user = userService.getByPhone(request.getPhone());
        if (user == null) {
            throw new BizException("该手机号未注册");
        }
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        // 重置密码后强制重新登录
        redisTemplate.delete(RedisKeys.loginToken(user.getId()));
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        User user = userService.requireUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        redisTemplate.delete(RedisKeys.loginToken(userId));
    }

    public UserVO profile(Long userId) {
        return userService.toVO(userService.requireUser(userId));
    }

    public void updateProfile(Long userId, UpdateProfileRequest request) {
        userService.updateProfile(userId, request.getNickname(), request.getDepartment(), request.getIntro(),
                request.getAvatar(), request.getWechat(), request.getContactPublic());
    }
}
