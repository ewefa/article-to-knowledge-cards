package com.campus.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 20, message = "分类名称最多 20 个字符")
    private String name;

    private String icon;

    private Integer sort;

    private Integer status;
}
