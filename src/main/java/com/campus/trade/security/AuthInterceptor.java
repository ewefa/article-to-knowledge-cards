package com.campus.trade.security;

import com.campus.trade.common.BizException;
import com.campus.trade.common.LoginUser;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.common.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：
 * 1. 解析 Authorization: Bearer <token>，校验 JWT 签名；
 * 2. 与 Redis 中的令牌比对（实现单点登录、退出即失效、禁用用户即时踢下线）；
 * 3. 根据 @LoginRequired 决定是否放行。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        LoginUser user = resolve(request);
        if (user != null) {
            UserContext.set(user);
        }
        LoginRequired required = handlerMethod.getMethodAnnotation(LoginRequired.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(LoginRequired.class);
        }
        if (required != null) {
            if (user == null) {
                throw BizException.unauthorized("登录已失效，请重新登录");
            }
            if (required.admin() && !user.isAdmin()) {
                throw BizException.forbidden("无权访问该功能");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private LoginUser resolve(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        if (token == null || token.isBlank()) {
            return null;
        }
        Claims claims = jwtUtil.parse(token);
        if (claims == null) {
            return null;
        }
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
        // 与 Redis 中的令牌比对：新登录会覆盖旧令牌，退出登录会删除令牌
        String cached = redisTemplate.opsForValue().get(RedisKeys.loginToken(userId));
        if (cached == null || !cached.equals(token)) {
            return null;
        }
        return new LoginUser(userId,
                claims.get("account", String.class),
                claims.get("name", String.class),
                claims.get("role", String.class));
    }
}
