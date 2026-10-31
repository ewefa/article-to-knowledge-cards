package com.campus.trade.common;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(Result.BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException unauthorized(String message) {
        return new BizException(Result.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(Result.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(Result.NOT_FOUND, message);
    }
}
