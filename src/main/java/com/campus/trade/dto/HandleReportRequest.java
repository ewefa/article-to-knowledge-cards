package com.campus.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HandleReportRequest {

    /** offline-下架商品 warn-警告用户 ban-封禁用户 reject-驳回举报 */
    @NotBlank(message = "请选择处理方式")
    private String action;

    @Size(max = 200, message = "处理说明最多 200 个字符")
    private String remark;
}
