package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String account;

    private String name;

    private String nickname;

    private String avatar;

    private String department;

    private String intro;

    private String role;

    private Integer status;

    private Integer identityStatus;

    private String wechat;

    /** 脱敏手机号，如 138****8888 */
    private String phone;

    private Integer contactPublic;

    private LocalDateTime createTime;

    /** 在售商品数 */
    private Long onSaleCount;

    /** 累计卖出数 */
    private Long soldCount;
}
