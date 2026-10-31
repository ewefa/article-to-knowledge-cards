package com.campus.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfigRequest {

    @NotBlank(message = "配置项不能为空")
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    private String configValue;
}
