package com.campus.trade.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private String token;

    private long expireSeconds;

    private UserVO user;
}
