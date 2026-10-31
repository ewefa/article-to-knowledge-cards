package com.campus.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "商品标题不能为空")
    @Size(min = 2, max = 50, message = "标题长度为 2-50 个字符")
    private String title;

    @Size(max = 2000, message = "描述最多 2000 个字符")
    private String description;

    @NotNull(message = "请选择商品分类")
    private Long categoryId;

    @DecimalMin(value = "0.0", inclusive = false, message = "售价必须大于 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "原价不能为负数")
    private BigDecimal originalPrice;

    /** 是否可议价：0-否 1-是 */
    private Integer negotiable;

    @Size(max = 64, message = "交易地点最多 64 个字符")
    private String tradePlace;

    @Size(max = 9, message = "最多上传 9 张图片")
    private List<String> images;
}
