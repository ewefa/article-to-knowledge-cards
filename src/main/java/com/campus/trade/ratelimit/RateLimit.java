package com.campus.trade.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流：基于 Redis 计数，默认按「用户 ID（未登录则按 IP）」维度统计。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流键，默认使用方法名 */
    String key() default "";

    /** 时间窗口内允许的最大次数 */
    int count() default 20;

    /** 时间窗口（秒） */
    int seconds() default 60;

    /** 触发限流后的提示 */
    String message() default "操作过于频繁，请稍后再试";
}
