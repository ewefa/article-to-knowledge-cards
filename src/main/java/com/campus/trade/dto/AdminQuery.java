package com.campus.trade.dto;

import lombok.Data;

@Data
public class AdminQuery {

    private String keyword;

    private Integer status;

    private Integer identityStatus;

    private String role;

    private Long categoryId;

    private Integer page = 1;

    private Integer size = 10;
}
