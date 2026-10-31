package com.campus.trade.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口需要登录；admin = true 时还要求管理员角色。
 * 未标注该注解的接口默认公开，但仍会尝试解析令牌（便于返回“是否已收藏”等信息）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {

    boolean admin() default false;
}
