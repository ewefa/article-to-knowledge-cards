package com.campus.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {

    /** 0-正常 1-禁用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
