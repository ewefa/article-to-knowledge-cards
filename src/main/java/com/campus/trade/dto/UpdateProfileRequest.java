package com.campus.trade.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 20, message = "昵称最多 20 个字符")
    private String nickname;

    private String avatar;

    @Size(max = 64, message = "院系最多 64 个字符")
    private String department;

    @Size(max = 100, message = "简介最多 100 个字符")
    private String intro;

    @Size(max = 64, message = "微信号最多 64 个字符")
    private String wechat;

    /** 是否公开联系方式：0-否 1-是 */
    private Integer contactPublic;
}
