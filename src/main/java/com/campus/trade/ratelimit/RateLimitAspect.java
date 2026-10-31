package com.campus.trade.ratelimit;

import com.campus.trade.common.BizException;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 限流切面：Redis INCR 计数 + 过期时间，防止刷接口（发布、留言、举报、发验证码等）
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String name = rateLimit.key().isBlank()
                ? joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName()
                : rateLimit.key();
        String identity = identity();
        String key = RedisKeys.RATE_LIMIT.formatted(name + ":" + identity);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(rateLimit.seconds()));
        }
        if (count != null && count > rateLimit.count()) {
            throw new BizException(Result.TOO_MANY_REQUESTS, rateLimit.message());
        }
        return joinPoint.proceed();
    }

    private String identity() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            return "u" + userId;
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            return "ip:" + ip;
        }
        return "anonymous";
    }
}
