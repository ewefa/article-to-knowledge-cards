package com.campus.trade.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuditRequest {

    @NotNull(message = "请选择审核结果")
    private Boolean pass;

    @Size(max = 100, message = "理由最多 100 个字符")
    private String reason;
}
