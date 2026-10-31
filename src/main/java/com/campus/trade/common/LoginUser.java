package com.campus.trade.common;

import java.io.Serializable;

/**
 * 当前登录用户（由 JWT + Redis 令牌解析得到）
 */
public record LoginUser(Long id, String account, String name, String role) implements Serializable {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
