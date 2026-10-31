package com.campus.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IdentityRequest {

    /** 1-通过 2-驳回 */
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    private String remark;
}
