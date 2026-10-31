package com.campus.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportRequest {

    @NotBlank(message = "请选择举报理由")
    @Size(max = 50, message = "举报理由最多 50 个字符")
    private String reason;

    @Size(max = 200, message = "补充说明最多 200 个字符")
    private String description;
}
