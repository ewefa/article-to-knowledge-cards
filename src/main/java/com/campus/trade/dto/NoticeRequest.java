package com.campus.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoticeRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题最多 50 个字符")
    private String title;

    private String content;

    private String image;

    /** 0-轮播图 1-公告 */
    @NotNull(message = "请选择类型")
    private Integer type;

    private Integer sort;

    private Integer status;
}
