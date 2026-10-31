package com.campus.trade.common;

/**
 * 登录用户上下文（ThreadLocal），在拦截器中写入，请求结束后清理
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 当前登录用户 ID，未登录返回 null */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.id();
    }

    /** 当前登录用户 ID，未登录抛出 401 */
    public static Long requireUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("请先登录");
        }
        return user.id();
    }

    public static LoginUser requireUser() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("请先登录");
        }
        return user;
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && user.isAdmin();
    }
}
